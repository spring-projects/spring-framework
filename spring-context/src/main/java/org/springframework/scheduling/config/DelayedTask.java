/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Duration;

import org.springframework.util.Assert;

/**
 * {@link Task} implementation defining a {@code Runnable} with an initial delay.
 *
 * @author Juergen Hoeller
 * @since 6.1
 */
public class DelayedTask extends Task {

	private final Duration initialDelay;


	/**
	 * Create a new {@code DelayedTask}.
	 * @param runnable the underlying task to execute
	 * @param initialDelay the initial delay before execution of the task
	 */
	public DelayedTask(Runnable runnable, Duration initialDelay) {
		super(runnable);
		Assert.notNull(initialDelay, "InitialDelay must not be null");
		this.initialDelay = initialDelay;
	}

	/**
	 * Copy constructor.
	 */
	DelayedTask(DelayedTask task) {
		super(task.getRunnable());
		Assert.notNull(task, "DelayedTask must not be null");
		this.initialDelay = task.getInitialDelayDuration();
	}


	/**
	 * Return the initial delay before first execution of the task.
	 */
	public Duration getInitialDelayDuration() {
		return this.initialDelay;
	}

}
