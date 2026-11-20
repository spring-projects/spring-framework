/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;


import java.time.Duration;
import java.time.Instant;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledTask}.
 * @author Brian Clozel
 */
class ScheduledTaskTests {

	private CountingRunnable countingRunnable = new CountingRunnable();

	private SimpleAsyncTaskScheduler taskScheduler = new SimpleAsyncTaskScheduler();

	private ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

	@BeforeEach
	void setup() {
		this.taskRegistrar.setTaskScheduler(this.taskScheduler);
		taskScheduler.start();
	}

	@AfterEach
	void tearDown() {
		taskScheduler.stop();
	}

	@Test
	void shouldReturnConfiguredTask() {
		Task task = new Task(countingRunnable);
		ScheduledTask scheduledTask = new ScheduledTask(task);
		assertThat(scheduledTask.getTask()).isEqualTo(task);
	}

	@Test
	void shouldUseTaskToString() {
		Task task = new Task(countingRunnable);
		ScheduledTask scheduledTask = new ScheduledTask(task);
		assertThat(scheduledTask.toString()).isEqualTo(task.toString());
	}

	@Test
	void unscheduledTaskShouldNotHaveNextExecution() {
		ScheduledTask scheduledTask = new ScheduledTask(new Task(countingRunnable));
		assertThat(scheduledTask.nextExecution()).isNull();
		assertThat(countingRunnable.executionCount).isZero();
	}

	@Test
	void scheduledTaskShouldHaveNextExecution() {
		ScheduledTask scheduledTask = taskRegistrar.scheduleFixedDelayTask(new FixedDelayTask(countingRunnable,
				Duration.ofSeconds(10), Duration.ofSeconds(10)));
		assertThat(scheduledTask.nextExecution()).isBefore(Instant.now().plusSeconds(11));
	}

	@Test
	void cancelledTaskShouldNotHaveNextExecution() {
		ScheduledTask scheduledTask = taskRegistrar.scheduleFixedDelayTask(new FixedDelayTask(countingRunnable,
				Duration.ofSeconds(10), Duration.ofSeconds(10)));
		scheduledTask.cancel(true);
		assertThat(scheduledTask.nextExecution()).isNull();
	}

	@Test
	void singleExecutionShouldNotHaveNextExecution() {
		ScheduledTask scheduledTask = taskRegistrar.scheduleOneTimeTask(new OneTimeTask(countingRunnable, Duration.ZERO));
		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> countingRunnable.executionCount > 0);
		assertThat(scheduledTask.nextExecution()).isNull();
	}

	static class CountingRunnable implements Runnable {

		int executionCount;

		@Override
		public void run() {
			executionCount++;
		}
	}

}
