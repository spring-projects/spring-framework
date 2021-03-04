/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;

import org.springframework.util.ConcurrencyThrottleSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SimpleAsyncTaskExecutorTests {

	@Test
	void cannotExecuteWhenConcurrencyIsSwitchedOff() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(ConcurrencyThrottleSupport.NO_CONCURRENCY);
		assertThat(executor.isThrottleActive()).isTrue();
		assertThatIllegalStateException().isThrownBy(() ->
				executor.execute(new NoOpRunnable()));
	}

	@Test
	void throttleIsNotActiveByDefault() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		assertThat(executor.isThrottleActive()).as("Concurrency throttle must not default to being active (on)").isFalse();
	}

	@Test
	void threadNameGetsSetCorrectly() throws Exception {
		final String customPrefix = "chankPop#";
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix);
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).startsWith(customPrefix);
	}

	@Test
	void threadFactoryOverridesDefaults() throws Exception {
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "test");
			}
		});
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).isEqualTo("test");
	}

	@Test
	void throwsExceptionWhenSuppliedWithNullRunnable() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SimpleAsyncTaskExecutor().execute(null));
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

		@Override
		public void run() {
			// no-op
		}
	}


	private static abstract class AbstractNotifyingRunnable implements Runnable {

		private final Object monitor;

		protected AbstractNotifyingRunnable(Object monitor) {
			this.monitor = monitor;
		}

		@Override
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

		@Override
		protected void doRun() {
			this.threadName = Thread.currentThread().getName();
		}
	}

}
