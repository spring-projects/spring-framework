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

package org.springframework.core.retry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.retry.support.CompositeRetryListener;
import org.springframework.core.retry.support.MaxRetryAttemptsPolicy;
import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

/**
 * A basic implementation of {@link RetryOperations} that uses a
 * {@link RetryPolicy} and a {@link BackOff} to retry a
 * {@link RetryCallback}. By default, the callback will be called
 * 3 times with a fixed backoff of 1 second.
 *
 * <p>It is also possible to register a {@link RetryListener} to intercept and inject code
 * during key retry phases (before a retry attempt, after a retry attempt, etc.).
 *
 * <p>All retry operations performed by this class are logged at debug level,
 * using "org.springframework.core.retry.RetryTemplate" as log category.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryOperations
 * @see RetryPolicy
 * @see BackOff
 * @see RetryListener
 */
public class RetryTemplate implements RetryOperations {

	protected final LogAccessor logger = new LogAccessor(LogFactory.getLog(getClass()));

	protected RetryPolicy retryPolicy = new MaxRetryAttemptsPolicy();

	protected BackOff backOffPolicy = new FixedBackOff();

	protected RetryListener retryListener = new RetryListener() {
	};

	/**
	 * Create a new retry template with default settings.
	 */
	public RetryTemplate() {
	}

	/**
	 * Create a new retry template with a custom {@link RetryPolicy}.
	 * @param retryPolicy the retry policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Create a new retry template with a custom {@link RetryPolicy} and {@link BackOff}.
	 * @param retryPolicy the retry policy to use
	 * @param backOffPolicy the backoff policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy, BackOff backOffPolicy) {
		this(retryPolicy);
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set the {@link RetryPolicy} to use. Defaults to <code>MaxAttemptsRetryPolicy()</code>.
	 * @param retryPolicy the retry policy to use. Must not be <code>null</code>.
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Set the {@link BackOff} to use. Defaults to <code>FixedBackOffPolicy(Duration.ofSeconds(1))</code>.
	 * @param backOffPolicy the backoff policy to use. Must not be <code>null</code>.
	 */
	public void setBackOffPolicy(BackOff backOffPolicy) {
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set the {@link RetryListener} to use. Defaults to a <code>NoOp</code> implementation.
	 * If multiple listeners are needed, use a {@link CompositeRetryListener}.
	 * @param retryListener the retry listener to use. Must not be <code>null</code>.
	 */
	public void setRetryListener(RetryListener retryListener) {
		Assert.notNull(retryListener, "Retry listener must not be null");
		this.retryListener = retryListener;
	}

	/**
	 * Call the retry callback according to the configured retry and backoff policies.
	 * If the callback succeeds, its result is returned. Otherwise, a {@link RetryException}
	 * will be thrown to the caller having all attempt exceptions as suppressed exceptions.
	 * @param retryCallback the callback to call initially and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the callback if any
	 * @throws RetryException thrown if the retry policy is exhausted. All attempt exceptions
	 * are added as suppressed exceptions to the final exception.
	 */
	@Override
	public <R extends @Nullable Object> R execute(RetryCallback<R> retryCallback) throws RetryException {
		Assert.notNull(retryCallback, "Retry Callback must not be null");
		String callbackName = retryCallback.getName();
		// initial attempt
		try {
			logger.debug(() -> "About to execute callback '" + callbackName + "'");
			R result = retryCallback.run();
			logger.debug(() -> "Callback '" + callbackName + "' executed successfully");
			return result;
		}
		catch (Throwable initialException) {
			logger.debug(initialException, () -> "Execution of callback '" + callbackName + "' failed, initiating the retry process");
			// retry process starts here
			RetryExecution retryExecution = this.retryPolicy.start();
			BackOffExecution backOffExecution = this.backOffPolicy.start();
			List<Throwable> suppressedExceptions = new ArrayList<>();

			Throwable retryException = initialException;
			while (retryExecution.shouldRetry(retryException)) {
				logger.debug(() -> "About to retry callback '" + callbackName + "'");
				try {
					this.retryListener.beforeRetry(retryExecution);
					R result = retryCallback.run();
					this.retryListener.onRetrySuccess(retryExecution, result);
					logger.debug(() -> "Callback '" + callbackName + "' retried successfully");
					return result;
				}
				catch (Throwable currentAttemptException) {
					this.retryListener.onRetryFailure(retryExecution, currentAttemptException);
					try {
						long duration = backOffExecution.nextBackOff();
						logger.debug(() -> "Retry callback '" + callbackName + "' failed for " + currentAttemptException.getMessage() + ", backing off for " + duration + "ms");
						Thread.sleep(duration);
					}
					catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						throw new RetryException("Unable to backoff for retry callback '" + callbackName + "'", interruptedException);
					}
					suppressedExceptions.add(currentAttemptException);
					retryException = currentAttemptException;
				}
			}
			// retry policy exhausted at this point, throwing a RetryException with the initial exception as cause and remaining attempts exceptions as suppressed
			RetryException finalException = new RetryException("Retry policy for callback '" + callbackName + "' exhausted, aborting execution", initialException);
			suppressedExceptions.forEach(finalException::addSuppressed);
			this.retryListener.onRetryPolicyExhaustion(retryExecution, finalException);
			throw finalException;
		}
	}

}
