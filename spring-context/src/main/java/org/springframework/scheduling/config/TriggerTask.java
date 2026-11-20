/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed
 * according to a given {@link Trigger}.
 *
 * @author Chris Beams
 * @since 3.2
 * @see ScheduledTaskRegistrar#addTriggerTask(TriggerTask)
 * @see org.springframework.scheduling.TaskScheduler#schedule(Runnable, Trigger)
 */
public class TriggerTask extends Task {

	private final Trigger trigger;


	/**
	 * Create a new {@link TriggerTask}.
	 * @param runnable the underlying task to execute
	 * @param trigger specifies when the task should be executed
	 */
	public TriggerTask(Runnable runnable, Trigger trigger) {
		super(runnable);
		Assert.notNull(trigger, "Trigger must not be null");
		this.trigger = trigger;
	}


	/**
	 * Return the associated trigger.
	 */
	public Trigger getTrigger() {
		return this.trigger;
	}

}
