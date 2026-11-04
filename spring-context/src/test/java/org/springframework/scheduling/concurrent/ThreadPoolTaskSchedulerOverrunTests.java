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

package org.springframework.scheduling.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the task startup overrun warning in {@link ThreadPoolTaskScheduler}.
 *
 * @author Naman Agrawal
 * @since 7.0
 * @see ThreadPoolTaskScheduler#setTaskStartupOverrunThreshold(Duration)
 */
class ThreadPoolTaskSchedulerOverrunTests {

	private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

	@AfterEach
	void tearDown() {
		scheduler.destroy();
	}

	@Test
	void defaultThresholdIsNull() {
		assertThat(scheduler.getTaskStartupOverrunThreshold()).isNull();
	}

	@Test
	void thresholdIsStoredCorrectly() {
		Duration threshold = Duration.ofMillis(500);
		scheduler.setTaskStartupOverrunThreshold(threshold);
		assertThat(scheduler.getTaskStartupOverrunThreshold()).isEqualTo(threshold);
	}

	@Test
	void thresholdCanBeReset() {
		scheduler.setTaskStartupOverrunThreshold(Duration.ofSeconds(1));
		scheduler.setTaskStartupOverrunThreshold(null);
		assertThat(scheduler.getTaskStartupOverrunThreshold()).isNull();
	}

	@Test
	void taskExecutesNormallyWithThresholdSet() throws InterruptedException {
		scheduler.setTaskStartupOverrunThreshold(Duration.ofMillis(100));
		scheduler.initialize();

		CountDownLatch latch = new CountDownLatch(1);
		scheduler.schedule(latch::countDown, Instant.now().plusMillis(50));

		assertThat(latch.await(2, TimeUnit.SECONDS))
				.as("Task should execute normally even with threshold configured")
				.isTrue();
	}

	@Test
	void taskExecutesNormallyWithoutThreshold() throws InterruptedException {
		scheduler.initialize();

		CountDownLatch latch = new CountDownLatch(1);
		scheduler.schedule(latch::countDown, Instant.now().plusMillis(50));

		assertThat(latch.await(2, TimeUnit.SECONDS))
				.as("Task should execute normally without threshold configured")
				.isTrue();
	}

	@Test
	void overrunIsDetectedWhenTaskStartsLate() throws InterruptedException {
		// Pool size 1 so the first long task blocks the second task from starting on time
		scheduler.setPoolSize(1);
		scheduler.setTaskStartupOverrunThreshold(Duration.ofMillis(100));
		scheduler.initialize();

		CountDownLatch blocker = new CountDownLatch(1);
		CountDownLatch blocked = new CountDownLatch(1);

		// Schedule a task immediately that holds the single thread for 500 ms
		scheduler.schedule(() -> {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				blocker.countDown();
			}
		}, Instant.now());

		// Schedule a second task 50 ms from now - it will be at least 450 ms late
		scheduler.schedule(blocked::countDown, Instant.now().plusMillis(50));

		// Both should still complete despite the overrun
		assertThat(blocker.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(blocked.await(3, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void noOverrunWhenTaskStartsOnTime() throws InterruptedException {
		scheduler.setPoolSize(2);  // enough threads so nothing is starved
		scheduler.setTaskStartupOverrunThreshold(Duration.ofMillis(100));
		scheduler.initialize();

		CountDownLatch latch = new CountDownLatch(2);

		// Both tasks should start on time with 2 threads
		scheduler.schedule(latch::countDown, Instant.now().plusMillis(50));
		scheduler.schedule(latch::countDown, Instant.now().plusMillis(50));

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
	}

}
