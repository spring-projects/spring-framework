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
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 7.0
 * @see org.springframework.core.retry.support.CompositeRetryListener
 */
public interface RetryListener {

	/**
	 * Called before every retry attempt.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 */
	default void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
	}

	/**
	 * Called after the first successful retry attempt.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param result the result of the {@code Retryable} operation
	 */
	default void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
	}

	/**
	 * Called after every failed retry attempt.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param throwable the exception thrown by the {@code Retryable} operation
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
	 * @see RetryException#getCause()
	 * @see RetryException#getSuppressed()
	 * @see RetryException#getRetryCount()
	 */
	default void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
	}

	/**
	 * Called if the {@link RetryPolicy} is interrupted between retry attempts.
	 * @param retryPolicy the {@code RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param exception the resulting {@link RetryException}, with an
	 * {@link InterruptedException} as the cause and any exceptions from previous
	 * invocations of the {@code Retryable} operation as suppressed exceptions
	 * @see RetryException#getCause()
	 * @see RetryException#getSuppressed()
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
	 * @see RetryException#getCause()
	 * @see RetryException#getSuppressed()
	 * @see RetryException#getRetryCount()
	 */
	default void onRetryPolicyTimeout(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
	}

}
