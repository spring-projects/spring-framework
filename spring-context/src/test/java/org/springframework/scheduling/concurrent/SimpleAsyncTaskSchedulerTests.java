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

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 6.2
 */
class SimpleAsyncTaskSchedulerTests extends AbstractSchedulingTaskExecutorTests {

	private final SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

	private final AtomicBoolean taskRun = new AtomicBoolean();


	@Override
	protected AsyncTaskExecutor buildExecutor() {
		scheduler.setTaskDecorator(runnable -> () -> {
			taskRun.set(true);
			runnable.run();
		});
		scheduler.setThreadNamePrefix(this.threadNamePrefix);
		return scheduler;
	}


	@Test
	@Override
	void submitRunnableWithGetAfterShutdown() {
		// decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
	}

	@Test
	@Override
	void submitCompletableRunnableWithGetAfterShutdown() {
		// decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
	}

	@Test
	@Override
	void submitCallableWithGetAfterShutdown() {
		// decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
	}

	@Test
	@Override
	void submitCompletableCallableWithGetAfterShutdown() {
		// decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
	}


	@Test
	void executeFailingRunnableWithErrorHandler() {
		TestTask task = new TestTask(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		scheduler.execute(task);
		await(errorHandler);
		assertThat(errorHandler.lastError).isNotNull();
		assertThat(taskRun.get()).isTrue();
	}

	@Test
	void submitFailingRunnableWithErrorHandler() throws Exception {
		TestTask task = new TestTask(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<?> future = scheduler.submit(task);
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(future.isDone()).isTrue();
		assertThat(result).isNull();
		assertThat(errorHandler.lastError).isNotNull();
		assertThat(taskRun.get()).isTrue();
	}

	@Test
	void submitFailingCallableWithErrorHandler() throws Exception {
		TestCallable task = new TestCallable(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<String> future = scheduler.submit(task);
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(future.isDone()).isTrue();
		assertThat(result).isNull();
		assertThat(errorHandler.lastError).isNotNull();
		assertThat(taskRun.get()).isTrue();
	}

	@Test
	@SuppressWarnings("deprecation")
	void scheduleOneTimeTask() throws Exception {
		TestTask task = new TestTask(this.testName, 1);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(result).isNull();
		await(task);
		assertThat(taskRun.get()).isTrue();
		assertThreadNamePrefix(task);
	}

	@Test
	@SuppressWarnings("deprecation")
	void scheduleOneTimeFailingTaskWithErrorHandler() throws Exception {
		TestTask task = new TestTask(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		await(errorHandler);
		assertThat(result).isNull();
		assertThat(errorHandler.lastError).isNotNull();
		assertThat(taskRun.get()).isTrue();
	}

	@RepeatedTest(20)
	void scheduleMultipleTriggerTasks() throws Exception {
		TestTask task = new TestTask(this.testName, 3);
		Future<?> future = scheduler.schedule(task, new TestTrigger(3));
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(result).isNull();
		await(task);
		assertThat(taskRun.get()).isTrue();
		assertThreadNamePrefix(task);
	}


	private void await(TestTask task) {
		await(task.latch);
	}

	private void await(TestErrorHandler errorHandler) {
		await(errorHandler.latch);
	}

	private void await(CountDownLatch latch) {
		try {
			latch.await(1000, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException(ex);
		}
		assertThat(latch.getCount()).as("latch did not count down").isEqualTo(0);
	}


	private static class TestErrorHandler implements ErrorHandler {

		private final CountDownLatch latch;

		private volatile Throwable lastError;

		TestErrorHandler(int expectedErrorCount) {
			this.latch = new CountDownLatch(expectedErrorCount);
		}

		@Override
		public void handleError(Throwable t) {
			this.lastError = t;
			this.latch.countDown();
		}
	}


	private static class TestTrigger implements Trigger {

		private final int maxRunCount;

		private final AtomicInteger actualRunCount = new AtomicInteger();

		TestTrigger(int maxRunCount) {
			this.maxRunCount = maxRunCount;
		}

		@Override
		public Instant nextExecution(TriggerContext triggerContext) {
			if (this.actualRunCount.incrementAndGet() > this.maxRunCount) {
				return null;
			}
			return Instant.now();
		}
	}

}
