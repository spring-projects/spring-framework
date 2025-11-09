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

package org.springframework.resilience.retry;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.util.ClassUtils;

/**
 * Abstract retry interceptor implementation, adapting a given
 * retry specification to either {@link RetryTemplate} or Reactor.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see #getRetrySpec
 * @see RetryTemplate
 * @see Mono#retryWhen
 * @see Flux#retryWhen
 */
public abstract class AbstractRetryInterceptor implements MethodInterceptor {

	/**
	 * Reactive Streams API present on the classpath?
	 */
	private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", AbstractRetryInterceptor.class.getClassLoader());

	private final @Nullable ReactiveAdapterRegistry reactiveAdapterRegistry;


	public AbstractRetryInterceptor() {
		if (REACTIVE_STREAMS_PRESENT) {
			this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		}
		else {
			this.reactiveAdapterRegistry = null;
		}
	}


	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		Object target = invocation.getThis();
		MethodRetrySpec spec = getRetrySpec(method, (target != null ? target.getClass() : method.getDeclaringClass()));

		if (spec == null) {
			return invocation.proceed();
		}

		if (this.reactiveAdapterRegistry != null && !Future.class.isAssignableFrom(method.getReturnType())) {
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(method.getReturnType());
			if (adapter != null) {
				Object result = invocation.proceed();
				if (result == null) {
					return null;
				}
				return ReactorDelegate.adaptReactiveResult(result, adapter, spec, method);
			}
		}

		RetryPolicy retryPolicy = RetryPolicy.builder()
				.includes(spec.includes())
				.excludes(spec.excludes())
				.predicate(spec.predicate().forMethod(method))
				.maxRetries(spec.maxRetries())
				.delay(spec.delay())
				.jitter(spec.jitter())
				.multiplier(spec.multiplier())
				.maxDelay(spec.maxDelay())
				.build();
		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);

		try {
			return retryTemplate.execute(new Retryable<@Nullable Object>() {
				@Override
				public @Nullable Object execute() throws Throwable {
					return (invocation instanceof ProxyMethodInvocation pmi ?
							pmi.invocableClone().proceed() : invocation.proceed());
				}
				@Override
				public String getName() {
					return ClassUtils.getQualifiedMethodName(method, (target != null ? target.getClass() : null));
				}
			});
		}
		catch (RetryException ex) {
			throw ex.getCause();
		}
	}

	/**
	 * Determine the retry specification for the given method on the given target.
	 * @param method the currently executing method
	 * @param targetClass the class of the current target object
	 * @return the retry specification as a {@link MethodRetrySpec}
	 */
	protected abstract @Nullable MethodRetrySpec getRetrySpec(Method method, Class<?> targetClass);


	/**
	 * Inner class to avoid a hard dependency on Reactive Streams and Reactor at runtime.
	 */
	private static class ReactorDelegate {

		public static Object adaptReactiveResult(
				Object result, ReactiveAdapter adapter, MethodRetrySpec spec, Method method) {

			Publisher<?> publisher = adapter.toPublisher(result);
			Retry retry = Retry.backoff(spec.maxRetries(), spec.delay())
					.jitter(calculateJitterFactor(spec))
					.multiplier(spec.multiplier())
					.maxBackoff(spec.maxDelay())
					.filter(spec.combinedPredicate().forMethod(method));
			publisher = (adapter.isMultiValue() ? Flux.from(publisher).retryWhen(retry) :
					Mono.from(publisher).retryWhen(retry));
			return adapter.fromPublisher(publisher);
		}

		private static double calculateJitterFactor(MethodRetrySpec spec) {
			return (spec.delay().isZero() ? 0.0 :
					Math.max(0.0, Math.min(1.0, spec.jitter().toNanos() / (double) spec.delay().toNanos())));
		}
	}

}
