/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Duration;

import org.springframework.util.Assert;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed at a given
 * millisecond interval which may be treated as fixed-rate or fixed-delay depending on
 * context.
 *
 * @author Chris Beams
 * @author Arjen Poutsma
 * @since 3.2
 * @see ScheduledTaskRegistrar#addFixedRateTask(IntervalTask)
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 */
public class IntervalTask extends DelayedTask {

	private final Duration interval;


	/**
	 * Create a new {@code IntervalTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often in milliseconds the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 * @deprecated as of 6.0, in favor on {@link #IntervalTask(Runnable, Duration, Duration)}
	 */
	@Deprecated(since = "6.0")
	public IntervalTask(Runnable runnable, long interval, long initialDelay) {
		this(runnable, Duration.ofMillis(interval), Duration.ofMillis(initialDelay));
	}

	/**
	 * Create a new {@code IntervalTask} with no initial delay.
	 * @param runnable the underlying task to execute
	 * @param interval how often in milliseconds the task should be executed
	 * @deprecated as of 6.0, in favor on {@link #IntervalTask(Runnable, Duration)}
	 */
	@Deprecated(since = "6.0")
	public IntervalTask(Runnable runnable, long interval) {
		this(runnable, Duration.ofMillis(interval), Duration.ZERO);
	}

	/**
	 * Create a new {@code IntervalTask} with no initial delay.
	 * @param runnable the underlying task to execute
	 * @param interval how often the task should be executed
	 * @since 6.0
	 */
	public IntervalTask(Runnable runnable, Duration interval) {
		this(runnable, interval, Duration.ZERO);
	}

	/**
	 * Create a new {@code IntervalTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 * @since 6.0
	 */
	public IntervalTask(Runnable runnable, Duration interval, Duration initialDelay) {
		super(runnable, initialDelay);
		Assert.notNull(interval, "Interval must not be null");
		this.interval = interval;
	}

	/**
	 * Copy constructor.
	 */
	IntervalTask(IntervalTask task) {
		super(task);
		this.interval = task.getIntervalDuration();
	}


	/**
	 * Return how often in milliseconds the task should be executed.
	 * @deprecated as of 6.0, in favor of {@link #getIntervalDuration()}
	 */
	@Deprecated(since = "6.0")
	public long getInterval() {
		return this.interval.toMillis();
	}

	/**
	 * Return how often the task should be executed.
	 * @since 6.0
	 */
	public Duration getIntervalDuration() {
		return this.interval;
	}

	/**
	 * Return the initial delay before first execution of the task.
	 * @deprecated as of 6.0, in favor of {@link #getInitialDelayDuration()}
	 */
	@Deprecated(since = "6.0")
	public long getInitialDelay() {
		return getInitialDelayDuration().toMillis();
	}

	/**
	 * Return the initial delay before first execution of the task.
	 * @since 6.0
	 */
	@Override
	public Duration getInitialDelayDuration() {
		return super.getInitialDelayDuration();
	}

}
