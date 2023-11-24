/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * {@link TaskExecutor} implementation that fires up a new Thread for each task,
 * executing it asynchronously. Provides a virtual thread option on JDK 21.
 *
 * <p>Supports a graceful shutdown through {@link #setTaskTerminationTimeout},
 * at the expense of task tracking overhead per execution thread at runtime.
 * Supports limiting concurrent threads through {@link #setConcurrencyLimit}.
 * By default, the number of concurrent task executions is unlimited.
 *
 * <p><b>NOTE: This implementation does not reuse threads!</b> Consider a
 * thread-pooling TaskExecutor implementation instead, in particular for
 * executing a large number of short-lived tasks. Alternatively, on JDK 21,
 * consider setting {@link #setVirtualThreads} to {@code true}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setVirtualThreads
 * @see #setTaskTerminationTimeout
 * @see #setConcurrencyLimit
 * @see org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 */
@SuppressWarnings({"serial", "deprecation"})
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
		implements AsyncListenableTaskExecutor, Serializable, AutoCloseable {

	/**
	 * Permit any number of concurrent invocations: that is, don't throttle concurrency.
	 * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
	 */
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * Switch concurrency 'off': that is, don't allow any concurrent invocations.
	 * @see ConcurrencyThrottleSupport#NO_CONCURRENCY
	 */
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/** Internal concurrency throttle used by this executor. */
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

	@Nullable
	private VirtualThreadDelegate virtualThreadDelegate;

	@Nullable
	private ThreadFactory threadFactory;

	@Nullable
	private TaskDecorator taskDecorator;

	private long taskTerminationTimeout;

	@Nullable
	private Set<Thread> activeThreads;

	private volatile boolean active = true;


	/**
	 * Create a new SimpleAsyncTaskExecutor with default thread name prefix.
	 */
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * Create a new SimpleAsyncTaskExecutor with the given thread name prefix.
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	/**
	 * Create a new SimpleAsyncTaskExecutor with the given external thread factory.
	 * @param threadFactory the factory to use for creating new Threads
	 */
	public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}


	/**
	 * Switch this executor to virtual threads. Requires Java 21 or higher.
	 * <p>The default is {@code false}, indicating platform threads.
	 * Set this flag to {@code true} in order to create virtual threads instead.
	 * @since 6.1
	 */
	public void setVirtualThreads(boolean virtual) {
		this.virtualThreadDelegate = (virtual ? new VirtualThreadDelegate() : null);
	}

	/**
	 * Specify an external factory to use for creating new Threads,
	 * instead of relying on the local properties of this executor.
	 * <p>You may specify an inner ThreadFactory bean or also a ThreadFactory reference
	 * obtained from JNDI (on a Jakarta EE server) or some other lookup mechanism.
	 * @see #setThreadNamePrefix
	 * @see #setThreadPriority
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * Return the external factory to use for creating new Threads, if any.
	 */
	@Nullable
	public final ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not necessarily being applied to the
	 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
	 * execution callback (which may be a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
	 * is limited to plain {@code Runnable} execution via {@code execute} calls.
	 * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
	 * {@code FutureTask} which does not propagate any exceptions; you might
	 * have to cast it and call {@code Future#get} to evaluate exceptions.
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * Specify a timeout for task termination when closing this executor.
	 * The default is 0, not waiting for task termination at all.
	 * <p>Note that a concrete >0 timeout specified here will lead to the
	 * wrapping of every submitted task into a task-tracking runnable which
	 * involves considerable overhead in case of a high number of tasks.
	 * However, for a modest level of submissions with longer-running
	 * tasks, this is feasible in order to arrive at a graceful shutdown.
	 * @param timeout the timeout in milliseconds
	 * @since 6.1
	 * @see #close()
	 * @see org.springframework.scheduling.concurrent.ExecutorConfigurationSupport#setAwaitTerminationMillis
	 */
	public void setTaskTerminationTimeout(long timeout) {
		Assert.isTrue(timeout >= 0, "Timeout value must be >=0");
		this.taskTerminationTimeout = timeout;
		this.activeThreads = (timeout > 0 ? Collections.newSetFromMap(new ConcurrentHashMap<>()) : null);
	}

	/**
	 * Return whether this executor is still active, i.e. not closed yet,
	 * and therefore accepts further task submissions. Otherwise, it is
	 * either in the task termination phase or entirely shut down already.
	 * @since 6.1
	 * @see #setTaskTerminationTimeout
	 * @see #close()
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * Set the maximum number of parallel task executions allowed.
	 * The default of -1 indicates no concurrency limit at all.
	 * <p>This is the equivalent of a maximum pool size in a thread pool,
	 * preventing temporary overload of the thread management system.
	 * @see #UNBOUNDED_CONCURRENCY
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor#setMaxPoolSize
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * Return the maximum number of parallel task executions allowed.
	 */
	public final int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * Return whether the concurrency throttle is currently active.
	 * @return {@code true} if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 * @see #setConcurrencyLimit
	 */
	public final boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * @see #doExecute(Runnable)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * <p>Executes urgent tasks (with 'immediate' timeout) directly,
	 * bypassing the concurrency throttle (if active). All other
	 * tasks are subject to throttling.
	 * @see #TIMEOUT_IMMEDIATE
	 * @see #doExecute(Runnable)
	 */
	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		if (!isActive()) {
			throw new TaskRejectedException(getClass().getSimpleName() + " has been closed already");
		}

		Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new TaskTrackingRunnable(taskToUse));
		}
		else if (this.activeThreads != null) {
			doExecute(new TaskTrackingRunnable(taskToUse));
		}
		else {
			doExecute(taskToUse);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * Template method for the actual execution of a task.
	 * <p>The default implementation creates a new Thread and starts it.
	 * @param task the Runnable to execute
	 * @see #newThread
	 * @see Thread#start()
	 */
	protected void doExecute(Runnable task) {
		newThread(task).start();
	}

	/**
	 * Create a new Thread for the given task.
	 * @param task the Runnable to create a Thread for
	 * @return the new Thread instance
	 * @since 6.1
	 * @see #setVirtualThreads
	 * @see #setThreadFactory
	 * @see #createThread
	 */
	protected Thread newThread(Runnable task) {
		if (this.virtualThreadDelegate != null) {
			return this.virtualThreadDelegate.newVirtualThread(nextThreadName(), task);
		}
		else {
			return (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		}
	}

	/**
	 * This close methods tracks the termination of active threads if a concrete
	 * {@link #setTaskTerminationTimeout task termination timeout} has been set.
	 * Otherwise, it is not necessary to close this executor.
	 * @since 6.1
	 */
	@Override
	public void close() {
		if (this.active) {
			this.active = false;
			Set<Thread> threads = this.activeThreads;
			if (threads != null) {
				threads.forEach(Thread::interrupt);
				synchronized (threads) {
					try {
						if (!threads.isEmpty()) {
							threads.wait(this.taskTerminationTimeout);
						}
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}


	/**
	 * Subclass of the general ConcurrencyThrottleSupport class,
	 * making {@code beforeAccess()} and {@code afterAccess()}
	 * visible to the surrounding class.
	 */
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		@Override
		protected void beforeAccess() {
			super.beforeAccess();
		}

		@Override
		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * Decorates a target task with active thread tracking
	 * and concurrency throttle management, if necessary.
	 */
	private class TaskTrackingRunnable implements Runnable {

		private final Runnable task;

		public TaskTrackingRunnable(Runnable task) {
			Assert.notNull(task, "Task must not be null");
			this.task = task;
		}

		@Override
		public void run() {
			Set<Thread> threads = activeThreads;
			Thread thread = null;
			if (threads != null) {
				thread = Thread.currentThread();
				threads.add(thread);
			}
			try {
				this.task.run();
			}
			finally {
				if (threads != null) {
					threads.remove(thread);
					if (!isActive()) {
						synchronized (threads) {
							if (threads.isEmpty()) {
								threads.notify();
							}
						}
					}
				}
				concurrencyThrottle.afterAccess();
			}
		}
	}

}
