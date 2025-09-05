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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A convenient {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} that applies a concurrency interceptor to all bean methods
 * annotated with {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
public class ConcurrencyLimitBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	public ConcurrencyLimitBeanPostProcessor() {
		setBeforeExistingAdvisors(true);

		Pointcut cpc = new AnnotationMatchingPointcut(ConcurrencyLimit.class, true);
		Pointcut mpc = new AnnotationMatchingPointcut(null, ConcurrencyLimit.class, true);
		this.advisor = new DefaultPointcutAdvisor(
				new ComposablePointcut(cpc).union(mpc),
				new ConcurrencyLimitInterceptor());
	}


	private static class ConcurrencyLimitInterceptor implements MethodInterceptor {

		private final Map<Object, ConcurrencyThrottleCache> cachePerInstance =
				new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

		@Override
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			Object target = invocation.getThis();
			Class<?> targetClass = (target != null ? target.getClass() : method.getDeclaringClass());
			if (target == null && invocation instanceof ProxyMethodInvocation methodInvocation) {
				// Allow validation for AOP proxy without a target
				target = methodInvocation.getProxy();
			}
			Assert.state(target != null, "Target must not be null");

			ConcurrencyThrottleCache cache = this.cachePerInstance.computeIfAbsent(target,
					k -> new ConcurrencyThrottleCache());
			MethodInterceptor interceptor = cache.methodInterceptors.get(method);
			if (interceptor == null) {
				synchronized (cache) {
					interceptor = cache.methodInterceptors.get(method);
					if (interceptor == null) {
						boolean perMethod = false;
						ConcurrencyLimit limit = AnnotatedElementUtils.getMergedAnnotation(method, ConcurrencyLimit.class);
						if (limit != null) {
							perMethod = true;
						}
						else {
							interceptor = cache.classInterceptor;
							if (interceptor == null) {
								limit = AnnotatedElementUtils.getMergedAnnotation(targetClass, ConcurrencyLimit.class);
							}
						}
						if (interceptor == null) {
							Assert.state(limit != null, "No @ConcurrencyLimit annotation found");
							interceptor = new ConcurrencyThrottleInterceptor(limit.value());
							if (!perMethod) {
								cache.classInterceptor = interceptor;
							}
						}
						cache.methodInterceptors.put(method, interceptor);
					}
				}
			}
			return interceptor.invoke(invocation);
		}
	}


	private static class ConcurrencyThrottleCache {

		final Map<Method, MethodInterceptor> methodInterceptors = new ConcurrentHashMap<>();

		@Nullable MethodInterceptor classInterceptor;
	}

}
