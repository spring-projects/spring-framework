/*
 * Copyright 2002-2024 the original author or authors.
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
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
 * A standard implementation of Spring's {@link TaskScheduler} interface, wrapping
 * a native {@link java.util.concurrent.ScheduledThreadPoolExecutor} and providing
 * all applicable configuration options for it. The default number of scheduler
 * threads is 1; a higher number can be configured through {@link #setPoolSize}.
 *
 * <p>This is Spring's traditional scheduler variant, staying as close as possible to
 * {@link java.util.concurrent.ScheduledExecutorService} semantics. Task execution happens
 * on the scheduler thread(s) rather than on separate execution threads. As a consequence,
 * a {@link ScheduledFuture} handle (e.g. from {@link #schedule(Runnable, Instant)})
 * represents the actual completion of the provided task (or series of repeated tasks).
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
 * @see ThreadPoolTaskExecutor
 * @see SimpleAsyncTaskScheduler
 */
@SuppressWarnings({"serial", "deprecation"})
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

	private static final TimeUnit NANO = TimeUnit.NANOSECONDS;


	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy;

	private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;

	private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;

	@Nullable
	private TaskDecorator taskDecorator;

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
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setCorePoolSize(poolSize);
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
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setRemoveOnCancelPolicy(flag);
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
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(flag);
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
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(flag);
		}
		this.executeExistingDelayedTasksAfterShutdownPolicy = flag;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not being applied to the user-supplied
	 * {@code Runnable}/{@code Callable} but rather to the scheduled execution
	 * callback (a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * @since 6.2
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
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
		Assert.notNull(clock, "Clock must not be null");
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

		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			if (this.removeOnCancelPolicy) {
				threadPoolExecutor.setRemoveOnCancelPolicy(true);
			}
			if (this.continueExistingPeriodicTasksAfterShutdownPolicy) {
				threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
			}
			if (!this.executeExistingDelayedTasksAfterShutdownPolicy) {
				threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
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

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler) {
			@Override
			protected void beforeExecute(Thread thread, Runnable task) {
				ThreadPoolTaskScheduler.this.beforeExecute(thread, task);
			}
			@Override
			protected void afterExecute(Runnable task, Throwable ex) {
				ThreadPoolTaskScheduler.this.afterExecute(task, ex);
			}
			@Override
			protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
				return decorateTaskIfNecessary(task);
			}
			@Override
			protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
				return decorateTaskIfNecessary(task);
			}
		};
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
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(new DelegatingErrorHandlingCallable<>(task, this.errorHandler));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
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
			throw new TaskRejectedException(executor, task, ex);
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
			throw new TaskRejectedException(executor, task, ex);
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
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration delay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.schedule(errorHandlingTask(task, false), NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration initialDelay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
					NANO.convert(initialDelay), NANO.convert(period), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
					0, NANO.convert(period), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration initialDelay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
					NANO.convert(initialDelay), NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
					0, NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}


	private <V> RunnableScheduledFuture<V> decorateTaskIfNecessary(RunnableScheduledFuture<V> future) {
		return (this.taskDecorator != null ? new DelegatingRunnableScheduledFuture<>(future, this.taskDecorator) :
				future);
	}

	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}


	private static class DelegatingRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {

		private final RunnableScheduledFuture<V> future;

		private final Runnable decoratedRunnable;

		public DelegatingRunnableScheduledFuture(RunnableScheduledFuture<V> future, TaskDecorator taskDecorator) {
			this.future = future;
			this.decoratedRunnable = taskDecorator.decorate(this.future);
		}

		@Override
		public void run() {
			this.decoratedRunnable.run();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return this.future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return this.future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return this.future.isDone();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return this.future.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return this.future.get(timeout, unit);
		}

		@Override
		public boolean isPeriodic() {
			return this.future.isPeriodic();
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return this.future.getDelay(unit);
		}

		@Override
		public int compareTo(Delayed o) {
			return this.future.compareTo(o);
		}
	}

}
