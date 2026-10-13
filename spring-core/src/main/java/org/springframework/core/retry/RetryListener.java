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

package org.springframework.core.retry;

import org.jspecify.annotations.Nullable;

/**
 * {@code RetryListener} defines a <em>listener</em> API for reacting to events
 * published during the execution of a {@link Retryable} operation.
 *
 * <p>Typically registered in a {@link RetryTemplate}, and can be composed using a
 * {@link org.springframework.core.retry.support.CompositeRetryListener CompositeRetryListener}.
 *
 * <p>Several callbacks come in two overloads: a reduced variant and a
 * full-argument variant that additionally receives the {@link RetryState}.
 * The full-argument variant delegates to the reduced variant by default.
 * Implement either the reduced method or the full-argument method, not both,
 * to avoid double processing of the same callback.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Chengang Guan
 * @since 7.0
 * @see org.springframework.core.retry.support.CompositeRetryListener
 */
public interface RetryListener {

	/**
	 * Called after every attempt, including the initial invocation.
	 * <p>The {@code retryState} is a live instance reflecting the current state;
	 * not intended to be stored.
	 * <p>The success of the attempt can be checked via {@link RetryState#isSuccessful()};
	 * if not successful, the current exception can be introspected via
	 * {@link RetryState#getLastException()}.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * @since 7.0.2
	 * @see RetryTemplate#execute(Retryable)
	 * @see RetryState#isSuccessful()
	 * @see RetryState#getLastException()
	 * @see RetryState#getRetryCount()
	 */
	default void onRetryableExecution(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
	}

	/**
	 * Called before every retry attempt.
	 * <p>The {@code retryState} is a live instance reflecting the current state;
	 * not intended to be stored.
	 * <p>For a corresponding callback after every retry attempt, consider
	 * {@link #onRetryableExecution(RetryPolicy, Retryable, RetryState)}, ignoring the
	 * initial attempt through a corresponding {@link RetryState#getRetryCount()} check.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * @since 7.0.4
	 * @see #onRetryableExecution(RetryPolicy, Retryable, RetryState)
	 */
	default void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
		beforeRetry(retryPolicy, retryable);
	}

	/**
	 * Called before every retry attempt.
	 * <p>Called by {@link #beforeRetry(RetryPolicy, Retryable, RetryState)}.
	 * See the {@linkplain RetryListener class-level documentation} for the
	 * overload contract.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @see #beforeRetry(RetryPolicy, Retryable, RetryState)
	 */
	default void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
	}

	/**
	 * Called after the first successful retry attempt.
	 * <p>The {@code retryState} is a live instance reflecting the current state;
	 * for a success callback, the state has reached the end of its updates and
	 * can therefore be considered effectively stable.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * @param result the result of the {@code Retryable} operation
	 * @since 7.1
	 */
	default void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState, @Nullable Object result) {
		onRetrySuccess(retryPolicy, retryable, result);
	}

	/**
	 * Called after the first successful retry attempt.
	 * <p>Called by {@link #onRetrySuccess(RetryPolicy, Retryable, RetryState, Object)}.
	 * See the {@linkplain RetryListener class-level documentation} for the
	 * overload contract.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param result the result of the {@code Retryable} operation
	 * @see #onRetrySuccess(RetryPolicy, Retryable, RetryState, Object)
	 */
	default void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
	}

	/**
	 * Called after every failed retry attempt.
	 * <p>The {@code retryState} is a live instance reflecting the current state;
	 * not intended to be stored.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * @param throwable the exception thrown by the {@code Retryable} operation
	 * @since 7.1
	 */
	default void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState, Throwable throwable) {
		onRetryFailure(retryPolicy, retryable, throwable);
	}

	/**
	 * Called after every failed retry attempt.
	 * <p>Called by {@link #onRetryFailure(RetryPolicy, Retryable, RetryState, Throwable)}.
	 * See the {@linkplain RetryListener class-level documentation} for the
	 * overload contract.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param throwable the exception thrown by the {@code Retryable} operation
	 * @see #onRetryFailure(RetryPolicy, Retryable, RetryState, Throwable)
	 */
	default void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
	}

	/**
	 * Called if the {@link RetryPolicy} is exhausted.
	 * @param retryPolicy the {@code RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param exception the resulting {@link RetryException}, with the last
	 * exception thrown by the {@code Retryable} operation as the cause and any
	 * exceptions from previous attempts as suppressed exceptions
	 * @see RetryException#getExceptions()
	 * @see RetryException#getRetryCount()
	 */
	default void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
	}

	/**
	 * Called if the {@link RetryPolicy} is interrupted between retry attempts.
	 * @param retryPolicy the {@code RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param exception the resulting {@link RetryException}, with the last
	 * exception thrown by the {@code Retryable} operation as the cause and any
	 * exceptions from previous attempts as suppressed exceptions
	 * @see RetryException#getExceptions()
	 * @see RetryException#getRetryCount()
	 */
	default void onRetryPolicyInterruption(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
	}

	/**
	 * Called if the configured {@linkplain RetryPolicy#getTimeout() timeout} for
	 * a {@link RetryPolicy} is exceeded.
	 * @param retryPolicy the {@code RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param exception the resulting {@link RetryException}, with the last
	 * exception thrown by the {@code Retryable} operation as the cause and any
	 * exceptions from previous attempts as suppressed exceptions
	 * @since 7.0.2
	 * @see RetryException#getExceptions()
	 * @see RetryException#getRetryCount()
	 */
	default void onRetryPolicyTimeout(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
	}

}
