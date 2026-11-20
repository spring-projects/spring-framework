/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Duration;

/**
 * Specialization of {@link IntervalTask} for fixed-rate semantics.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 5.0.2
 * @see org.springframework.scheduling.annotation.Scheduled#fixedRate()
 * @see ScheduledTaskRegistrar#addFixedRateTask(IntervalTask)
 */
public class FixedRateTask extends IntervalTask {

	/**
	 * Create a new {@code FixedRateTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often in milliseconds the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 * @deprecated as of 6.0, in favor on {@link #FixedRateTask(Runnable, Duration, Duration)}
	 */
	@Deprecated(since = "6.0")
	public FixedRateTask(Runnable runnable, long interval, long initialDelay) {
		super(runnable, interval, initialDelay);
	}

	/**
	 * Create a new {@code FixedRateTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 * @since 6.0
	 */
	public FixedRateTask(Runnable runnable, Duration interval, Duration initialDelay) {
		super(runnable, interval, initialDelay);
	}

	FixedRateTask(IntervalTask task) {
		super(task);
	}

}
