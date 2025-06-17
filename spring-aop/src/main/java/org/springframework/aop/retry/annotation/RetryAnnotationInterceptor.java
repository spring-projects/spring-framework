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

package org.springframework.aop.retry.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.retry.AbstractRetryInterceptor;
import org.springframework.aop.retry.MethodRetryPredicate;
import org.springframework.aop.retry.MethodRetrySpec;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

/**
 * An annotation-based retry interceptor based on {@link Retryable} annotations.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
public class RetryAnnotationInterceptor extends AbstractRetryInterceptor {

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

		retrySpec = new MethodRetrySpec(
				Arrays.asList(retryable.includes()), Arrays.asList(retryable.excludes()),
				instantiatePredicate(retryable.predicate()), retryable.maxAttempts(),
				retryable.delay(), retryable.jitterDelay(),
				retryable.delayMultiplier(), retryable.maxDelay());

		MethodRetrySpec existing = this.retrySpecCache.putIfAbsent(cacheKey, retrySpec);
		return (existing != null ? existing : retrySpec);
	}

	private MethodRetryPredicate instantiatePredicate(Class<? extends MethodRetryPredicate> predicateClass) {
		if (predicateClass == MethodRetryPredicate.class) {
			return (method, throwable) -> true;
		}
		try {
			return ReflectionUtils.accessibleConstructor(predicateClass).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to instantiate predicate class [" + predicateClass + "]", ex);
		}
	}

}
