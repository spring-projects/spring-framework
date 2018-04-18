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

package org.springframework.scheduling.concurrent;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.ErrorHandler;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
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
	public void executeFailingRunnableWithErrorHandler() {
		TestTask task = new TestTask(0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		scheduler.execute(task);
		await(errorHandler);
		assertNotNull(errorHandler.lastError);
	}

	@Test
	public void submitFailingRunnableWithErrorHandler() throws Exception {
		TestTask task = new TestTask(0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<?> future = scheduler.submit(task);
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertTrue(future.isDone());
		assertNull(result);
		assertNotNull(errorHandler.lastError);
	}

	@Test
	public void submitFailingCallableWithErrorHandler() throws Exception {
		TestCallable task = new TestCallable(0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<String> future = scheduler.submit(task);
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertTrue(future.isDone());
		assertNull(result);
		assertNotNull(errorHandler.lastError);
	}

	@Test
	public void scheduleOneTimeTask() throws Exception {
		TestTask task = new TestTask(1);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertNull(result);
		assertTrue(future.isDone());
		assertThreadNamePrefix(task);
	}

	@Test(expected = ExecutionException.class)
	public void scheduleOneTimeFailingTaskWithoutErrorHandler() throws Exception {
		TestTask task = new TestTask(0);
		Future<?> future = scheduler.schedule(task, new Date());
		try {
			future.get(1000, TimeUnit.MILLISECONDS);
		}
		catch (ExecutionException ex) {
			assertTrue(future.isDone());
			throw ex;
		}
	}

	@Test
	public void scheduleOneTimeFailingTaskWithErrorHandler() throws Exception {
		TestTask task = new TestTask(0);
		TestErrorHandler errorHandler = new TestErrorHandler(1);
		scheduler.setErrorHandler(errorHandler);
		Future<?> future = scheduler.schedule(task, new Date());
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertTrue(future.isDone());
		assertNull(result);
		assertNotNull(errorHandler.lastError);
	}

	@Test
	public void scheduleTriggerTask() throws Exception {
		TestTask task = new TestTask(3);
		Future<?> future = scheduler.schedule(task, new TestTrigger(3));
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertNull(result);
		await(task);
		assertThreadNamePrefix(task);
	}

	@Test
	public void scheduleMultipleTriggerTasks() throws Exception {
		for (int i = 0; i < 1000; i++) {
			scheduleTriggerTask();
		}
	}


	private void assertThreadNamePrefix(TestTask task) {
		assertEquals(THREAD_NAME_PREFIX, task.lastThread.getName().substring(0, THREAD_NAME_PREFIX.length()));
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
		assertEquals("latch did not count down,", 0, latch.getCount());
	}


	private static class TestTask implements Runnable {

		private final int expectedRunCount;

		private final AtomicInteger actualRunCount = new AtomicInteger();

		private final CountDownLatch latch;

		private Thread lastThread;

		TestTask(int expectedRunCount) {
			this.expectedRunCount = expectedRunCount;
			this.latch = new CountDownLatch(expectedRunCount);
		}

		@Override
		public void run() {
			lastThread = Thread.currentThread();
			if (actualRunCount.incrementAndGet() > expectedRunCount) {
				throw new RuntimeException("intentional test failure");
			}
			latch.countDown();
		}
	}


	private static class TestCallable implements Callable<String> {

		private final int expectedRunCount;

		private final AtomicInteger actualRunCount = new AtomicInteger();

		TestCallable(int expectedRunCount) {
			this.expectedRunCount = expectedRunCount;
		}

		@Override
		public String call() throws Exception {
			if (actualRunCount.incrementAndGet() > expectedRunCount) {
				throw new RuntimeException("intentional test failure");
			}
			return Thread.currentThread().getName();
		}
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
