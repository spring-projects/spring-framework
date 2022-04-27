/*
 * Copyright 2002-2021 the original author or authors.
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
