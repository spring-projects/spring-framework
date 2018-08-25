package org.springframework.util.asyncassert;
/*
 * Copyright 2002-2018 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.junit.Assert;

/**
 * Utility class for testing asynchronous events.
 *
 * @author Korovin Anatoliy
 */
public class AsyncAssert {

	private Duration timeout;
	private Duration pollingInterval;

	private static final Duration DEFAULT_POLLING_TIME = Duration.ofMillis(10);

	private AsyncAssert() {
		this.pollingInterval = DEFAULT_POLLING_TIME;
	}

	/**
	 * static factory method
	 *
	 * @return new instance of the AsyncAssert
	 */
	public static AsyncAssert get() {
		return new AsyncAssert();
	}

	/**
	 * Set up polling interval settings.
	 *
	 * @param amount duration between condition checks
	 * @param unit   unit of time
	 * @return AsyncAssert with new settings of polling.
	 */
	public AsyncAssert polling(long amount, ChronoUnit unit) {
		this.pollingInterval = Duration.of(amount, unit);
		return this;
	}

	/**
	 * Set up timeout settings
	 *
	 * @param amount how long to wait for conditions
	 * @param unit   unit of time
	 * @return AsyncAssert with new settings of timeout
	 */
	public AsyncAssert timeout(long amount, ChronoUnit unit) {
		this.timeout = Duration.of(amount, unit);
		return this;
	}

	/**
	 * Wait for the condition until the timeout ends.
	 *
	 * Throws the AsyncAssertTimeoutException if the condition fails or
	 * if the condition is not true after the timeout has elapsed.
	 *
	 * @param condition A condition that will be checked asynchronously during the timeout.
	 */
	public void await(Supplier<Boolean> condition) {

		long endTime = evaluateEndTime();
		while (System.currentTimeMillis() < endTime) {
			try {
				if (condition.get()) return;
				Thread.sleep(getPollingIntervalInMillis());
			}
			catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Condition supplier threw an exception");
			}
		}
		Assert.fail("Time limit exception.");
	}

	/**
	 * Waits until the function stops throwing errors or a timeout exceeded,
	 * used for waiting a pass of assertions.
	 *
	 * @param runnableWithThrows function that can throw a Throwable
	 */
	public void await(RunnableWithThrows runnableWithThrows) {

		long endTime = evaluateEndTime();
		while (System.currentTimeMillis() < endTime) {
			try {
				Thread.sleep(getPollingIntervalInMillis());
				runnableWithThrows.run();
				return;
			}
			catch (Throwable t) {
				//ignore
			}
		}
		Assert.fail("Time limit exception.");
	}

	private long evaluateEndTime() {
		if (timeout == null) {
			Assert.fail("Not found timeout settings for the AwaitAssert");
		}
		long startTime = System.currentTimeMillis();
		return startTime + getTimeoutInMillis();
	}

	private long getPollingIntervalInMillis() {
		return this.pollingInterval.toMillis();
	}

	private long getTimeoutInMillis() {
		return this.timeout.toMillis();
	}
}
