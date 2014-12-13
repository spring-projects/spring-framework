/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.lang.UsesJava7;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @see #setThreadFactory
 * @see #setErrorHandler
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

	// ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(boolean) only available on JDK 7+
	private static final boolean setRemoveOnCancelPolicyAvailable =
			ClassUtils.hasMethod(ScheduledThreadPoolExecutor.class, "setRemoveOnCancelPolicy", boolean.class);


	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy = false;

	private volatile ScheduledExecutorService scheduledExecutor;

	private volatile ErrorHandler errorHandler;


	/**
	 * Set the ScheduledExecutorService's pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setCorePoolSize(poolSize);
		}
	}

	/**
	 * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor} (JDK 7+).
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into remove-on-cancel mode (if possible, with a soft fallback otherwise).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	@UsesJava7
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
		if (setRemoveOnCancelPolicyAvailable && this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(removeOnCancelPolicy);
		}
		else if (removeOnCancelPolicy && this.scheduledExecutor != null) {
			logger.info("Could not apply remove-on-cancel policy - not a Java 7+ ScheduledThreadPoolExecutor");
		}
	}

	/**
	 * Set a custom {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}


	@UsesJava7
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		this.scheduledExecutor = createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (setRemoveOnCancelPolicyAvailable && this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.info("Could not apply remove-on-cancel policy - not a Java 7+ ScheduledThreadPoolExecutor");
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
	 * Return the current setting for the remove-on-cancel mode.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 */
	@UsesJava7
	public boolean isRemoveOnCancelPolicy() {
		if (!setRemoveOnCancelPolicyAvailable) {
			return false;
		}
		if (this.scheduledExecutor == null) {
			// Not initialized yet: return our setting for the time being.
			return this.removeOnCancelPolicy;
		}
		return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
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
			if (this.errorHandler != null) {
				taskToUse = new DelegatingErrorHandlingCallable<T>(task, this.errorHandler);
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
			ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
			executor.execute(errorHandlingTask(future, false));
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
			executor.execute(errorHandlingTask(future, false));
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}


	// TaskScheduler implementation

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			ErrorHandler errorHandler =
					(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
			return new ReschedulingRunnable(task, trigger, executor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
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
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
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
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
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
		public V call() throws Exception {
			try {
				return this.delegate.call();
			}
			catch (Throwable t) {
				this.errorHandler.handleError(t);
				return null;
			}
		}
	}

}
