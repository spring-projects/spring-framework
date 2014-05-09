/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

/**
 * Implementation of {@link BackOff} that increases the back off period for each
 * retry attempt. When the interval has reached the {@link #setMaxInterval(long)
 * max interval}, it is no longer increased. Stops retrying once the
 * {@link #setMaxElapsedTime(long) max elapsed time} has been reached.
 *
 * <p>Example: The default interval is {@value #DEFAULT_INITIAL_INTERVAL}ms, default
 * multiplier is {@value #DEFAULT_MULTIPLIER} and the default max interval is
 * {@value #DEFAULT_MAX_INTERVAL}. For 10 attempts the sequence will be
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
 * Note that the default max elapsed time is {@link Long#MAX_VALUE}. Use
 * {@link #setMaxElapsedTime(long)} to limit the maximum number of time
 * that an instance should accumulate before returning {@link BackOff#STOP}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class ExponentialBackOff implements BackOff {

	/**
	 * The default initial interval.
	 */
	public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

	/**
	 * The default multiplier (increases the interval by 50%).
	 */
	public static final double DEFAULT_MULTIPLIER = 1.5;

	/**
	 * The default maximum back off time.
	 */
	public static final long DEFAULT_MAX_INTERVAL = 30000L;

	/**
	 * The default maximum elapsed time.
	 */
	public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;


	private long initialInterval = DEFAULT_INITIAL_INTERVAL;

	private double multiplier = DEFAULT_MULTIPLIER;

	private long maxInterval = DEFAULT_MAX_INTERVAL;

	private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;

	private long currentInterval = -1;

	private long currentElapsedTime = 0;

	/**
	 * Create an instance with the default settings.
	 * @see #DEFAULT_INITIAL_INTERVAL
	 * @see #DEFAULT_MULTIPLIER
	 * @see #DEFAULT_MAX_INTERVAL
	 * @see #DEFAULT_MAX_ELAPSED_TIME
	 */
	public ExponentialBackOff() {
	}

	/**
	 * Create an instance.
	 * @param initialInterval the initial interval in milliseconds
	 * @param multiplier the multiplier (should be equal or higher to 1)
	 */
	public ExponentialBackOff(long initialInterval, double multiplier) {
		checkMultiplier(multiplier);
		this.initialInterval = initialInterval;
		this.multiplier = multiplier;
	}

	/**
	 * The initial interval in milliseconds.
	 */
	public void setInitialInterval(long initialInterval) {
		this.initialInterval = initialInterval;
	}

	/**
	 * The value to multiply the current interval with for each retry attempt.
	 */
	public void setMultiplier(double multiplier) {
		checkMultiplier(multiplier);
		this.multiplier = multiplier;
	}

	/**
	 * The maximum back off time.
	 */
	public void setMaxInterval(long maxInterval) {
		this.maxInterval = maxInterval;
	}

	/**
	 * The maximum elapsed time in milliseconds after which a call to
	 * {@link #nextBackOff()} returns {@link BackOff#STOP}.
	 */
	public void setMaxElapsedTime(long maxElapsedTime) {
		this.maxElapsedTime = maxElapsedTime;
	}

	@Override
	public long nextBackOff() {
		if (currentElapsedTime >= maxElapsedTime) {
			return BackOff.STOP;
		}

		long nextInterval = computeNextInterval();
		currentElapsedTime += nextInterval;
		return nextInterval;

	}

	@Override
	public void reset() {
		this.currentInterval = -1;
		this.currentElapsedTime = 0;
	}

	private long computeNextInterval() {
		if (this.currentInterval >= this.maxInterval) {
			return this.maxInterval;
		}
		else if (this.currentInterval < 0) {
			this.currentInterval = (this.initialInterval < this.maxInterval
					? this.initialInterval : this.maxInterval);
		}
		else {
			this.currentInterval = multiplyInterval();
		}
		return currentInterval;
	}

	private long multiplyInterval() {
		long i = this.currentInterval;
		i *= this.multiplier;
		return (i > this.maxInterval ? this.maxInterval :i);
	}

	private void checkMultiplier(double multiplier) {
		if (multiplier < 1) {
			throw new IllegalArgumentException("Invalid multiplier '" + multiplier + "'. Should be equal" +
					"or higher than 1. A multiplier of 1 is equivalent to a fixed interval");
		}
	}

	@Override
	public String toString() {
		String i = (this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms");
		final StringBuilder sb = new StringBuilder("ExponentialBackOff{");
		sb.append("currentInterval=").append(i);
		sb.append(", multiplier=").append(this.multiplier);
		sb.append('}');
		return sb.toString();
	}

}
