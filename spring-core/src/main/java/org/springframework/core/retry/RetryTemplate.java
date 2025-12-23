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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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


	@Override
	public <R extends @Nullable Object> R execute(Retryable<R> retryable) throws RetryException {
		long startTime = System.currentTimeMillis();
		String retryableName = retryable.getName();
		MutableRetryState retryState = new MutableRetryState();

		// Initial attempt
		logger.debug(() -> "Preparing to execute retryable operation '%s'".formatted(retryableName));
		R result;
		try {
			result = retryable.execute();
		}
		catch (Throwable initialException) {
			logger.debug(initialException,
					() -> "Execution of retryable operation '%s' failed; initiating the retry process"
							.formatted(retryableName));
			retryState.addException(initialException);
			this.retryListener.onRetryableExecution(this.retryPolicy, retryable, retryState);

			// Retry process starts here
			BackOffExecution backOffExecution = this.retryPolicy.getBackOff().start();
			Throwable lastException = initialException;
			long timeout = this.retryPolicy.getTimeout().toMillis();

			while (this.retryPolicy.shouldRetry(lastException) && retryState.getRetryCount() < Integer.MAX_VALUE) {
				checkIfTimeoutExceeded(timeout, startTime, 0, retryable, retryState);

				try {
					long sleepTime = backOffExecution.nextBackOff();
					if (sleepTime == BackOffExecution.STOP) {
						break;
					}
					checkIfTimeoutExceeded(timeout, startTime, sleepTime, retryable, retryState);
					logger.debug(() -> "Backing off for %dms after retryable operation '%s'"
							.formatted(sleepTime, retryableName));
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					RetryException retryException = new RetryException("Interrupted during back-off for " +
							"retryable operation '%s'; aborting execution".formatted(retryableName), retryState);
					this.retryListener.onRetryPolicyInterruption(this.retryPolicy, retryable, retryException);
					throw retryException;
				}

				logger.debug(() -> "Preparing to retry operation '%s'".formatted(retryableName));
				retryState.increaseRetryCount();
				this.retryListener.beforeRetry(this.retryPolicy, retryable);
				try {
					result = retryable.execute();
				}
				catch (Throwable currentException) {
					logger.debug(currentException, () -> "Retry attempt for operation '%s' failed due to '%s'"
							.formatted(retryableName, currentException));
					retryState.addException(currentException);
					this.retryListener.onRetryFailure(this.retryPolicy, retryable, currentException);
					this.retryListener.onRetryableExecution(this.retryPolicy, retryable, retryState);
					lastException = currentException;
					continue;
				}

				// Did not enter catch block above -> retry success.
				logger.debug(() -> "Retryable operation '%s' completed successfully after retry"
						.formatted(retryableName));
				this.retryListener.onRetrySuccess(this.retryPolicy, retryable, result);
				this.retryListener.onRetryableExecution(this.retryPolicy, retryable, retryState);
				return result;
			}

			// The RetryPolicy has exhausted at this point, so we throw a RetryException with the
			// last exception as the cause and remaining exceptions as suppressed exceptions.
			RetryException retryException = new RetryException(
					"Retry policy for operation '%s' exhausted; aborting execution".formatted(retryableName),
					retryState);
			this.retryListener.onRetryPolicyExhaustion(this.retryPolicy, retryable, retryException);
			throw retryException;
		}

		// Never entered initial catch block -> initial success.
		logger.debug(() -> "Retryable operation '%s' completed successfully".formatted(retryableName));
		this.retryListener.onRetryableExecution(this.retryPolicy, retryable, retryState);
		return result;
	}

	private void checkIfTimeoutExceeded(long timeout, long startTime, long sleepTime, Retryable<?> retryable,
			RetryState retryState) throws RetryException {

		if (timeout > 0) {
			// If sleepTime > 0, we are predicting what the effective elapsed time
			// would be if we were to sleep for sleepTime milliseconds.
			long elapsedTime = System.currentTimeMillis() + sleepTime - startTime;
			if (elapsedTime >= timeout) {
				String message = (sleepTime > 0 ? """
						Retry policy for operation '%s' would exceed timeout (%dms) due \
						to pending sleep time (%dms); preemptively aborting execution"""
								.formatted(retryable.getName(), timeout, sleepTime) :
						"Retry policy for operation '%s' exceeded timeout (%dms); aborting execution"
								.formatted(retryable.getName(), timeout));
				RetryException retryException = new RetryException(message, retryState);
				this.retryListener.onRetryPolicyTimeout(this.retryPolicy, retryable, retryException);
				throw retryException;
			}
		}
	}

	@Override
	public <R extends @Nullable Object> R invoke(Supplier<R> retryable) {
		try {
			return execute(new Retryable<>() {
				@Override
				public R execute() {
					return retryable.get();
				}
				@Override
				public String getName() {
					return retryable.getClass().getName();
				}
			});
		}
		catch (RetryException retryException) {
			Throwable ex = retryException.getCause();
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (ex instanceof Error error) {
				throw error;
			}
			throw new UndeclaredThrowableException(ex);
		}
	}

	@Override
	public void invoke(Runnable retryable) {
		try {
			execute(new Retryable<>() {
				@Override
				public Void execute() {
					retryable.run();
					return null;
				}
				@Override
				public String getName() {
					return retryable.getClass().getName();
				}
			});
		}
		catch (RetryException retryException) {
			Throwable ex = retryException.getCause();
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (ex instanceof Error error) {
				throw error;
			}
			throw new UndeclaredThrowableException(ex);
		}
	}


	private static class MutableRetryState implements RetryState {

		private int retryCount;

		private final List<Throwable> exceptions = new ArrayList<>(4);

		public void increaseRetryCount(){
			this.retryCount++;
		}

		@Override
		public int getRetryCount() {
			return this.retryCount;
		}

		public void addException(Throwable exception) {
			this.exceptions.add(exception);
		}

		@Override
		public List<Throwable> getExceptions() {
			return Collections.unmodifiableList(this.exceptions);
		}

		@Override
		public String toString() {
			return "RetryState: retryCount=" + this.retryCount + ", exceptions=" + this.exceptions;
		}
	}

}
