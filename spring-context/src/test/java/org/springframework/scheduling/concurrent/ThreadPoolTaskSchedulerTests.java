/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class ThreadPoolTaskSchedulerTests extends AbstractSchedulingTaskExecutorTests {

	private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();


	@Override
	protected AsyncListenableTaskExecutor buildExecutor() {
		scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
		scheduler.afterPropertiesSet();
		return scheduler;
	}


	@Test
	void executeFailingRunnableWithErrorHandler() {
		TestTask task = new TestTask(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		scheduler.execute(task);
		await(errorHandler);
		assertThat(errorHandler.lastError).isNotNull();
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
	}

	@Test
	void scheduleOneTimeTask() throws Exception {
		TestTask task = new TestTask(this.testName, 1);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(result).isNull();
		assertThat(future.isDone()).isTrue();
		assertThreadNamePrefix(task);
	}

	@Test
	void scheduleOneTimeFailingTaskWithoutErrorHandler() throws Exception {
		TestTask task = new TestTask(this.testName, 0);
		Future<?> future = scheduler.schedule(task, new Date());
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> future.get(1000, TimeUnit.MILLISECONDS));
		assertThat(future.isDone()).isTrue();
	}

	@Test
	void scheduleOneTimeFailingTaskWithErrorHandler() throws Exception {
		TestTask task = new TestTask(this.testName, 0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(future.isDone()).isTrue();
		assertThat(result).isNull();
		assertThat(errorHandler.lastError).isNotNull();
	}

	@Test
	void scheduleTriggerTask() throws Exception {
		TestTask task = new TestTask(this.testName, 3);
		Future<?> future = scheduler.schedule(task, new TestTrigger(3));
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertThat(result).isNull();
		await(task);
		assertThreadNamePrefix(task);
	}

	@Test
	void scheduleMultipleTriggerTasks() throws Exception {
		for (int i = 0; i < 100; i++) {
			scheduleTriggerTask();
		}
	}


	private void assertThreadNamePrefix(TestTask task) {
		assertThat(task.lastThread.getName().substring(0, THREAD_NAME_PREFIX.length())).isEqualTo(THREAD_NAME_PREFIX);
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
		assertThat(latch.getCount()).as("latch did not count down,").isEqualTo(0);
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
		public Date nextExecutionTime(TriggerContext triggerContext) {
			if (this.actualRunCount.incrementAndGet() > this.maxRunCount) {
				return null;
			}
			return new Date();
		}
	}

}
