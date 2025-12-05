/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.resilience.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.resilience.retry.AbstractRetryInterceptor;
import org.springframework.resilience.retry.MethodRetryPredicate;
import org.springframework.resilience.retry.MethodRetrySpec;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * A convenient {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} that applies a retry interceptor to all bean methods
 * annotated with {@link Retryable @Retryable}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
public class RetryAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements EmbeddedValueResolverAware {

	private @Nullable StringValueResolver embeddedValueResolver;


	public RetryAnnotationBeanPostProcessor() {
		setBeforeExistingAdvisors(true);

		Pointcut cpc = new AnnotationMatchingPointcut(Retryable.class, true);
		Pointcut mpc = new AnnotationMatchingPointcut(null, Retryable.class, true);
		this.advisor = new DefaultPointcutAdvisor(
				new ComposablePointcut(cpc).union(mpc),
				new RetryAnnotationInterceptor());
	}


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	private class RetryAnnotationInterceptor extends AbstractRetryInterceptor {

		private final Map<MethodClassKey, MethodRetrySpec> retrySpecCache = new ConcurrentHashMap<>();

		@Override
		protected @Nullable MethodRetrySpec getRetrySpec(Method method, Class<?> targetClass) {
			MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
			MethodRetrySpec retrySpec = this.retrySpecCache.get(cacheKey);
			if (retrySpec != null) {
				return retrySpec;
			}

			Retryable retryable = AnnotatedElementUtils.getMergedAnnotation(method, Retryable.class);
			if (retryable == null) {
				retryable = AnnotatedElementUtils.getMergedAnnotation(targetClass, Retryable.class);
				if (retryable == null) {
					return null;
				}
			}

			TimeUnit timeUnit = retryable.timeUnit();
			retrySpec = new MethodRetrySpec(
					Arrays.asList(retryable.includes()), Arrays.asList(retryable.excludes()),
					instantiatePredicate(retryable.predicate()),
					parseLong(retryable.maxRetries(), retryable.maxRetriesString()),
					parseDuration(retryable.timeout(), retryable.timeoutString(), timeUnit),
					parseDuration(retryable.delay(), retryable.delayString(), timeUnit),
					parseDuration(retryable.jitter(), retryable.jitterString(), timeUnit),
					parseDouble(retryable.multiplier(), retryable.multiplierString()),
					parseDuration(retryable.maxDelay(), retryable.maxDelayString(), timeUnit));

			MethodRetrySpec existing = this.retrySpecCache.putIfAbsent(cacheKey, retrySpec);
			return (existing != null ? existing : retrySpec);
		}

		private MethodRetryPredicate instantiatePredicate(Class<? extends MethodRetryPredicate> predicateClass) {
			if (predicateClass == MethodRetryPredicate.class) {
				return (method, throwable) -> true;
			}
			try {
				return (beanFactory != null ? beanFactory.createBean(predicateClass) :
						ReflectionUtils.accessibleConstructor(predicateClass).newInstance());
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to instantiate predicate class [" + predicateClass + "]", ex);
			}
		}

		private long parseLong(long value, String stringValue) {
			if (StringUtils.hasText(stringValue)) {
				if (embeddedValueResolver != null) {
					stringValue = embeddedValueResolver.resolveStringValue(stringValue);
				}
				if (StringUtils.hasText(stringValue)) {
					return Long.parseLong(stringValue);
				}
			}
			return value;
		}

		private double parseDouble(double value, String stringValue) {
			if (StringUtils.hasText(stringValue)) {
				if (embeddedValueResolver != null) {
					stringValue = embeddedValueResolver.resolveStringValue(stringValue);
				}
				if (StringUtils.hasText(stringValue)) {
					return Double.parseDouble(stringValue);
				}
			}
			return value;
		}

		private Duration parseDuration(long value, String stringValue, TimeUnit timeUnit) {
			if (StringUtils.hasText(stringValue)) {
				if (embeddedValueResolver != null) {
					stringValue = embeddedValueResolver.resolveStringValue(stringValue);
				}
				if (StringUtils.hasText(stringValue)) {
					return toDuration(stringValue, timeUnit);
				}
			}
			return toDuration(value, timeUnit);
		}

		private static Duration toDuration(long value, TimeUnit timeUnit) {
			return Duration.of(value, timeUnit.toChronoUnit());
		}

		private static Duration toDuration(String value, TimeUnit timeUnit) {
			DurationFormat.Unit unit = DurationFormat.Unit.fromChronoUnit(timeUnit.toChronoUnit());
			return DurationFormatterUtils.detectAndParse(value, unit);
		}
	}

}
