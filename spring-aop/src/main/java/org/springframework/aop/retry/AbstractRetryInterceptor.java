/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.aop.retry;

import java.lang.reflect.Method;
import java.time.Duration;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.util.ClassUtils;
import org.springframework.util.backoff.ExponentialBackOff;

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
	private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", AbstractRetryInterceptor.class.getClassLoader());

	private final @Nullable ReactiveAdapterRegistry reactiveAdapterRegistry;


	public AbstractRetryInterceptor() {
		if (reactiveStreamsPresent) {
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

		if (this.reactiveAdapterRegistry != null) {
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(method.getReturnType());
			if (adapter != null) {
				Object result = invocation.proceed();
				if (result == null) {
					return null;
				}
				return ReactorDelegate.adaptReactiveResult(result, adapter, spec, method);
			}
		}

		RetryTemplate retryTemplate = new RetryTemplate();

		RetryPolicy.Builder policyBuilder = RetryPolicy.builder();
		for (Class<? extends Throwable> include : spec.includes()) {
			policyBuilder.includes(include);
		}
		for (Class<? extends Throwable> exclude : spec.excludes()) {
			policyBuilder.excludes(exclude);
		}
		policyBuilder.predicate(spec.predicate().forMethod(method));
		policyBuilder.maxAttempts(spec.maxAttempts());
		retryTemplate.setRetryPolicy(policyBuilder.build());

		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(spec.delay());
		backOff.setJitter(spec.jitterDelay());
		backOff.setMultiplier(spec.delayMultiplier());
		backOff.setMaxInterval(spec.maxDelay());
		backOff.setMaxAttempts(spec.maxAttempts());
		retryTemplate.setBackOffPolicy(backOff);

		try {
			return retryTemplate.execute(new Retryable<>() {
				@Override
				public @Nullable Object execute() throws Throwable {
					return invocation.proceed();
				}
				@Override
				public String getName() {
					Object target = invocation.getThis();
					return ClassUtils.getQualifiedMethodName(method, (target != null ? target.getClass() : null));
				}
			});
		}
		catch (RetryException ex) {
			Throwable cause = ex.getCause();
			throw (cause != null ? cause : new IllegalStateException(ex.getMessage(), ex));
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
			Retry retry = Retry.backoff(spec.maxAttempts(), Duration.ofMillis(spec.delay()))
					.jitter((double) spec.jitterDelay() / spec.delay())
					.multiplier(spec.delayMultiplier())
					.maxBackoff(Duration.ofMillis(spec.maxDelay()))
					.filter(spec.combinedPredicate().forMethod(method));
			publisher = (adapter.isMultiValue() ? Flux.from(publisher).retryWhen(retry) :
					Mono.from(publisher).retryWhen(retry));
			return adapter.fromPublisher(publisher);
		}
	}

}
