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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Pointcut;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * A convenient {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} that applies a concurrency interceptor to all bean methods
 * annotated with {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * @author Juergen Hoeller
 * @author Hyunsang Han
 * @since 7.0
 */
@SuppressWarnings("serial")
public class ConcurrencyLimitBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements EmbeddedValueResolverAware {

	private @Nullable StringValueResolver embeddedValueResolver;


	public ConcurrencyLimitBeanPostProcessor() {
		setBeforeExistingAdvisors(true);

		Pointcut cpc = new AnnotationMatchingPointcut(ConcurrencyLimit.class, true);
		Pointcut mpc = new AnnotationMatchingPointcut(null, ConcurrencyLimit.class, true);
		this.advisor = new DefaultPointcutAdvisor(
				new ComposablePointcut(cpc).union(mpc),
				new ConcurrencyLimitInterceptor());
	}


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	private class ConcurrencyLimitInterceptor implements MethodInterceptor {

		private final Map<Object, ConcurrencyThrottleHolder> holderPerInstance =
				Collections.synchronizedMap(new IdentityHashMap<>(16));

		@Override
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			Object instance = invocation.getThis();
			Class<?> targetClass = (instance != null ? instance.getClass() : method.getDeclaringClass());
			if (invocation instanceof ProxyMethodInvocation methodInvocation) {
				// Apply concurrency throttling at the AOP proxy level (independent of target instance)
				instance = methodInvocation.getProxy();
			}
			Assert.state(instance != null, "Unique instance required - use a ProxyMethodInvocation");

			// Build unique ConcurrencyThrottleHolder instance per target object
			ConcurrencyThrottleHolder holder = this.holderPerInstance.computeIfAbsent(instance,
					k -> new ConcurrencyThrottleHolder());

			// Determine method-specific interceptor instance with isolated concurrency count
			MethodInterceptor interceptor = holder.methodInterceptors.get(method);
			if (interceptor == null) {
				synchronized (holder) {
					interceptor = holder.methodInterceptors.get(method);
					if (interceptor == null) {
						boolean perMethod = false;
						ConcurrencyLimit annotation = AnnotatedElementUtils.getMergedAnnotation(method, ConcurrencyLimit.class);
						if (annotation != null) {
							perMethod = true;
						}
						else {
							interceptor = holder.classInterceptor;
							if (interceptor == null) {
								annotation = AnnotatedElementUtils.getMergedAnnotation(targetClass, ConcurrencyLimit.class);
							}
						}
						if (interceptor == null) {
							Assert.state(annotation != null, "No @ConcurrencyLimit annotation found");
							int concurrencyLimit = parseInt(annotation.limit(), annotation.limitString());
							if (concurrencyLimit < -1) {
								throw new IllegalStateException(annotation + " must be configured with a valid limit");
							}
							interceptor = new ConcurrencyThrottleInterceptor(concurrencyLimit);
							if (!perMethod) {
								holder.classInterceptor = interceptor;
							}
						}
						holder.methodInterceptors.put(method, interceptor);
					}
				}
			}
			return interceptor.invoke(invocation);
		}

		private int parseInt(int value, String stringValue) {
			if (StringUtils.hasText(stringValue)) {
				if (embeddedValueResolver != null) {
					stringValue = embeddedValueResolver.resolveStringValue(stringValue);
				}
				if (StringUtils.hasText(stringValue)) {
					return Integer.parseInt(stringValue);
				}
			}
			return value;
		}
	}


	private static class ConcurrencyThrottleHolder {

		final Map<Method, MethodInterceptor> methodInterceptors = new ConcurrentHashMap<>();

		@Nullable MethodInterceptor classInterceptor;
	}

}
