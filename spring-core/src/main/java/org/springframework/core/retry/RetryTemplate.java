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

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogAccessor;
import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

/**
 * A basic implementation of {@link RetryOperations} that executes and potentially
 * retries a {@link Retryable} operation based on a configured {@link RetryPolicy}.
 *
 * <p>By default, a retryable operation will be executed once and potentially
 * retried at most 3 times with a fixed backoff of 1 second.
 *
 * <p>A {@link RetryListener} can be {@linkplain #setRetryListener(RetryListener)
 * registered} to react to events published during key retry phases (before a
 * retry attempt, after a retry attempt, etc.).
 *
 * <p>All retry actions performed by this template are logged at debug level, using
 * {@code "org.springframework.core.retry.RetryTemplate"} as the log category.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 7.0
 * @see RetryOperations
 * @see RetryPolicy
 * @see BackOff
 * @see RetryListener
 * @see Retryable
 */
public class RetryTemplate implements RetryOperations {

	private static final LogAccessor logger = new LogAccessor(RetryTemplate.class);

	private RetryPolicy retryPolicy = RetryPolicy.withDefaults();

	private RetryListener retryListener = new RetryListener() {};


	/**
	 * Create a new {@code RetryTemplate} with maximum 3 retry attempts and a
	 * fixed backoff of 1 second.
	 * @see RetryPolicy#withDefaults()
	 */
	public RetryTemplate() {
	}

	/**
	 * Create a new {@code RetryTemplate} with the supplied {@link RetryPolicy}.
	 * @param retryPolicy the retry policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "RetryPolicy must not be null");
		this.retryPolicy = retryPolicy;
	}


	/**
	 * Set the {@link RetryPolicy} to use.
	 * <p>Defaults to {@code RetryPolicy.withDefaults()}.
	 * @param retryPolicy the retry policy to use
	 * @see RetryPolicy#withDefaults()
	 * @see RetryPolicy#withMaxRetries(long)
	 * @see RetryPolicy#builder()
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Return the current {@link RetryPolicy} that is in use
	 * with this template.
	 */
	public RetryPolicy getRetryPolicy() {
		return this.retryPolicy;
	}

	/**
	 * Set the {@link RetryListener} to use.
	 * <p>If multiple listeners are needed, use a
	 * {@link org.springframework.core.retry.support.CompositeRetryListener}.
	 * <p>Defaults to a <em>no-op</em> implementation.
	 * @param retryListener the retry listener to use
	 */
	public void setRetryListener(RetryListener retryListener) {
		Assert.notNull(retryListener, "Retry listener must not be null");
		this.retryListener = retryListener;
	}

	/**
	 * Return the current {@link RetryListener} that is in use
	 * with this template.
	 */
	public RetryListener getRetryListener() {
		return this.retryListener;
	}


	/**
	 * Execute the supplied {@link Retryable} operation according to the configured
	 * {@link RetryPolicy}.
	 * <p>If the {@code Retryable} succeeds, its result will be returned. Otherwise, a
	 * {@link RetryException} will be thrown to the caller. The {@code RetryException}
	 * will contain the last exception thrown by the {@code Retryable} operation as the
	 * {@linkplain RetryException#getCause() cause} and any exceptions from previous
	 * attempts as {@linkplain RetryException#getSuppressed() suppressed exceptions}.
	 * @param retryable the {@code Retryable} to execute and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the {@code Retryable}, if any
	 * @throws RetryException if the {@code RetryPolicy} is exhausted
	 */
	@Override
	public <R extends @Nullable Object> R execute(Retryable<R> retryable) throws RetryException {
		String retryableName = retryable.getName();
		long startTime = System.currentTimeMillis();
		// Initial attempt
		try {
			logger.debug(() -> "Preparing to execute retryable operation '%s'".formatted(retryableName));
			R result = retryable.execute();
			logger.debug(() -> "Retryable operation '%s' completed successfully".formatted(retryableName));
			return result;
		}
		catch (Throwable initialException) {
			logger.debug(initialException,
					() -> "Execution of retryable operation '%s' failed; initiating the retry process"
							.formatted(retryableName));
			// Retry process starts here
			BackOffExecution backOffExecution = this.retryPolicy.getBackOff().start();
			Deque<Throwable> exceptions = new ArrayDeque<>(4);
			exceptions.add(initialException);

			Throwable lastException = initialException;
			long timeout = this.retryPolicy.getTimeout().toMillis();
			while (this.retryPolicy.shouldRetry(lastException)) {
				checkIfTimeoutExceeded(timeout, startTime, 0, retryable, exceptions);
				try {
					long sleepTime = backOffExecution.nextBackOff();
					if (sleepTime == BackOffExecution.STOP) {
						break;
					}
					checkIfTimeoutExceeded(timeout, startTime, sleepTime, retryable, exceptions);
					logger.debug(() -> "Backing off for %dms after retryable operation '%s'"
							.formatted(sleepTime, retryableName));
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					RetryException retryException = new RetryInterruptedException(
							"Unable to back off for retryable operation '%s'".formatted(retryableName),
							interruptedException);
					exceptions.forEach(retryException::addSuppressed);
					this.retryListener.onRetryPolicyInterruption(this.retryPolicy, retryable, retryException);
					throw retryException;
				}
				logger.debug(() -> "Preparing to retry operation '%s'".formatted(retryableName));
				try {
					this.retryListener.beforeRetry(this.retryPolicy, retryable);
					R result = retryable.execute();
					this.retryListener.onRetrySuccess(this.retryPolicy, retryable, result);
					logger.debug(() -> "Retryable operation '%s' completed successfully after retry"
							.formatted(retryableName));
					return result;
				}
				catch (Throwable currentException) {
					logger.debug(currentException, () -> "Retry attempt for operation '%s' failed due to '%s'"
							.formatted(retryableName, currentException));
					this.retryListener.onRetryFailure(this.retryPolicy, retryable, currentException);
					exceptions.add(currentException);
					lastException = currentException;
				}
			}

			// The RetryPolicy has exhausted at this point, so we throw a RetryException with the
			// last exception as the cause and remaining exceptions as suppressed exceptions.
			RetryException retryException = new RetryException(
					"Retry policy for operation '%s' exhausted; aborting execution".formatted(retryableName),
					exceptions.removeLast());
			exceptions.forEach(retryException::addSuppressed);
			this.retryListener.onRetryPolicyExhaustion(this.retryPolicy, retryable, retryException);
			throw retryException;
		}
	}

	private void checkIfTimeoutExceeded(long timeout, long startTime, long sleepTime, Retryable<?> retryable,
			Deque<Throwable> exceptions) throws RetryException {

		if (timeout != 0) {
			// If sleepTime > 0, we are predicting what the effective elapsed time
			// would be if we were to sleep for sleepTime milliseconds.
			long elapsedTime = System.currentTimeMillis() + sleepTime - startTime;
			if (elapsedTime >= timeout) {
				RetryException retryException = new RetryException(
						"Retry policy for operation '%s' exceeded timeout (%d ms); aborting execution"
						.formatted(retryable.getName(), timeout), exceptions.removeLast());
				exceptions.forEach(retryException::addSuppressed);
				this.retryListener.onRetryPolicyTimeout(this.retryPolicy, retryable, retryException);
				throw retryException;
			}
		}
	}


	private static class RetryInterruptedException extends RetryException {

		@Serial
		private static final long serialVersionUID = 1L;


		RetryInterruptedException(String message, InterruptedException cause) {
			super(message, cause);
		}

		@Override
		public int getRetryCount() {
			return (getSuppressed().length - 1);
		}
	}

}
