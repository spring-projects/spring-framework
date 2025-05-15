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

import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} that signals to the caller to retry up to a maximum number of attempts.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
public class MaxAttemptsRetryPolicy implements RetryPolicy {

	private final int maxAttempts;

	/**
	 * Create a new {@link MaxAttemptsRetryPolicy}.
	 * @param maxAttempts the maximum number of retry attempts. Must be greater than 0.
	 */
	public MaxAttemptsRetryPolicy(int maxAttempts) {
		Assert.isTrue(maxAttempts > 0, "Max attempts must be greater than zero");
		this.maxAttempts = maxAttempts;
	}

	@Override
	public int getMaxAttempts() {
		return this.maxAttempts;
	}

}
