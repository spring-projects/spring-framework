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

import java.time.Duration;
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
 * <p>By default, a retryable operation will be retried at most 3 times with a
 * fixed backoff of 1 second.
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
	 * @see RetryPolicy#withMaxAttempts(long)
	 * @see RetryPolicy#withMaxElapsedTime(Duration)
	 * @see RetryPolicy#builder()
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
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
	 * Execute the supplied {@link Retryable} according to the configured retry
	 * and backoff policies.
	 * <p>If the {@code Retryable} succeeds, its result will be returned. Otherwise,
	 * a {@link RetryException} will be thrown to the caller.
	 * @param retryable the {@code Retryable} to execute and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the {@code Retryable}, if any
	 * @throws RetryException if the {@code RetryPolicy} is exhausted; exceptions
	 * encountered during retry attempts are available as suppressed exceptions
	 */
	@Override
	public <R> @Nullable R execute(Retryable<? extends @Nullable R> retryable) throws RetryException {
		String retryableName = retryable.getName();
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
			Deque<Throwable> exceptions = new ArrayDeque<>();
			exceptions.add(initialException);

			Throwable retryException = initialException;
			while (this.retryPolicy.shouldRetry(retryException)) {
				try {
					long duration = backOffExecution.nextBackOff();
					if (duration == BackOffExecution.STOP) {
						break;
					}
					logger.debug(() -> "Backing off for %dms after retryable operation '%s'"
							.formatted(duration, retryableName));
					Thread.sleep(duration);
				}
				catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					throw new RetryException(
							"Unable to back off for retryable operation '%s'".formatted(retryableName),
							interruptedException);
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
				catch (Throwable currentAttemptException) {
					logger.debug(() -> "Retry attempt for operation '%s' failed due to '%s'"
							.formatted(retryableName, currentAttemptException));
					this.retryListener.onRetryFailure(this.retryPolicy, retryable, currentAttemptException);
					exceptions.add(currentAttemptException);
					retryException = currentAttemptException;
				}
			}

			// The RetryPolicy has exhausted at this point, so we throw a RetryException with the
			// initial exception as the cause and remaining exceptions as suppressed exceptions.
			RetryException finalException = new RetryException(
					"Retry policy for operation '%s' exhausted; aborting execution".formatted(retryableName),
					exceptions.removeLast());
			exceptions.forEach(finalException::addSuppressed);
			this.retryListener.onRetryPolicyExhaustion(this.retryPolicy, retryable, finalException);
			throw finalException;
		}
	}

}
