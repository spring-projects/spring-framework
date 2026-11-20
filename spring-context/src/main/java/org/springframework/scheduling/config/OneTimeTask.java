/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Duration;

/**
 * {@link Task} implementation defining a {@code Runnable} with an initial delay.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see ScheduledTaskRegistrar#addOneTimeTask(DelayedTask)
 */
public class OneTimeTask extends DelayedTask {

	/**
	 * Create a new {@code DelayedTask}.
	 * @param runnable the underlying task to execute
	 * @param initialDelay the initial delay before execution of the task
	 */
	public OneTimeTask(Runnable runnable, Duration initialDelay) {
		super(runnable, initialDelay);
	}

	OneTimeTask(DelayedTask task) {
		super(task);
	}

}
