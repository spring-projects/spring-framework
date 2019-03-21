/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.commonj;

import commonj.timers.TimerListener;

import org.springframework.lang.Nullable;

/**
 * JavaBean that describes a scheduled TimerListener, consisting of
 * the TimerListener itself (or a Runnable to create a TimerListener for)
 * and a delay plus period. Period needs to be specified;
 * there is no point in a default for it.
 *
 * <p>The CommonJ TimerManager does not offer more sophisticated scheduling
 * options such as cron expressions. Consider using Quartz for such
 * advanced needs.
 *
 * <p>Note that the TimerManager uses a TimerListener instance that is
 * shared between repeated executions, in contrast to Quartz which
 * instantiates a new Job for each execution.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see commonj.timers.TimerListener
 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
 */
public class ScheduledTimerListener {

	@Nullable
	private TimerListener timerListener;

	private long delay = 0;

	private long period = -1;

	private boolean fixedRate = false;


	/**
	 * Create a new ScheduledTimerListener,
	 * to be populated via bean properties.
	 * @see #setTimerListener
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledTimerListener() {
	}

	/**
	 * Create a new ScheduledTimerListener, with default
	 * one-time execution without delay.
	 * @param timerListener the TimerListener to schedule
	 */
	public ScheduledTimerListener(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * Create a new ScheduledTimerListener, with default
	 * one-time execution with the given delay.
	 * @param timerListener the TimerListener to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay) {
		this.timerListener = timerListener;
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledTimerListener.
	 * @param timerListener the TimerListener to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay, long period, boolean fixedRate) {
		this.timerListener = timerListener;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}

	/**
	 * Create a new ScheduledTimerListener, with default
	 * one-time execution without delay.
	 * @param timerTask the Runnable to schedule as TimerListener
	 */
	public ScheduledTimerListener(Runnable timerTask) {
		setRunnable(timerTask);
	}

	/**
	 * Create a new ScheduledTimerListener, with default
	 * one-time execution with the given delay.
	 * @param timerTask the Runnable to schedule as TimerListener
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay) {
		setRunnable(timerTask);
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledTimerListener.
	 * @param timerTask the Runnable to schedule as TimerListener
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay, long period, boolean fixedRate) {
		setRunnable(timerTask);
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * Set the Runnable to schedule as TimerListener.
	 * @see DelegatingTimerListener
	 */
	public void setRunnable(Runnable timerTask) {
		this.timerListener = new DelegatingTimerListener(timerTask);
	}

	/**
	 * Set the TimerListener to schedule.
	 */
	public void setTimerListener(@Nullable TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * Return the TimerListener to schedule.
	 */
	@Nullable
	public TimerListener getTimerListener() {
		return this.timerListener;
	}

	/**
	 * Set the delay before starting the task for the first time,
	 * in milliseconds. Default is 0, immediately starting the
	 * task after successful scheduling.
	 * <p>If the "firstTime" property is specified, this property will be ignored.
	 * Specify one or the other, not both.
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
	 * <p>Default is -1, leading to one-time execution. In case of zero or a
	 * positive value, the task will be executed repeatedly, with the given
	 * interval in-between executions.
	 * <p>Note that the semantics of the period value vary between fixed-rate
	 * and fixed-delay execution.
	 * <p><b>Note:</b> A period of 0 (for example as fixed delay) <i>is</i>
	 * supported, because the CommonJ specification defines this as a legal value.
	 * Hence a value of 0 will result in immediate re-execution after a job has
	 * finished (not in one-time execution like with {@code java.util.Timer}).
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
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
		return (this.period < 0);
	}

	/**
	 * Set whether to schedule as fixed-rate execution, rather than
	 * fixed-delay execution. Default is "false", i.e. fixed delay.
	 * <p>See TimerManager javadoc for details on those execution modes.
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
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
