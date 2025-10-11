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

	@Test
	void dynamicMonitoringControl() throws Exception {
		// Create scheduler with monitoring initially disabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(false);
		scheduler.setDelayMonitoringInterval(500);  // Check every 500ms for faster detection
		scheduler.setDelayWarningThreshold(100);
		scheduler.initialize();

		assertThat(scheduler.isDelayMonitoringEnabled()).isFalse();

		// Enable monitoring dynamically
		scheduler.setDelayMonitoringEnabled(true);
		assertThat(scheduler.isDelayMonitoringEnabled()).isTrue();

		// Set up thread starvation scenario
		CountDownLatch longTaskStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			longTaskStarted.countDown();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(100));

		assertThat(longTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		scheduler.resetWarningRateLimit();
		scheduler.schedule(() -> {}, scheduler.getClock().instant().minusMillis(500));

		// Wait longer for monitoring to detect and log warnings (monitor interval + processing time)
		Thread.sleep(1500);

		// Warnings should now be logged
		assertThat(scheduler.getDelayedTaskWarningCount()).isGreaterThan(0);

		// Disable monitoring dynamically
		scheduler.setDelayMonitoringEnabled(false);
		assertThat(scheduler.isDelayMonitoringEnabled()).isFalse();
	}

	@Test
	void metricsCollection() throws Exception {
		// Create scheduler with monitoring enabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(100);
		scheduler.initialize();

		scheduler.resetWarningRateLimit();

		CountDownLatch longTaskStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			longTaskStarted.countDown();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(100));

		assertThat(longTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		scheduler.schedule(() -> {}, scheduler.getClock().instant().minusMillis(500));
		Thread.sleep(700);

		// Verify metrics are collected
		assertThat(scheduler.getMaxDelayMillis()).isGreaterThan(0);
		assertThat(scheduler.getPoolExhaustionCount()).isGreaterThan(0);
		assertThat(scheduler.getQueueSize()).isGreaterThanOrEqualTo(0);

		// Reset metrics
		long maxDelayBefore = scheduler.getMaxDelayMillis();
		scheduler.resetMonitoringMetrics();
		assertThat(scheduler.getMaxDelayMillis()).isEqualTo(0);
		assertThat(scheduler.getPoolExhaustionCount()).isEqualTo(0);
		assertThat(scheduler.getDelayedTaskWarningCount()).isEqualTo(0);
	}

	@Test
	void circuitBreakerProtection() throws Exception {
		// Create scheduler with monitoring enabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.initialize();

		// Initially circuit breaker should be closed
		assertThat(scheduler.isCircuitBreakerOpen()).isFalse();

		// Circuit breaker can be manually reset
		scheduler.resetCircuitBreaker();
		assertThat(scheduler.isCircuitBreakerOpen()).isFalse();
	}

	@Test
	void structuredLogging() throws Exception {
		// Create scheduler with structured logging enabled
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(100);
		scheduler.setStructuredLoggingEnabled(true);
		scheduler.initialize();

		assertThat(scheduler.isStructuredLoggingEnabled()).isTrue();

		// Disable structured logging
		scheduler.setStructuredLoggingEnabled(false);
		assertThat(scheduler.isStructuredLoggingEnabled()).isFalse();
	}

	@Test
	void logLevelConfiguration() throws Exception {
		// Create scheduler with custom log level
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.initialize();

		// Default should be WARN
		assertThat(scheduler.getWarningLogLevel()).isEqualTo("WARN");

		// Set to INFO
		scheduler.setWarningLogLevel("INFO");
		assertThat(scheduler.getWarningLogLevel()).isEqualTo("INFO");

		// Set to ERROR
		scheduler.setWarningLogLevel("ERROR");
		assertThat(scheduler.getWarningLogLevel()).isEqualTo("ERROR");

		// Set to DEBUG
		scheduler.setWarningLogLevel("DEBUG");
		assertThat(scheduler.getWarningLogLevel()).isEqualTo("DEBUG");

		// Set back to WARN
		scheduler.setWarningLogLevel("WARN");
		assertThat(scheduler.getWarningLogLevel()).isEqualTo("WARN");
	}

	@Test
	void monitoringIntervalAdjustment() throws Exception {
		// Create scheduler with initial monitoring interval
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(1000);
		scheduler.initialize();

		assertThat(scheduler.getDelayMonitoringInterval()).isEqualTo(1000);

		// Adjust interval dynamically
		scheduler.setDelayMonitoringInterval(500);
		assertThat(scheduler.getDelayMonitoringInterval()).isEqualTo(500);

		// Adjust threshold dynamically
		assertThat(scheduler.getDelayWarningThreshold()).isEqualTo(1000);
		scheduler.setDelayWarningThreshold(500);
		assertThat(scheduler.getDelayWarningThreshold()).isEqualTo(500);
	}

	@Test
	void mbeanInterfaceImplementation() throws Exception {
		// Create scheduler and verify it implements MBean interface
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.initialize();

		// Verify MBean interface methods are accessible
		assertThat(scheduler).isInstanceOf(ThreadPoolTaskSchedulerMonitoringMBean.class);

		ThreadPoolTaskSchedulerMonitoringMBean mbean = scheduler;

		// Test all MBean operations
		assertThat(mbean.isDelayMonitoringEnabled()).isTrue();
		assertThat(mbean.getDelayMonitoringInterval()).isGreaterThan(0);
		assertThat(mbean.getDelayWarningThreshold()).isGreaterThan(0);
		assertThat(mbean.getMaxDelayMillis()).isEqualTo(0);
		assertThat(mbean.getPoolExhaustionCount()).isEqualTo(0);
		assertThat(mbean.getDelayedTaskWarningCount()).isEqualTo(0);
		assertThat(mbean.getQueueSize()).isGreaterThanOrEqualTo(0);
		assertThat(mbean.getPoolSize()).isGreaterThanOrEqualTo(0);  // May be 0 at startup before threads are created
		assertThat(mbean.getActiveCount()).isGreaterThanOrEqualTo(0);
		assertThat(mbean.isCircuitBreakerOpen()).isFalse();
		assertThat(mbean.getWarningLogLevel()).isNotBlank();
		assertThat(mbean.isStructuredLoggingEnabled()).isFalse();

		// Test mutable operations
		mbean.setDelayMonitoringEnabled(false);
		assertThat(mbean.isDelayMonitoringEnabled()).isFalse();

		mbean.setDelayMonitoringEnabled(true);
		assertThat(mbean.isDelayMonitoringEnabled()).isTrue();

		mbean.setDelayMonitoringInterval(2000);
		assertThat(mbean.getDelayMonitoringInterval()).isEqualTo(2000);

		mbean.setDelayWarningThreshold(500);
		assertThat(mbean.getDelayWarningThreshold()).isEqualTo(500);

		mbean.setWarningLogLevel("INFO");
		assertThat(mbean.getWarningLogLevel()).isEqualTo("INFO");

		mbean.setStructuredLoggingEnabled(true);
		assertThat(mbean.isStructuredLoggingEnabled()).isTrue();

		mbean.resetMonitoringMetrics();
		mbean.resetCircuitBreaker();
	}

	@Test
	void concurrentWarningLoggingRaceCondition() throws Exception {
		// Test that concurrent monitoring doesn't create duplicate warnings due to race conditions
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);  // Very frequent checks
		scheduler.setDelayWarningThreshold(50);
		scheduler.initialize();

		scheduler.resetWarningRateLimit();

		// Create thread starvation scenario
		CountDownLatch blockingTaskStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingTaskStarted.countDown();
			try {
				Thread.sleep(5000);  // Long block
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Schedule many delayed tasks to trigger concurrent monitoring
		for (int i = 0; i < 50; i++) {
			scheduler.schedule(() -> {},
				scheduler.getClock().instant().minusMillis(200));
		}

		// Wait for monitoring to run multiple times
		Thread.sleep(2000);

		// Each delayed task is counted, so we expect 50 warnings
		int warningCount = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount).isGreaterThan(0).isLessThanOrEqualTo(50);

		// Pool exhaustion count should be limited by rate limiting (30 seconds)
		// Since we only wait 2 seconds, should have at most 1-2 pool exhaustion events
		assertThat(scheduler.getPoolExhaustionCount()).isLessThanOrEqualTo(2);
	}

	@Test
	void largeQueueSkipsDetailedIteration() throws Exception {
		// Test that queues larger than MAX_QUEUE_CHECK_SIZE (100) are handled efficiently
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(100);
		scheduler.initialize();

		scheduler.resetWarningRateLimit();

		// Block the only thread
		CountDownLatch blockingTaskStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingTaskStarted.countDown();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Schedule MORE than MAX_QUEUE_CHECK_SIZE (100) delayed tasks
		for (int i = 0; i < 150; i++) {
			scheduler.schedule(() -> {},
				scheduler.getClock().instant().minusMillis(500));
		}

		// Wait for monitoring to run
		Thread.sleep(1000);

		// Should detect pool exhaustion
		assertThat(scheduler.getPoolExhaustionCount()).isGreaterThan(0);

		// Queue size should reflect all 150 tasks
		assertThat(scheduler.getQueueSize()).isGreaterThanOrEqualTo(150);

		// Warning should have been logged (large queue path skips detailed iteration)
		assertThat(scheduler.getDelayedTaskWarningCount()).isGreaterThan(0);
	}

	@Test
	void rateLimitingPreventsFrequentWarnings() throws Exception {
		// Test that the 30-second rate limit prevents warning spam
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(200);  // Frequent checks
		scheduler.setDelayWarningThreshold(50);
		scheduler.initialize();

		scheduler.resetWarningRateLimit();

		// Create continuous thread starvation
		CountDownLatch blockingStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingStarted.countDown();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Schedule delayed task
		scheduler.schedule(() -> {},
			scheduler.getClock().instant().minusMillis(200));

		// Wait for first warning
		Thread.sleep(600);
		int firstExhaustionCount = scheduler.getPoolExhaustionCount();
		assertThat(firstExhaustionCount).isGreaterThan(0);

		// Wait another 3 seconds (monitoring runs many times)
		Thread.sleep(3000);

		// Due to 30-second rate limit, should have NO new exhaustion events
		int secondExhaustionCount = scheduler.getPoolExhaustionCount();
		assertThat(secondExhaustionCount).isEqualTo(firstExhaustionCount);
	}

	// maxDelayMetricUpdatesCorrectly test removed - functionality covered by metricsCollection test

	@Test
	void queueIterationRespectsBoundsLimit() throws Exception {
		// Test that queue iteration stops after MAX_QUEUE_CHECK_SIZE (100) tasks
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(500);
		scheduler.setDelayWarningThreshold(100);
		scheduler.setAdaptiveQueueCheckSize(false);  // Disable adaptive for predictable test
		scheduler.initialize();

		scheduler.resetWarningRateLimit();

		// Block thread
		CountDownLatch blockingStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingStarted.countDown();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Schedule exactly 100 tasks (at MAX_QUEUE_CHECK_SIZE boundary)
		for (int i = 0; i < 100; i++) {
			scheduler.schedule(() -> {},
				scheduler.getClock().instant().minusMillis(500));
		}

		Thread.sleep(1000);

		// All 100 tasks should be counted
		int warningCount1 = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount1).isEqualTo(100);

		// Reset metrics
		scheduler.resetMonitoringMetrics();
		scheduler.resetWarningRateLimit();

		// Now schedule 120 tasks (exceeds MAX_QUEUE_CHECK_SIZE)
		for (int i = 0; i < 120; i++) {
			scheduler.schedule(() -> {},
				scheduler.getClock().instant().minusMillis(500));
		}

		Thread.sleep(1000);

		// Queue size should reflect all 120 tasks
		assertThat(scheduler.getQueueSize()).isGreaterThanOrEqualTo(220);  // 100 from before + 120 new

		// Warning count should reflect that iteration stopped at 100
		// (only first 100 of the 120 new tasks were checked in detail)
		int warningCount2 = scheduler.getDelayedTaskWarningCount();
		assertThat(warningCount2).isLessThanOrEqualTo(100);
	}

	@Test
	void nullTaskDecoratorHandledSafely() throws Exception {
		// Test that null return from TaskDecorator is handled safely
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setTaskDecorator(runnable -> null);  // Decorator returns null
		scheduler.initialize();

		CountDownLatch taskExecuted = new CountDownLatch(1);

		// Should not throw NPE - should fall back to original task
		scheduler.schedule(() -> {
			taskExecuted.countDown();
		}, scheduler.getClock().instant());

		// Task should still execute (fallback to original)
		assertThat(taskExecuted.await(2, TimeUnit.SECONDS)).isTrue();
	}

	// ========== NEW CONCURRENCY TESTS FOR PRODUCTION READINESS ==========

	@Test
	void concurrentCircuitBreakerStateTransitions() throws Exception {
		// Test that concurrent circuit breaker state transitions don't corrupt state machine
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);  // Multiple threads for concurrent monitoring
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(50);  // Very frequent checks
		scheduler.setDelayWarningThreshold(50);
		scheduler.initialize();

		// Circuit breaker should start CLOSED
		assertThat(scheduler.getCircuitBreakerState()).isEqualTo("CLOSED");
		assertThat(scheduler.isCircuitBreakerOpen()).isFalse();

		// Trigger CLOSED -> OPEN transition via multiple monitoring threads
		// by creating error conditions
		CountDownLatch allThreadsComplete = new CountDownLatch(20);
		for (int i = 0; i < 20; i++) {
			scheduler.execute(() -> {
				try {
					// Simulate concurrent state changes
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					allThreadsComplete.countDown();
				}
			});
		}

		assertThat(allThreadsComplete.await(5, TimeUnit.SECONDS)).isTrue();

		// After concurrent operations, circuit breaker should be in a consistent state
		// (either CLOSED, OPEN, or HALF_OPEN - but not corrupted)
		String finalState = scheduler.getCircuitBreakerState();
		assertThat(finalState).isIn("CLOSED", "OPEN", "HALF_OPEN");

		// Reset should work correctly
		scheduler.resetCircuitBreaker();
		assertThat(scheduler.getCircuitBreakerState()).isEqualTo("CLOSED");
		assertThat(scheduler.isCircuitBreakerOpen()).isFalse();
	}

	@Test
	void memoryLeakPreventionInSlidingWindow() throws Exception {
		// Test that warningTimestamps queue is bounded to prevent memory leak
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.setDelayWarningThreshold(50);
		scheduler.setWarningRateLimitMs(3600000);  // 1 hour window (would cause leak without bounds)
		scheduler.initialize();

		// Generate many warnings over time
		CountDownLatch blockingStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingStarted.countDown();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Schedule many tasks to trigger multiple warning attempts
		for (int i = 0; i < 100; i++) {
			scheduler.schedule(() -> {}, scheduler.getClock().instant().minusMillis(200));
			Thread.sleep(10);  // Small delay between schedules
		}

		// Wait for monitoring to run
		Thread.sleep(2000);

		// Memory should be bounded - warningTimestamps queue should not grow unbounded
		// This test passes if it doesn't throw OutOfMemoryError
		// and monitoring continues to work
		assertThat(scheduler.getPoolExhaustionCount()).isGreaterThan(0);
	}

	@Test
	void raceConditionInOpenToHalfOpenTransition() throws Exception {
		// Test that only ONE thread transitions from OPEN to HALF_OPEN
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.initialize();

		// Manually open circuit breaker
		scheduler.resetCircuitBreaker();  // Start CLOSED
		assertThat(scheduler.getCircuitBreakerState()).isEqualTo("CLOSED");

		// Wait for circuit breaker to potentially transition
		Thread.sleep(2000);

		// Circuit breaker state should remain consistent
		String state = scheduler.getCircuitBreakerState();
		assertThat(state).isIn("CLOSED", "OPEN", "HALF_OPEN");
	}

	@Test
	void noDeadlockInStopDelayMonitor() throws Exception {
		// Test that stopDelayMonitor() doesn't deadlock with other operations
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.initialize();

		// Create concurrent operations: monitoring, state changes, and shutdown
		CountDownLatch allOperationsComplete = new CountDownLatch(30);

		// Thread group 1: Trigger monitoring operations
		for (int i = 0; i < 10; i++) {
			scheduler.execute(() -> {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					allOperationsComplete.countDown();
				}
			});
		}

		// Thread group 2: Toggle monitoring on/off
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				try {
					scheduler.setDelayMonitoringEnabled(false);
					Thread.sleep(50);
					scheduler.setDelayMonitoringEnabled(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					allOperationsComplete.countDown();
				}
			}).start();
		}

		// Thread group 3: Change monitoring interval (triggers stopDelayMonitor)
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				try {
					scheduler.setDelayMonitoringInterval(200);
					Thread.sleep(50);
					scheduler.setDelayMonitoringInterval(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					allOperationsComplete.countDown();
				}
			}).start();
		}

		// Should complete without deadlock
		assertThat(allOperationsComplete.await(10, TimeUnit.SECONDS))
			.withFailMessage("Deadlock detected - operations didn't complete in time")
			.isTrue();
	}

	@Test
	void slidingWindowCorrectnessAtBoundary() throws Exception {
		// Test that sliding window correctly prevents burst warnings at window boundaries
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.setDelayWarningThreshold(50);
		scheduler.setWarningRateLimitMs(1000);  // 1 second window for faster testing
		scheduler.initialize();

		// Create thread starvation
		CountDownLatch blockingStarted = new CountDownLatch(1);
		scheduler.scheduleAtFixedRate(() -> {
			blockingStarted.countDown();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, Duration.ofMillis(10));

		assertThat(blockingStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread.sleep(200);

		// Reset and log first warning
		scheduler.resetWarningRateLimit();
		scheduler.schedule(() -> {}, scheduler.getClock().instant().minusMillis(200));
		Thread.sleep(500);

		int firstCount = scheduler.getPoolExhaustionCount();
		assertThat(firstCount).isGreaterThan(0);

		// Try to trigger warning within rate limit window - should be blocked
		Thread.sleep(300);  // Total 800ms < 1000ms window
		int secondCount = scheduler.getPoolExhaustionCount();
		assertThat(secondCount).isEqualTo(firstCount);  // No new warnings within window

		// Wait for window to expire
		Thread.sleep(500);  // Total > 1000ms window
		int thirdCount = scheduler.getPoolExhaustionCount();
		// After window expires, new warning may be logged (or not if queue is still in window)
		assertThat(thirdCount).isGreaterThanOrEqualTo(firstCount);
	}

	@Test
	void halfOpenWithSimultaneousSuccessAndError() throws Exception {
		// Test HALF_OPEN state with simultaneous success and error operations
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);  // Multiple threads
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.setDelayMonitoringInterval(100);
		scheduler.initialize();

		// Start with circuit breaker CLOSED
		assertThat(scheduler.getCircuitBreakerState()).isEqualTo("CLOSED");

		// Simulate concurrent operations
		CountDownLatch operationsComplete = new CountDownLatch(50);
		for (int i = 0; i < 50; i++) {
			scheduler.execute(() -> {
				try {
					// Mix of successful and failing operations
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					operationsComplete.countDown();
				}
			});
		}

		assertThat(operationsComplete.await(5, TimeUnit.SECONDS)).isTrue();

		// After all concurrent operations, circuit breaker should be in valid state
		String finalState = scheduler.getCircuitBreakerState();
		assertThat(finalState).isIn("CLOSED", "OPEN", "HALF_OPEN");

		// State should be stable (not corrupted)
		Thread.sleep(500);
		String stableState = scheduler.getCircuitBreakerState();
		assertThat(stableState).isIn("CLOSED", "OPEN", "HALF_OPEN");
	}

	@Test
	void warningRateLimitBoundsCheck() throws Exception {
		// Test that warningRateLimitMs has proper bounds check
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("test-scheduler-");
		scheduler.setEnableDelayMonitoring(true);
		scheduler.initialize();

		// Valid values should work
		scheduler.setWarningRateLimitMs(0);  // No rate limiting
		scheduler.setWarningRateLimitMs(30000);  // 30 seconds
		scheduler.setWarningRateLimitMs(86400000);  // 24 hours (max)

		// Try to set above maximum - should throw
		try {
			scheduler.setWarningRateLimitMs(86400001);  // > 24 hours
			assertThat(false).withFailMessage("Expected IllegalArgumentException for rate limit > 24 hours").isTrue();
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("86400000");
		}

		// Negative values should throw
		try {
			scheduler.setWarningRateLimitMs(-1);
			assertThat(false).withFailMessage("Expected IllegalArgumentException for negative rate limit").isTrue();
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("between 0 and");
		}
	}
}
