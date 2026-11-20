/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 6.1
 */
class VirtualThreadTaskExecutorTests {

	@Test
	void virtualThreadsWithoutName() {
		final Object monitor = new Object();
		VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor();
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).isEmpty();
		assertThat(task.isVirtual()).isTrue();
		assertThat(task.runCount()).isOne();
	}

	@Test
	void virtualThreadsWithNamePrefix() {
		final Object monitor = new Object();
		VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor("test-");
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).isEqualTo("test-0");
		assertThat(task.isVirtual()).isTrue();
		assertThat(task.runCount()).isOne();
	}

	@Test
	void simpleWithVirtualThreadFactory() {
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().name("test").factory());
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).isEqualTo("test");
		assertThat(task.isVirtual()).isTrue();
		assertThat(task.runCount()).isOne();
	}

	@Test
	void simpleWithVirtualThreadFlag() {
		final String customPrefix = "chankPop#";
		final Object monitor = new Object();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix);
		executor.setVirtualThreads(true);
		ThreadNameHarvester task = new ThreadNameHarvester(monitor);
		executeAndWait(executor, task, monitor);
		assertThat(task.getThreadName()).startsWith(customPrefix);
		assertThat(task.isVirtual()).isTrue();
		assertThat(task.runCount()).isOne();
	}

	private void executeAndWait(TaskExecutor executor, Runnable task, Object monitor) {
		synchronized (monitor) {
			executor.execute(task);
			try {
				monitor.wait();
			}
			catch (InterruptedException ignored) {
			}
		}
	}


	private abstract static class AbstractNotifyingRunnable implements Runnable {

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

		private final AtomicInteger runCount = new AtomicInteger();

		private String threadName;

		private boolean virtual;

		protected ThreadNameHarvester(Object monitor) {
			super(monitor);
		}

		public String getThreadName() {
			return this.threadName;
		}

		public boolean isVirtual() {
			return this.virtual;
		}

		public int runCount() {
			return this.runCount.get();
		}

		@Override
		protected void doRun() {
			Thread thread = Thread.currentThread();
			this.threadName = thread.getName();
			this.virtual = thread.isVirtual();
			runCount.incrementAndGet();
		}
	}

}
