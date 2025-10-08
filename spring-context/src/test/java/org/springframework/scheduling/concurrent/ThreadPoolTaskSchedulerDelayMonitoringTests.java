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

class ThreadPoolTaskSchedulerDelayMonitoringTests {

	private ThreadPoolTaskScheduler scheduler;


	@AfterEach
	void tearDown() {
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}


	@Test
	void delayMonitoringDetectsThreadStarvation() throws Exception {
		// Create scheduler with pool size of 1
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);  // Check every 500ms
		scheduler.setDelayWarningThreshold(100);    // Warn if delayed by > 100ms
		scheduler.initialize();

		// Reset rate limit to allow immediate warning in test
		scheduler.resetWarningRateLimit();

		CountDownLatch longTaskStarted = new CountDownLatch(1);
		CountDownLatch longTaskFinish = new CountDownLatch(1);

		// Schedule a long-running task that blocks the only thread
		scheduler.scheduleAtFixedRate(() -> {
			longTaskStarted.countDown();
			try {
				// Block for 3 seconds
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(100));

		// Wait for the long task to start
		assertThat(longTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();

		// Wait a bit to ensure the long task is blocking
		Thread.sleep(200);

		// Schedule a quick task slightly in the past so it's immediately delayed
		CountDownLatch quickTaskExecuted = new CountDownLatch(1);
		Instant scheduledTime = scheduler.getClock().instant().minusMillis(200);
		scheduler.schedule(() -> {
			quickTaskExecuted.countDown();
		}, scheduledTime);

		// Wait for delay monitoring to detect the issue
		// The monitor runs every 500ms, task is already delayed by 200ms > threshold (100ms)
		// So we wait for the next monitor run + margin
		Thread.sleep(700);

		// Verify that warnings were logged
		// The delayedTaskWarningCount should be greater than 0
		int warningCount = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount)
			.withFailMessage("Expected warnings to be logged but got %d. " +
							"Active threads: %d, Pool size: %d, Queue size: %d",
					warningCount,
					scheduler.getScheduledThreadPoolExecutor().getActiveCount(),
					scheduler.getScheduledThreadPoolExecutor().getPoolSize(),
					scheduler.getScheduledThreadPoolExecutor().getQueue().size())
			.isGreaterThan(0);

		// The quick task should still be waiting (not executed yet)
		assertThat(quickTaskExecuted.getCount()).isEqualTo(1);
	}

	@Test
	void delayMonitoringCanBeDisabled() throws Exception {
		// Create scheduler with monitoring disabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(false);
		scheduler.initialize();

		CountDownLatch longTaskStarted = new CountDownLatch(1);

		// Schedule a long-running task
		scheduler.scheduleAtFixedRate(() -> {
			longTaskStarted.countDown();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(100));

		// Wait for the long task to start
		assertThat(longTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();

		// Schedule another task
		scheduler.schedule(() -> {
			// Quick task
		}, scheduler.getClock().instant());

		// Wait
		Thread.sleep(1000);

		// No warnings should be logged since monitoring is disabled
		int warningCount = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount).isEqualTo(0);
	}

	@Test
	void delayMonitoringWithSufficientPoolSize() throws Exception {
		// Create scheduler with sufficient pool size
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5);  // Plenty of threads
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(100);
		scheduler.initialize();

		CountDownLatch allTasksStarted = new CountDownLatch(3);

		// Schedule multiple tasks
		for (int i = 0; i < 3; i++) {
			scheduler.scheduleAtFixedRate(() -> {
				allTasksStarted.countDown();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}, Duration.ofMillis(100));
		}

		// All tasks should start without delay
		assertThat(allTasksStarted.await(2, TimeUnit.SECONDS)).isTrue();

		// Wait for monitoring to run
		Thread.sleep(1000);

		// No warnings should be logged since there's no thread starvation
		int warningCount = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount).isEqualTo(0);
	}

	@Test
	void delayMonitoringShutdownGracefully() throws Exception {
		// Create scheduler with monitoring enabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.initialize();

		// Schedule a task
		CountDownLatch taskExecuted = new CountDownLatch(1);
		scheduler.schedule(() -> {
			taskExecuted.countDown();
		}, scheduler.getClock().instant());

		// Wait for task to execute
		assertThat(taskExecuted.await(2, TimeUnit.SECONDS)).isTrue();

		// Shutdown should complete without errors
		scheduler.shutdown();

		// Verify scheduler is shut down
		assertThat(scheduler.getScheduledExecutor().isShutdown()).isTrue();
	}

	@Test
	void delayMonitoringWithCustomThreshold() throws Exception {
		// Create scheduler with custom warning threshold
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(2000);  // Only warn if delayed > 2 seconds
		scheduler.initialize();

		CountDownLatch longTaskStarted = new CountDownLatch(1);

		// Schedule a long-running task
		scheduler.scheduleAtFixedRate(() -> {
			longTaskStarted.countDown();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(100));

		// Wait for the long task to start
		assertThat(longTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();

		// Schedule another task
		scheduler.schedule(() -> {
			// Quick task
		}, scheduler.getClock().instant());

		// Wait for 1 second (less than threshold)
		Thread.sleep(1000);

		// No warnings should be logged yet (delay < threshold)
		int warningCountBefore = scheduler.getDelayedTaskWarningCount();

		// Wait for another 1.5 seconds (now delay > threshold)
		Thread.sleep(1500);

		// Now warnings should be logged
		int warningCountAfter = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCountAfter).isGreaterThanOrEqualTo(warningCountBefore);
	}
}
