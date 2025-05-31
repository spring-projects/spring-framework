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

package org.springframework.util.backoff;

/**
 * Implementation of {@link BackOffPolicy} that increases the back off period for each
 * retry attempt. When the interval has reached the {@linkplain #setMaxInterval
 * max interval}, it is no longer increased. Stops retrying once the
 * {@linkplain #setMaxElapsedTime max elapsed time} has been reached.
 *
 * <p>Example: The default interval is {@value #DEFAULT_INITIAL_INTERVAL} ms;
 * the default multiplier is {@value #DEFAULT_MULTIPLIER}; and the default max
 * interval is {@value #DEFAULT_MAX_INTERVAL}. For 10 attempts the sequence will be
 * as follows:
 *
 * <pre>
 * request#     back off
 *
 *  1              2000
 *  2              3000
 *  3              4500
 *  4              6750
 *  5             10125
 *  6             15187
 *  7             22780
 *  8             30000
 *  9             30000
 * 10             30000
 * </pre>
 *
 * <p>Note that the default max elapsed time is {@link Long#MAX_VALUE}, and the
 * default maximum number of attempts is {@link Integer#MAX_VALUE}.
 * Use {@link #setMaxElapsedTime} to limit the length of time that an instance
 * should accumulate before returning {@link BackOffExecution#STOP}. Alternatively,
 * use {@link #setMaxAttempts} to limit the number of attempts. The execution
 * stops when either of those two limits is reached.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @since 4.1
 * @deprecated Since 7.0, use {@link ExponentialBackOffPolicy} instead.
 */
@Deprecated(since = "7.0")
public class ExponentialBackOff extends ExponentialBackOffPolicy {

	public ExponentialBackOff() {
		super();
	}

	public ExponentialBackOff(long initialInterval, double multiplier) {
		super(initialInterval, multiplier);
	}
}
