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

import java.time.Duration;

/**
 * A simple {@link BackOff} implementation that provides a fixed interval
 * between two attempts and a maximum number of retries.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class FixedBackOff implements BackOff {

	/**
	 * The default recovery interval: 5000 ms = 5 seconds.
	 */
	public static final long DEFAULT_INTERVAL = 5000;

	/**
	 * Constant value indicating an unlimited number of attempts.
	 */
	public static final long UNLIMITED_ATTEMPTS = Long.MAX_VALUE;


	private long interval = DEFAULT_INTERVAL;

	private long maxAttempts = UNLIMITED_ATTEMPTS;


	/**
	 * Create an instance with an interval of {@value #DEFAULT_INTERVAL} ms and
	 * an unlimited number of attempts.
	 * @see #setInterval(long)
	 * @see #setMaxAttempts(long)
	 */
	public FixedBackOff() {
	}

	/**
	 * Create an instance with the supplied interval and an unlimited number of
	 * attempts.
	 * @param interval the interval between two attempts in milliseconds
	 * @since 7.0
	 * @see #setMaxAttempts(long)
	 */
	public FixedBackOff(long interval) {
		this.interval = interval;
	}

	/**
	 * Create an instance with the supplied interval and an unlimited number of
	 * attempts.
	 * @param interval the interval between two attempts
	 * @since 7.0
	 * @see #setMaxAttempts(long)
	 */
	public FixedBackOff(Duration interval) {
		this.interval = interval.toMillis();
	}

	/**
	 * Create an instance with the supplied interval and maximum number of attempts.
	 * @param interval the interval between two attempts in milliseconds
	 * @param maxAttempts the maximum number of attempts
	 */
	public FixedBackOff(long interval, long maxAttempts) {
		this.interval = interval;
		this.maxAttempts = maxAttempts;
	}


	/**
	 * Set the interval between two attempts in milliseconds.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * Return the interval between two attempts in milliseconds.
	 */
	public long getInterval() {
		return this.interval;
	}

	/**
	 * Set the maximum number of attempts.
	 */
	public void setMaxAttempts(long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Return the maximum number of attempts.
	 */
	public long getMaxAttempts() {
		return this.maxAttempts;
	}


	@Override
	public BackOffExecution start() {
		return new FixedBackOffExecution();
	}


	private class FixedBackOffExecution implements BackOffExecution {

		private long currentAttempts = 0;

		@Override
		public long nextBackOff() {
			this.currentAttempts++;
			if (this.currentAttempts <= getMaxAttempts()) {
				return getInterval();
			}
			else {
				return STOP;
			}
		}

		@Override
		public String toString() {
			String attemptValue = (FixedBackOff.this.maxAttempts == Long.MAX_VALUE ?
					"unlimited" : String.valueOf(FixedBackOff.this.maxAttempts));
			return "FixedBackOff{interval=" + FixedBackOff.this.interval +
					", currentAttempts=" + this.currentAttempts +
					", maxAttempts=" + attemptValue +
					'}';
		}
	}

}
