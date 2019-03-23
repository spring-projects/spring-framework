/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 5.0.5
 */
public abstract class AbstractSchedulingTaskExecutorTests {

	static final String THREAD_NAME_PREFIX = "test-";

	private AsyncListenableTaskExecutor executor;

	private volatile Object outcome;


	@Before
	public void initExecutor() {
		executor = buildExecutor();
	}

	protected abstract AsyncListenableTaskExecutor buildExecutor();

	@After
	public void shutdownExecutor() throws Exception {
		if (executor instanceof DisposableBean) {
			((DisposableBean) executor).destroy();
		}
	}


	@Test
	public void executeRunnable() {
		TestTask task = new TestTask(1);
		executor.execute(task);
		await(task);
		assertThreadNamePrefix(task);
	}

	@Test
	public void executeFailingRunnable() {
		TestTask task = new TestTask(0);
		executor.execute(task);
		// nothing to assert
	}

	@Test
	public void submitRunnable() throws Exception {
		TestTask task = new TestTask(1);
		Future<?> future = executor.submit(task);
		Object result = future.get(1000, TimeUnit.MILLISECONDS);
		assertNull(result);
		assertThreadNamePrefix(task);
	}

	@Test(expected = ExecutionException.class)
	public void submitFailingRunnable() throws Exception {
		TestTask task = new TestTask(0);
		Future<?> future = executor.submit(task);
		try {
			future.get(1000, TimeUnit.MILLISECONDS);
		}
		catch (ExecutionException ex) {
			assertTrue(future.isDone());
			throw ex;
		}
	}

	@Test(expected = CancellationException.class)
	public void submitRunnableWithGetAfterShutdown() throws Exception {
		TestTask task1 = new TestTask(-1);
		Future<?> future1 = executor.submit(task1);
		TestTask task2 = new TestTask(-1);
		Future<?> future2 = executor.submit(task2);
		shutdownExecutor();
		future1.get();
		future2.get();
	}

	@Test
	public void submitListenableRunnable() throws Exception {
		TestTask task = new TestTask(1);
		ListenableFuture<?> future = executor.submitListenable(task);
		future.addCallback(result -> outcome = result, ex -> outcome = ex);
		Thread.sleep(1000);
		assertTrue(future.isDone());
		assertNull(outcome);
		assertThreadNamePrefix(task);
	}

	@Test
	public void submitFailingListenableRunnable() throws Exception {
		TestTask task = new TestTask(0);
		ListenableFuture<?> future = executor.submitListenable(task);
		future.addCallback(result -> outcome = result, ex -> outcome = ex);
		Thread.sleep(1000);
		assertTrue(future.isDone());
		assertSame(RuntimeException.class, outcome.getClass());
	}

	@Test(expected = CancellationException.class)
	public void submitListenableRunnableWithGetAfterShutdown() throws Exception {
		TestTask task1 = new TestTask(-1);
		ListenableFuture<?> future1 = executor.submitListenable(task1);
		TestTask task2 = new TestTask(-1);
		ListenableFuture<?> future2 = executor.submitListenable(task2);
		shutdownExecutor();
		future1.get();
		future2.get();
	}

	@Test
	public void submitCallable() throws Exception {
		TestCallable task = new TestCallable(1);
		Future<String> future = executor.submit(task);
		String result = future.get(1000, TimeUnit.MILLISECONDS);
		assertEquals(THREAD_NAME_PREFIX, result.substring(0, THREAD_NAME_PREFIX.length()));
	}

	@Test(expected = ExecutionException.class)
	public void submitFailingCallable() throws Exception {
		TestCallable task = new TestCallable(0);
		Future<String> future = executor.submit(task);
		future.get(1000, TimeUnit.MILLISECONDS);
		assertTrue(future.isDone());
	}

	@Test(expected = CancellationException.class)
	public void submitCallableWithGetAfterShutdown() throws Exception {
		TestCallable task1 = new TestCallable(-1);
		Future<?> future1 = executor.submit(task1);
		TestCallable task2 = new TestCallable(-1);
		Future<?> future2 = executor.submit(task2);
		shutdownExecutor();
		future1.get(100, TimeUnit.MILLISECONDS);
		future2.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void submitListenableCallable() throws Exception {
		TestCallable task = new TestCallable(1);
		ListenableFuture<String> future = executor.submitListenable(task);
		future.addCallback(result -> outcome = result, ex -> outcome = ex);
		Thread.sleep(100);
		assertTrue(future.isDone());
		assertEquals(THREAD_NAME_PREFIX, outcome.toString().substring(0, THREAD_NAME_PREFIX.length()));
	}

	@Test
	public void submitFailingListenableCallable() throws Exception {
		TestCallable task = new TestCallable(0);
		ListenableFuture<String> future = executor.submitListenable(task);
		future.addCallback(result -> outcome = result, ex -> outcome = ex);
		Thread.sleep(100);
		assertTrue(future.isDone());
		assertSame(RuntimeException.class, outcome.getClass());
	}

	@Test(expected = CancellationException.class)
	public void submitListenableCallableWithGetAfterShutdown() throws Exception {
		TestCallable task1 = new TestCallable(-1);
		ListenableFuture<?> future1 = executor.submitListenable(task1);
		TestCallable task2 = new TestCallable(-1);
		ListenableFuture<?> future2 = executor.submitListenable(task2);
		shutdownExecutor();
		future1.get(100, TimeUnit.MILLISECONDS);
		future2.get(100, TimeUnit.MILLISECONDS);
	}


	private void assertThreadNamePrefix(TestTask task) {
		assertEquals(THREAD_NAME_PREFIX, task.lastThread.getName().substring(0, THREAD_NAME_PREFIX.length()));
	}

	private void await(TestTask task) {
		await(task.latch);
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
			this.latch = (expectedRunCount > 0 ? new CountDownLatch(expectedRunCount) : null);
		}

		@Override
		public void run() {
			lastThread = Thread.currentThread();
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
			}
			if (expectedRunCount >= 0) {
				if (actualRunCount.incrementAndGet() > expectedRunCount) {
					throw new RuntimeException("intentional test failure");
				}
				latch.countDown();
			}
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
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
			}
			if (expectedRunCount >= 0) {
				if (actualRunCount.incrementAndGet() > expectedRunCount) {
					throw new RuntimeException("intentional test failure");
				}
			}
			return Thread.currentThread().getName();
		}
	}

}
