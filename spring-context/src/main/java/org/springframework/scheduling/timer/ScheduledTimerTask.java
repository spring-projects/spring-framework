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

package org.springframework.scheduling.timer;

import java.util.TimerTask;

/**
 * JavaBean that describes a scheduled {@link TimerTask}, consisting of the
 * {@link TimerTask} itself or a {@link Runnable} to create a {@link TimerTask}
 * for and a delay plus period. The period needs to be specified; there is
 * no point in a default for it.
 *
 * <p>The JDK's {@link java.util.Timer} facility does not offer sophisticated
 * scheduling options such as cron expressions. Consider using Quartz for
 * such advanced needs.
 *
 * <p>Note that the {@link java.util.Timer} mechanism uses a {@link TimerTask}
 * instance that is shared between repeated executions, in contrast to Quartz
 * which creates a new Job instance for each execution.
 *
 * @author Juergen Hoeller
 * @since 19.02.2004
 * @see java.util.TimerTask
 * @see java.util.Timer#schedule(TimerTask, long, long)
 * @see java.util.Timer#scheduleAtFixedRate(TimerTask, long, long)
 * @deprecated as of Spring 3.0, in favor of the <code>scheduling.concurrent</code>
 * package which is based on Java 5's <code>java.util.concurrent.ExecutorService</code>
 */
@Deprecated
public class ScheduledTimerTask {

	private TimerTask timerTask;

	private long delay = 0;

	private long period = -1;

	private boolean fixedRate = false;


	/**
	 * Create a new ScheduledTimerTask,
	 * to be populated via bean properties.
	 * @see #setTimerTask
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledTimerTask() {
	}

	/**
	 * Create a new ScheduledTimerTask, with default
	 * one-time execution without delay.
	 * @param timerTask the TimerTask to schedule
	 */
	public ScheduledTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	/**
	 * Create a new ScheduledTimerTask, with default
	 * one-time execution with the given delay.
	 * @param timerTask the TimerTask to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerTask(TimerTask timerTask, long delay) {
		this.timerTask = timerTask;
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledTimerTask.
	 * @param timerTask the TimerTask to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerTask(TimerTask timerTask, long delay, long period, boolean fixedRate) {
		this.timerTask = timerTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}

	/**
	 * Create a new ScheduledTimerTask, with default
	 * one-time execution without delay.
	 * @param timerTask the Runnable to schedule as TimerTask
	 */
	public ScheduledTimerTask(Runnable timerTask) {
		setRunnable(timerTask);
	}

	/**
	 * Create a new ScheduledTimerTask, with default
	 * one-time execution with the given delay.
	 * @param timerTask the Runnable to schedule as TimerTask
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerTask(Runnable timerTask, long delay) {
		setRunnable(timerTask);
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledTimerTask.
	 * @param timerTask the Runnable to schedule as TimerTask
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerTask(Runnable timerTask, long delay, long period, boolean fixedRate) {
		setRunnable(timerTask);
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * Set the Runnable to schedule as TimerTask.
	 * @see DelegatingTimerTask
	 */
	public void setRunnable(Runnable timerTask) {
		this.timerTask = new DelegatingTimerTask(timerTask);
	}

	/**
	 * Set the TimerTask to schedule.
	 */
	public void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	/**
	 * Return the TimerTask to schedule.
	 */
	public TimerTask getTimerTask() {
		return this.timerTask;
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
	 * <p>Default is -1, leading to one-time execution. In case of a positive
	 * value, the task will be executed repeatedly, with the given interval
	 * inbetween executions.
	 * <p>Note that the semantics of the period value vary between fixed-rate
	 * and fixed-delay execution.
	 * <p><b>Note:</b> A period of 0 (for example as fixed delay) is <i>not</i>
	 * supported, simply because <code>java.util.Timer</code> itself does not
	 * support it. Hence a value of 0 will be treated as one-time execution;
	 * however, that value should never be specified explicitly in the first place!
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see java.util.Timer#schedule(TimerTask, long, long)
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
	 * @return <code>true</code> if this task is only ever going to execute once
	 * @see #getPeriod()
	 */
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * Set whether to schedule as fixed-rate execution, rather than
	 * fixed-delay execution. Default is "false", that is, fixed delay.
	 * <p>See Timer javadoc for details on those execution modes.
	 * @see java.util.Timer#schedule(TimerTask, long, long)
	 * @see java.util.Timer#scheduleAtFixedRate(TimerTask, long, long)
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
