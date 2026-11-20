/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import org.springframework.scheduling.support.CronTrigger;

/**
 * {@link TriggerTask} implementation defining a {@code Runnable} to be executed according
 * to a {@linkplain org.springframework.scheduling.support.CronExpression#parse(String)
 * standard cron expression}.
 *
 * @author Chris Beams
 * @since 3.2
 * @see org.springframework.scheduling.annotation.Scheduled#cron()
 * @see ScheduledTaskRegistrar#addCronTask(CronTask)
 */
public class CronTask extends TriggerTask {

	private final String expression;


	/**
	 * Create a new {@code CronTask}.
	 * @param runnable the underlying task to execute
	 * @param expression the cron expression defining when the task should be executed
	 */
	public CronTask(Runnable runnable, String expression) {
		this(runnable, new CronTrigger(expression));
	}

	/**
	 * Create a new {@code CronTask}.
	 * @param runnable the underlying task to execute
	 * @param cronTrigger the cron trigger defining when the task should be executed
	 */
	public CronTask(Runnable runnable, CronTrigger cronTrigger) {
		super(runnable, cronTrigger);
		this.expression = cronTrigger.getExpression();
	}


	/**
	 * Return the cron expression defining when the task should be executed.
	 */
	public String getExpression() {
		return this.expression;
	}

}
