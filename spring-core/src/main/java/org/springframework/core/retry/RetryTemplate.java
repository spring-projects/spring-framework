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
	 * @param <R>           the type of the result
	 * @return the result of the callback if any
	 * @throws Exception thrown if the retry policy is exhausted
	 */
	@Override
	public <R> @Nullable R execute(Callable<R> retryCallback) throws Exception {
		Assert.notNull(retryCallback, "Retry Callback must not be null");
		int attempts = 0;
		int maxAttempts = this.retryPolicy.getMaxAttempts();
		while (attempts++ <= maxAttempts) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retry attempt #" + attempts);
			}
			try {
				this.retryListener.beforeRetry();
				R result = retryCallback.call();
				this.retryListener.onSuccess(result);
				if (logger.isDebugEnabled()) {
					logger.debug("Retry attempt #" + attempts + " succeeded");
				}
				return result;
			}
			catch (Exception exception) {
				this.retryListener.onFailure(exception);
				Duration duration = this.backOffPolicy.backOff();
				Thread.sleep(duration.toMillis());
				if (logger.isDebugEnabled()) {
					logger.debug("Attempt #" + attempts + " failed, backing off for " + duration.toMillis() + "ms");
				}
				if (attempts >= maxAttempts) {
					if (logger.isDebugEnabled()) {
						logger.debug("Maximum retry attempts " + attempts + " exhausted, aborting execution");
					}
					this.retryListener.onMaxAttempts(exception);
					throw exception;
				}
			}
		}
		return null;
	}

}
