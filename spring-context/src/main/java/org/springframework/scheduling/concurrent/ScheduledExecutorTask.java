/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JavaBean that describes a scheduled executor task, consisting of the
 * {@link Runnable} and a delay plus period. The period needs to be specified;
 * there is no point in a default for it.
 *
 * <p>The {@link java.util.concurrent.ScheduledExecutorService} does not offer
 * more sophisticated scheduling options such as cron expressions.
 * Consider using {@link ThreadPoolTaskScheduler} for such needs.
 *
 * <p>Note that the {@link java.util.concurrent.ScheduledExecutorService} mechanism
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which creates a new Job instance for each execution.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 */
public class ScheduledExecutorTask {

	@Nullable
	private Runnable runnable;

	private long delay = 0;

	private long period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;


	/**
	 * Create a new ScheduledExecutorTask,
	 * to be populated via bean properties.
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledExecutorTask() {
	}

	/**
	 * Create a new ScheduledExecutorTask, with default
	 * one-time execution without delay.
	 * @param executorTask the Runnable to schedule
	 */
	public ScheduledExecutorTask(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * Create a new ScheduledExecutorTask, with default
	 * one-time execution with the given delay.
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay) {
		this.runnable = executorTask;
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledExecutorTask.
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
		this.runnable = executorTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * Set the Runnable to schedule as executor task.
	 */
	public void setRunnable(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * Return the Runnable to schedule as executor task.
	 */
	public Runnable getRunnable() {
		Assert.state(this.runnable != null, "No Runnable set");
		return this.runnable;
	}

	/**
	 * Set the delay before starting the task for the first time,
	 * in milliseconds. Default is 0, immediately starting the
	 * task after successful scheduling.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * Return the delay before starting the job for the first time.
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * Set the period between repeated task executions, in milliseconds.
	 * <p>Default is -1, leading to one-time execution. In case of a positive value,
	 * the task will be executed repeatedly, with the given interval in-between executions.
	 * <p>Note that the semantics of the period value vary between fixed-rate and
	 * fixed-delay execution.
	 * <p><b>Note:</b> A period of 0 (for example as fixed delay) is <i>not</i> supported,
	 * simply because {@code java.util.concurrent.ScheduledExecutorService} itself
	 * does not support it. Hence a value of 0 will be treated as one-time execution;
	 * however, that value should never be specified explicitly in the first place!
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * Return the period between repeated task executions.
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * Is this task only ever going to execute once?
	 * @return {@code true} if this task is only ever going to execute once
	 * @see #getPeriod()
	 */
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * Specify the time unit for the delay and period values.
	 * Default is milliseconds ({@code TimeUnit.MILLISECONDS}).
	 * @see java.util.concurrent.TimeUnit#MILLISECONDS
	 * @see java.util.concurrent.TimeUnit#SECONDS
	 */
	public void setTimeUnit(@Nullable TimeUnit timeUnit) {
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
	}

	/**
	 * Return the time unit for the delay and period values.
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * Set whether to schedule as fixed-rate execution, rather than
	 * fixed-delay execution. Default is "false", that is, fixed delay.
	 * <p>See ScheduledExecutorService javadoc for details on those execution modes.
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * Return whether to schedule as fixed-rate execution.
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
