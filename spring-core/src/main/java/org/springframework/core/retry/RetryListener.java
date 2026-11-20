/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * Called after every attempt, including the initial invocation.
	 * <p>The success of the attempt can be checked via {@link RetryState#isSuccessful()};
	 * if not successful, the current exception can be introspected via
	 * {@link RetryState#getLastException()}.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * (this is a live instance reflecting the current state; not intended to be stored)
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
	 * <p>For a corresponding callback after every retry attempt, consider
	 * {@link #onRetryableExecution(RetryPolicy, Retryable, RetryState)}, ignoring the
	 * initial attempt through a corresponding {@link RetryState#getRetryCount()} check.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @param retryState the current state of retry processing
	 * (this is a live instance reflecting the current state; not intended to be stored)
	 * @since 7.0.4
	 * @see #onRetryableExecution(RetryPolicy, Retryable, RetryState)
	 */
	default void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
		beforeRetry(retryPolicy, retryable);
	}

	/**
	 * Called before every retry attempt.
	 * <p>Called by {@link #beforeRetry(RetryPolicy, Retryable, RetryState)}.
	 * Implement either this reduced method or that full-argument method, not both.
	 * @param retryPolicy the {@link RetryPolicy}
	 * @param retryable the {@link Retryable} operation
	 * @see #beforeRetry(RetryPolicy, Retryable, RetryState)
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
