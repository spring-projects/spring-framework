/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * Adapter that takes a JDK 1.5 {@code java.util.concurrent.ScheduledExecutorService}
 * and exposes a Spring {@link org.springframework.scheduling.TaskScheduler} for it.
 * Extends {@link ConcurrentTaskExecutor} in order to implement the
 * {@link org.springframework.scheduling.SchedulingTaskExecutor} interface as well.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskScheduler} that allows for
 * defining a JDK 1.5 {@link java.util.concurrent.ScheduledThreadPoolExecutor} in bean style,
 * exposing it as a Spring {@link org.springframework.scheduling.TaskScheduler} directly.
 * This is a convenient alternative to a raw ScheduledThreadPoolExecutor definition with
 * a separate definition of the present adapter class.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see ThreadPoolTaskScheduler
 */
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

	private volatile ScheduledExecutorService scheduledExecutor;

	private volatile ErrorHandler errorHandler;


	/**
	 * Create a new ConcurrentTaskScheduler,
	 * using a single thread executor as default.
	 * @see java.util.concurrent.Executors#newSingleThreadScheduledExecutor()
	 */
	public ConcurrentTaskScheduler() {
		super();
		setScheduledExecutor(null);
	}

	/**
	 * Create a new ConcurrentTaskScheduler,
	 * using the given JDK 1.5 executor as shared delegate.
	 * @param scheduledExecutor the JDK 1.5 scheduled executor to delegate to
	 * for {@link org.springframework.scheduling.SchedulingTaskExecutor} as well
	 * as {@link TaskScheduler} invocations
	 */
	public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
		super(scheduledExecutor);
		setScheduledExecutor(scheduledExecutor);
	}

	/**
	 * Create a new ConcurrentTaskScheduler,
	 * using the given JDK 1.5 executors as delegates.
	 * @param concurrentExecutor the JDK 1.5 concurrent executor to delegate to
	 * for {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * @param scheduledExecutor the JDK 1.5 scheduled executor to delegate to
	 * for {@link TaskScheduler} invocations
	 */
	public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
		super(concurrentExecutor);
		setScheduledExecutor(scheduledExecutor);
	}


	/**
	 * Specify the JDK 1.5 scheduled executor to delegate to.
	 * <p>Note: This will only apply to {@link TaskScheduler} invocations.
	 * If you want the given executor to apply to
	 * {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * as well, pass the same executor reference to {@link #setConcurrentExecutor}.
	 * @see #setConcurrentExecutor
	 */
	public final void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
		this.scheduledExecutor =
				(scheduledExecutor != null ? scheduledExecutor : Executors.newSingleThreadScheduledExecutor());
	}

	/**
	 * Provide an {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		this.errorHandler = errorHandler;
	}


	public ScheduledFuture schedule(Runnable task, Trigger trigger) {
		try {
			ErrorHandler errorHandler =
					(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
			return new ReschedulingRunnable(task, trigger, this.scheduledExecutor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	public ScheduledFuture schedule(Runnable task, Date startTime) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.schedule(
					errorHandlingTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(
					errorHandlingTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(
					errorHandlingTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(
					errorHandlingTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(
					errorHandlingTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}

}
