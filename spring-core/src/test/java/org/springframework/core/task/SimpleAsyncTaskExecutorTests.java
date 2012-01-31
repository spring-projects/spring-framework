/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.ThreadFactory;

import junit.framework.TestCase;

import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public final class SimpleAsyncTaskExecutorTests extends TestCase {

	public void testCannotExecuteWhenConcurrencyIsSwitchedOff() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(ConcurrencyThrottleSupport.NO_CONCURRENCY);
		assertFalse(executor.isThrottleActive());
		try {
			executor.execute(new NoOpRunnable());
		}
		catch (IllegalStateException expected) {
		}
	}

	public void testThrottleIsNotActiveByDefault() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		assertFalse("Concurrency throttle must not default to being active (on)", executor.isThrottleActive());
	}

	public void testThreadNameGetsSetCorrectly() throws Exception {
		final String customPrefix = "chankPop#";
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix);
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertTrue(task.getThreadName().startsWith(customPrefix));
	}

	public void testThreadFactoryOverridesDefaults() throws Exception {
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				return new Thread(r, "test");
			}
		});
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertTrue(task.getThreadName().equals("test"));
	}

	public void testThrowsExceptionWhenSuppliedWithNullRunnable() throws Exception {
		try {
			new SimpleAsyncTaskExecutor().execute(null);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	private void executeAndWait(SimpleAsyncTaskExecutor executor, Runnable task, Object monitor) {
		synchronized (monitor) {
			executor.execute(task);
			try {
				monitor.wait();
			}
			catch (InterruptedException ignored) {
			}
		}
	}


	private static final class NoOpRunnable implements Runnable {

		public void run() {
			// no-op
		}
	}


	private static abstract class AbstractNotifyingRunnable implements Runnable {

		private final Object monitor;

		protected AbstractNotifyingRunnable(Object monitor) {
			this.monitor = monitor;
		}

		public final void run() {
			synchronized (this.monitor) {
				try {
					doRun();
				}
				finally {
					this.monitor.notifyAll();
				}
			}
		}

		protected abstract void doRun();
	}


	private static final class ThreadNameHarvester extends AbstractNotifyingRunnable {

		private String threadName;

		protected ThreadNameHarvester(Object monitor) {
			super(monitor);
		}

		public String getThreadName() {
			return this.threadName;
		}

		protected void doRun() {
			this.threadName = Thread.currentThread().getName();
		}
	}

}
