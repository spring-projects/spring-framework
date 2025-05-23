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

import java.time.Duration;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.support.MaxAttemptsRetryPolicy;
import org.springframework.core.retry.support.backoff.FixedBackOffPolicy;
import org.springframework.core.retry.support.listener.CompositeRetryListener;
import org.springframework.util.Assert;

/**
 * A basic implementation of {@link RetryOperations} that uses a
 * {@link RetryPolicy} and a {@link BackOffPolicy} to retry a
 * {@link Callable} piece of code. By default, the callback will be called
 * 3 times (<code>MaxAttemptsRetryPolicy(3)</code>) with a fixed backoff
 * of 1 second (<code>FixedBackOffPolicy(Duration.ofSeconds(1))</code>).
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
 * @see BackOffPolicy
 * @see RetryListener
 */
public class RetryTemplate implements RetryOperations {

	protected final Log logger = LogFactory.getLog(getClass());

	private RetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3);

	private BackOffPolicy backOffPolicy = new FixedBackOffPolicy(Duration.ofSeconds(1));

	private RetryListener retryListener = new RetryListener() {
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
	 * Create a new retry template with a custom {@link RetryPolicy} and {@link BackOffPolicy}.
	 * @param retryPolicy the retry policy to use
	 * @param backOffPolicy the backoff policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy, BackOffPolicy backOffPolicy) {
		this(retryPolicy);
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set the {@link RetryPolicy} to use. Defaults to <code>MaxAttemptsRetryPolicy(3)</code>.
	 * @param retryPolicy the retry policy to use. Must not be <code>null</code>.
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Set the {@link BackOffPolicy} to use. Defaults to <code>FixedBackOffPolicy(Duration.ofSeconds(1))</code>.
	 * @param backOffPolicy the backoff policy to use. Must not be <code>null</code>.
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set a {@link RetryListener} to use. Defaults to a <code>NoOp</code> implementation.
	 * If multiple listeners are needed, use a {@link CompositeRetryListener}.
	 * @param retryListener the retry listener to use. Must not be <code>null</code>.
	 */
	public void setRetryListener(RetryListener retryListener) {
		Assert.notNull(retryListener, "Retry listener must not be null");
		this.retryListener = retryListener;
	}

	/**
	 * Call the retry callback according to the configured retry and backoff policies.
	 * If the callback succeeds, its result is returned. Otherwise, the last exception will
	 * be propagated to the caller.
	 * @param retryCallback the callback to call initially and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the callback if any
	 * @throws RetryException thrown if the retry policy is exhausted
	 */
	@Override
	public <R> @Nullable R execute(RetryCallback<R> retryCallback) throws RetryException {
		Assert.notNull(retryCallback, "Retry Callback must not be null");
		int attempts = 0;
		int maxAttempts = this.retryPolicy.getMaxAttempts();
		while (attempts++ <= maxAttempts) {
			String callbackName = retryCallback.getName();
			if (logger.isDebugEnabled()) {
				logger.debug("Retry callback '" + callbackName + "'  attempt #" + attempts);
			}
			try {
				this.retryListener.beforeRetry();
				R result = retryCallback.run();
				this.retryListener.onSuccess(result);
				if (logger.isDebugEnabled()) {
					logger.debug("Retry callback '" + callbackName + "' attempt #" + attempts + " succeeded");
				}
				return result;
			}
			catch (Exception exception) {
				if (!this.retryPolicy.retryOn().test(exception)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Retry callback '" + callbackName + "' aborted on " + exception.getMessage(), exception);
					}
					break;
				}
				this.retryListener.onFailure(exception);
				Duration duration = this.backOffPolicy.backOff();
				if (logger.isDebugEnabled()) {
					logger.debug("Retry callback '" + callbackName + "' attempt #" + attempts + " failed, backing off for " + duration.toMillis() + "ms");
				}
				try {
					Thread.sleep(duration.toMillis());
				}
				catch (InterruptedException interruptedException) {
					throw new RetryException("Unable to backoff for retry callback '" + callbackName + "'", interruptedException);
				}
				if (attempts >= maxAttempts) {
					this.retryListener.onMaxAttempts(exception);
					throw new RetryException("Retry callback '" + callbackName + "' exceeded maximum retry attempts " + attempts + ", aborting execution", exception);
				}
			}
		}
		return null;
	}

}
