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

package org.springframework.core.retry.support;

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} based on a number of attempts that should not exceed a maximum number.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public class MaxRetryAttemptsPolicy implements RetryPolicy {

	private int maxRetryAttempts = 3;

	/**
	 * Start a new retry execution.
	 * @return a fresh {@link MaxRetryAttemptsPolicyExecution} ready to be used
	 */
	@Override
	public MaxRetryAttemptsPolicyExecution start() {
		return new MaxRetryAttemptsPolicyExecution();
	}

	/**
	 * Set the maximum number of retry attempts.
	 * @param maxRetryAttempts the maximum number of retry attempts. Must be greater than zero.
	 */
	public void setMaxRetryAttempts(int maxRetryAttempts) {
		Assert.isTrue(maxRetryAttempts > 0, "Max retry attempts must be greater than zero");
		this.maxRetryAttempts = maxRetryAttempts;
	}

	/**
	 * A {@link RetryExecution} based on a maximum number of retry attempts.
	 */
	public class MaxRetryAttemptsPolicyExecution implements RetryExecution {

		protected int retryAttempts;

		public MaxRetryAttemptsPolicyExecution() {
			this.retryAttempts = 0;
		}

		@Override
		public boolean shouldRetry(Throwable throwable) {
			return this.retryAttempts++ < maxRetryAttempts;
		}

	}

}
