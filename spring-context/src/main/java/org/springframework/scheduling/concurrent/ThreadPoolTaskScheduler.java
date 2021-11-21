/*
 * Copyright 2002-2021 the original author or authors.
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

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ErrorHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * Implementation of Spring's {@link TaskScheduler} interface, wrapping
 * a native {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
 * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
 * @see #setThreadFactory
 * @see #setErrorHandler
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy;

	private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;

	private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;

	@Nullable
	private volatile ErrorHandler errorHandler;

	private Clock clock = Clock.systemDefaultZone();

	@Nullable
	private ScheduledExecutorService scheduledExecutor;

	// Underlying ScheduledFutureTask to user-level ListenableFuture handle, if any
	private final Map<Object, ListenableFuture<?>> listenableFutureMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * Set the ScheduledExecutorService's pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setCorePoolSize(poolSize);
		}
		this.poolSize = poolSize;
	}

	/**
	 * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor}.
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into remove-on-cancel mode (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy
	 */
	public void setRemoveOnCancelPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(flag);
		}
		this.removeOnCancelPolicy = flag;
	}

	/**
	 * Set whether to continue existing periodic tasks even when this executor has been shutdown.
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into continuing periodic tasks (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setContinueExistingPeriodicTasksAfterShutdownPolicy
	 */
	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setContinueExistingPeriodicTasksAfterShutdownPolicy(flag);
		}
		this.continueExistingPeriodicTasksAfterShutdownPolicy = flag;
	}

	/**
	 * Set whether to execute existing delayed tasks even when this executor has been shutdown.
	 * <p>Default is {@code true}. If set to {@code false}, the target executor will be
	 * switched into dropping remaining tasks (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setExecuteExistingDelayedTasksAfterShutdownPolicy
	 */
	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setExecuteExistingDelayedTasksAfterShutdownPolicy(flag);
		}
		this.executeExistingDelayedTasksAfterShutdownPolicy = flag;
	}

	/**
	 * Set a custom {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the clock to use for scheduling purposes.
	 * <p>The default clock is the system clock for the default time zone.
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	public void setClock(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		this.scheduledExecutor = createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor scheduledPoolExecutor) {
			if (this.removeOnCancelPolicy) {
				scheduledPoolExecutor.setRemoveOnCancelPolicy(true);
			}
			if (this.continueExistingPeriodicTasksAfterShutdownPolicy) {
				scheduledPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
			}
			if (!this.executeExistingDelayedTasksAfterShutdownPolicy) {
				scheduledPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			}
		}

		return this.scheduledExecutor;
	}

	/**
	 * Create a new {@link ScheduledExecutorService} instance.
	 * <p>The default implementation creates a {@link ScheduledThreadPoolExecutor}.
	 * Can be overridden in subclasses to provide custom {@link ScheduledExecutorService} instances.
	 * @param poolSize the specified pool size
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ScheduledExecutorService instance
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * Return the underlying ScheduledExecutorService for native access.
	 * @return the underlying ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
	 */
	public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor != null, "ThreadPoolTaskScheduler not initialized");
		return this.scheduledExecutor;
	}

	/**
	 * Return the underlying ScheduledThreadPoolExecutor, if available.
	 * @return the underlying ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
	 * or if the underlying ScheduledExecutorService isn't a ScheduledThreadPoolExecutor
	 * @see #getScheduledExecutor()
	 */
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor instanceof ScheduledThreadPoolExecutor,
				"No ScheduledThreadPoolExecutor available");
		return (ScheduledThreadPoolExecutor) this.scheduledExecutor;
	}

	/**
	 * Return the current pool size.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: assume initial pool size.
			return this.poolSize;
		}
		return getScheduledThreadPoolExecutor().getPoolSize();
	}

	/**
	 * Return the number of currently active threads.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: assume no active threads.
			return 0;
		}
		return getScheduledThreadPoolExecutor().getActiveCount();
	}

	/**
	 * Return the current setting for the remove-on-cancel mode.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @deprecated as of 5.3.9, in favor of direct
	 * {@link #getScheduledThreadPoolExecutor()} access
	 */
	@Deprecated
	public boolean isRemoveOnCancelPolicy() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: return our setting for the time being.
			return this.removeOnCancelPolicy;
		}
		return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
	}


	// SchedulingTaskExecutor implementation

	@Override
	public void execute(Runnable task) {
		Executor executor = getScheduledExecutor();
		try {
			executor.execute(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			Callable<T> taskToUse = task;
			ErrorHandler errorHandler = this.errorHandler;
			if (errorHandler != null) {
				taskToUse = new DelegatingErrorHandlingCallable<>(task, errorHandler);
			}
			return executor.submit(taskToUse);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<Object> listenableFuture = new ListenableFutureTask<>(task, null);
			executeAndTrack(executor, listenableFuture);
			return listenableFuture;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<T> listenableFuture = new ListenableFutureTask<>(task);
			executeAndTrack(executor, listenableFuture);
			return listenableFuture;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	private void executeAndTrack(ExecutorService executor, ListenableFutureTask<?> listenableFuture) {
		Future<?> scheduledFuture = executor.submit(errorHandlingTask(listenableFuture, false));
		this.listenableFutureMap.put(scheduledFuture, listenableFuture);
		listenableFuture.addCallback(result -> this.listenableFutureMap.remove(scheduledFuture),
				ex -> this.listenableFutureMap.remove(scheduledFuture));
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// Cancel associated user-level ListenableFuture handle as well
		ListenableFuture<?> listenableFuture = this.listenableFutureMap.get(task);
		if (listenableFuture != null) {
			listenableFuture.cancel(true);
		}
	}


	// TaskScheduler implementation

	@Override
	@Nullable
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			ErrorHandler errorHandler = this.errorHandler;
			if (errorHandler == null) {
				errorHandler = TaskUtils.getDefaultErrorHandler(true);
			}
			return new ReschedulingRunnable(task, trigger, this.clock, executor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.schedule(errorHandlingTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}


	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}


	private static class DelegatingErrorHandlingCallable<V> implements Callable<V> {

		private final Callable<V> delegate;

		private final ErrorHandler errorHandler;

		public DelegatingErrorHandlingCallable(Callable<V> delegate, ErrorHandler errorHandler) {
			this.delegate = delegate;
			this.errorHandler = errorHandler;
		}

		@Override
		@Nullable
		public V call() throws Exception {
			try {
				return this.delegate.call();
			}
			catch (Throwable ex) {
				this.errorHandler.handleError(ex);
				return null;
			}
		}
	}

}
