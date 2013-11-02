/*
 * Copyright 2002-2013 the original author or authors.
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
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

/**
 * Adapter that takes a {@code java.util.concurrent.ScheduledExecutorService} and
 * exposes a Spring {@link org.springframework.scheduling.TaskScheduler} for it.
 * Extends {@link ConcurrentTaskExecutor} in order to implement the
 * {@link org.springframework.scheduling.SchedulingTaskExecutor} interface as well.
 *
 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
 * in order to use it for trigger-based scheduling if possible, instead of Spring's
 * local trigger management which ends up delegating to regular delay-based scheduling
 * against the {@code java.util.concurrent.ScheduledExecutorService} API. For JSR-236 style
 * lookup in a Java EE 7 environment, consider using {@link DefaultManagedTaskScheduler}.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskScheduler} that allows for
 * defining a {@link java.util.concurrent.ScheduledThreadPoolExecutor} in bean style,
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
 * @see DefaultManagedTaskScheduler
 * @see ThreadPoolTaskScheduler
 */
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

	private static Class<?> managedScheduledExecutorServiceClass;

	static {
		try {
			managedScheduledExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedScheduledExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API not available...
			managedScheduledExecutorServiceClass = null;
		}
	}

	private ScheduledExecutorService scheduledExecutor;

	private boolean enterpriseConcurrentScheduler = false;

	private ErrorHandler errorHandler;


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
	 * Create a new ConcurrentTaskScheduler, using the given
	 * {@link java.util.concurrent.ScheduledExecutorService} as shared delegate.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * @param scheduledExecutor the {@link java.util.concurrent.ScheduledExecutorService}
	 * to delegate to for {@link org.springframework.scheduling.SchedulingTaskExecutor}
	 * as well as {@link TaskScheduler} invocations
	 */
	public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
		super(scheduledExecutor);
		setScheduledExecutor(scheduledExecutor);
	}

	/**
	 * Create a new ConcurrentTaskScheduler, using the given {@link java.util.concurrent.Executor}
	 * and {@link java.util.concurrent.ScheduledExecutorService} as delegates.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * @param concurrentExecutor the {@link java.util.concurrent.Executor} to delegate to
	 * for {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * @param scheduledExecutor the {@link java.util.concurrent.ScheduledExecutorService}
	 * to delegate to for {@link TaskScheduler} invocations
	 */
	public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
		super(concurrentExecutor);
		setScheduledExecutor(scheduledExecutor);
	}


	/**
	 * Specify the {@link java.util.concurrent.ScheduledExecutorService} to delegate to.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * <p>Note: This will only apply to {@link TaskScheduler} invocations.
	 * If you want the given executor to apply to
	 * {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * as well, pass the same executor reference to {@link #setConcurrentExecutor}.
	 * @see #setConcurrentExecutor
	 */
	public final void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
		if (scheduledExecutor != null) {
			this.scheduledExecutor = scheduledExecutor;
			this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null &&
					managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
		}
		else {
			this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			this.enterpriseConcurrentScheduler = false;
		}
	}

	/**
	 * Provide an {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		this.errorHandler = errorHandler;
	}


	@Override
	public ScheduledFuture schedule(Runnable task, Trigger trigger) {
		try {
			if (this.enterpriseConcurrentScheduler) {
				return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
			}
			else {
				ErrorHandler errorHandler = (this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
				return new ReschedulingRunnable(task, trigger, this.scheduledExecutor, errorHandler).schedule();
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture schedule(Runnable task, Date startTime) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.schedule(decorateTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
		Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
		if (this.enterpriseConcurrentScheduler) {
			result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
		}
		return result;
	}


	/**
	 * Delegate that adapts a Spring Trigger to a JSR-236 Trigger.
	 * Separated into an inner class in order to avoid a hard dependency on the JSR-236 API.
	 */
	private class EnterpriseConcurrentTriggerScheduler {

		public ScheduledFuture schedule(Runnable task, final Trigger trigger) {
			ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
			return executor.schedule(task, new javax.enterprise.concurrent.Trigger() {
				@Override
				public Date getNextRunTime(LastExecution le, Date taskScheduledTime) {
					return trigger.nextExecutionTime(le != null ?
							new SimpleTriggerContext(le.getScheduledStart(), le.getRunStart(), le.getRunEnd()) :
							new SimpleTriggerContext());
				}
				@Override
				public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
					return false;
				}
			});
		}
	}

}
