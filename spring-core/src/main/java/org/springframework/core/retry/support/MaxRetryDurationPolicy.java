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

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} based on a timeout.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public class MaxRetryDurationPolicy implements RetryPolicy {

	private Duration maxRetryDuration = Duration.ofSeconds(3); // what would be a reasonable here?

	/**
	 * Start a new retry execution.
	 * @return a fresh {@link MaxRetryDurationPolicyExecution} ready to be used
	 */
	@Override
	public MaxRetryDurationPolicyExecution start() {
		return new MaxRetryDurationPolicyExecution();
	}

	/**
	 * Set the maximum retry duration.
	 * @param maxRetryDuration the maximum retry duration. Must be positive.
	 */
	public void setMaxRetryDuration(Duration maxRetryDuration) {
		Assert.isTrue(!maxRetryDuration.isNegative() && !maxRetryDuration.isZero(),
				"Max retry duration must be positive");
		this.maxRetryDuration = maxRetryDuration;
	}

	/**
	 * A {@link RetryExecution} based on a maximum retry duration.
	 */
	public class MaxRetryDurationPolicyExecution implements RetryExecution {

		protected LocalDateTime retryStartTime = LocalDateTime.now();

		@Override
		public boolean shouldRetry(Throwable throwable) {
			Duration currentRetryDuration = Duration.between(this.retryStartTime, LocalDateTime.now());
			return currentRetryDuration.compareTo(maxRetryDuration) <= 0;
		}

	}
}
