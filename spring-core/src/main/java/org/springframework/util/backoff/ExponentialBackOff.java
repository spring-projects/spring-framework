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

package org.springframework.util.backoff;

import java.util.StringJoiner;

import org.springframework.util.Assert;

/**
 * Implementation of {@link BackOff} that increases the back-off period for each attempt.
 * When the interval has reached the {@linkplain #setMaxInterval max interval}, it is no
 * longer increased. Stops once the {@linkplain #setMaxElapsedTime max elapsed time} or
 * {@linkplain #setMaxAttempts max attempts} has been reached.
 *
 * <p>Example: The default interval is {@value #DEFAULT_INITIAL_INTERVAL} ms;
 * the default multiplier is {@value #DEFAULT_MULTIPLIER}; and the default max
 * interval is {@value #DEFAULT_MAX_INTERVAL}. For 10 attempts the sequence will be
 * as follows:
 *
 * <pre>
 * request#     back-off
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
 * @author Juergen Hoeller
 * @since 4.1
 */
public class ExponentialBackOff implements BackOff {

	/**
	 * The default initial interval: {@value} ms.
	 */
	public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

	/**
	 * The default jitter value for each interval: {@value} ms.
	 * @since 7.0
	 */
	public static final long DEFAULT_JITTER = 0;

	/**
	 * The default multiplier (increases the interval by 50%): {@value}.
	 */
	public static final double DEFAULT_MULTIPLIER = 1.5;

	/**
	 * The default maximum back-off time: {@value} ms.
	 */
	public static final long DEFAULT_MAX_INTERVAL = 30_000L;

	/**
	 * The default maximum elapsed time: unlimited.
	 */
	public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;

	/**
	 * The default maximum attempts: unlimited.
	 * @since 6.1
	 */
	public static final long DEFAULT_MAX_ATTEMPTS = Long.MAX_VALUE;


	private long initialInterval = DEFAULT_INITIAL_INTERVAL;

	private long jitter = DEFAULT_JITTER;

	private double multiplier = DEFAULT_MULTIPLIER;

	private long maxInterval = DEFAULT_MAX_INTERVAL;

	private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;

	private long maxAttempts = DEFAULT_MAX_ATTEMPTS;


	/**
	 * Create an instance with the default settings.
	 * @see #DEFAULT_INITIAL_INTERVAL
	 * @see #DEFAULT_JITTER
	 * @see #DEFAULT_MULTIPLIER
	 * @see #DEFAULT_MAX_INTERVAL
	 * @see #DEFAULT_MAX_ELAPSED_TIME
	 * @see #DEFAULT_MAX_ATTEMPTS
	 */
	public ExponentialBackOff() {
	}

	/**
	 * Create an instance with the supplied settings.
	 * @param initialInterval the initial interval in milliseconds
	 * @param multiplier the multiplier (must be greater than or equal to 1)
	 */
	public ExponentialBackOff(long initialInterval, double multiplier) {
		checkMultiplier(multiplier);
		this.initialInterval = initialInterval;
		this.multiplier = multiplier;
	}


	/**
	 * Set the initial interval.
	 * @param initialInterval the initial interval in milliseconds
	 */
	public void setInitialInterval(long initialInterval) {
		this.initialInterval = initialInterval;
	}

	/**
	 * Return the initial interval in milliseconds.
	 */
	public long getInitialInterval() {
		return this.initialInterval;
	}

	/**
	 * Set the jitter value to apply for each interval, leading to random
	 * milliseconds to be subtracted or added and resulting in a value between
	 * {@code interval - jitter} and {@code interval + jitter} but never below
	 * {@code initialInterval} or above {@code maxInterval}.
	 * <p>If a {@code multiplier} is specified, it is applied to the jitter value
	 * as well.
	 * @param jitter the jitter value in milliseconds
	 * @since 7.0
	 */
	public void setJitter(long jitter) {
		Assert.isTrue(jitter >= 0, () -> "Invalid jitter '" + jitter + "': must be >= 0.");
		this.jitter = jitter;
	}

	/**
	 * Return the jitter value to apply for each interval in milliseconds.
	 * @since 7.0
	 */
	public long getJitter() {
		return this.jitter;
	}

	/**
	 * Set the value to multiply the current interval by for each attempt.
	 * <p>This applies to the {@linkplain #setInitialInterval initial interval}
	 * as well as the {@linkplain #setJitter jitter range}.
	 * @param multiplier the multiplier (must be greater than or equal to 1)
	 */
	public void setMultiplier(double multiplier) {
		checkMultiplier(multiplier);
		this.multiplier = multiplier;
	}

	private void checkMultiplier(double multiplier) {
		Assert.isTrue(multiplier >= 1, () -> "Invalid multiplier '" + multiplier + "': " +
				"Should be greater than or equal to 1. A multiplier of 1 is equivalent to a fixed interval.");
	}

	/**
	 * Return the value to multiply the current interval by for each attempt.
	 */
	public double getMultiplier() {
		return this.multiplier;
	}

	/**
	 * Set the maximum back-off time in milliseconds.
	 */
	public void setMaxInterval(long maxInterval) {
		this.maxInterval = maxInterval;
	}

	/**
	 * Return the maximum back-off time in milliseconds.
	 */
	public long getMaxInterval() {
		return this.maxInterval;
	}

	/**
	 * Set the maximum elapsed time in milliseconds after which a call to
	 * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
	 * @param maxElapsedTime the maximum elapsed time
	 * @see #setMaxAttempts
	 */
	public void setMaxElapsedTime(long maxElapsedTime) {
		this.maxElapsedTime = maxElapsedTime;
	}

	/**
	 * Return the maximum elapsed time in milliseconds after which a call to
	 * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
	 * @return the maximum elapsed time
	 * @see #getMaxAttempts()
	 */
	public long getMaxElapsedTime() {
		return this.maxElapsedTime;
	}

	/**
	 * The maximum number of attempts after which a call to
	 * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
	 * @param maxAttempts the maximum number of attempts
	 * @since 6.1
	 * @see #setMaxElapsedTime
	 */
	public void setMaxAttempts(long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Return the maximum number of attempts after which a call to
	 * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
	 * @return the maximum number of attempts
	 * @since 6.1
	 * @see #getMaxElapsedTime()
	 */
	public long getMaxAttempts() {
		return this.maxAttempts;
	}


	@Override
	public BackOffExecution start() {
		return new ExponentialBackOffExecution();
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", "ExponentialBackOff[", "]")
				.add("initialInterval=" + this.initialInterval)
				.add("jitter=" + this.jitter)
				.add("multiplier=" + this.multiplier)
				.add("maxInterval=" + this.maxInterval)
				.add("maxElapsedTime=" + this.maxElapsedTime)
				.add("maxAttempts=" + this.maxAttempts)
				.toString();
	}


	private class ExponentialBackOffExecution implements BackOffExecution {

		private long currentInterval = -1;

		private long currentElapsedTime = 0;

		private int attempts = 0;

		@Override
		public long nextBackOff() {
			if (this.currentElapsedTime >= getMaxElapsedTime() || this.attempts >= getMaxAttempts()) {
				return STOP;
			}
			long nextInterval = computeNextInterval();
			this.currentElapsedTime += nextInterval;
			this.attempts++;
			return nextInterval;
		}

		private long computeNextInterval() {
			long maxInterval = getMaxInterval();
			long nextInterval;
			if (this.currentInterval < 0) {
				nextInterval = getInitialInterval();
			}
			else if (this.currentInterval >= maxInterval) {
				nextInterval = maxInterval;
			}
			else {
				nextInterval = Math.min((long) (this.currentInterval * getMultiplier()), maxInterval);
			}
			this.currentInterval = nextInterval;
			return Math.min(applyJitter(nextInterval), maxInterval);
		}

		private long applyJitter(long interval) {
			long jitter = getJitter();
			if (jitter > 0) {
				long initialInterval = getInitialInterval();
				long applicableJitter = jitter * (interval / initialInterval);
				long min = Math.max(interval - applicableJitter, initialInterval);
				long max = Math.min(interval + applicableJitter, getMaxInterval());
				return min + (long) (Math.random() * (max - min));
			}
			return interval;
		}

		@Override
		public String toString() {
			String currentIntervalDescription = this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms";
			return new StringJoiner(", ", "ExponentialBackOffExecution[", "]")
					.add("currentInterval=" + currentIntervalDescription)
					.add("multiplier=" + getMultiplier())
					.add("attempts=" + this.attempts)
					.toString();
		}
	}

}
