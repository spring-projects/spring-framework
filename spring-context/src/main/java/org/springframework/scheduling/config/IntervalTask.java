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

package org.springframework.scheduling.config;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed at a given
 * millisecond interval which may be treated as fixed-rate or fixed-delay depending on
 * context.
 *
 * @author Chris Beams
 * @since 3.2
 * @see ScheduledTaskRegistrar#addFixedRateTask(IntervalTask)
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 */
public class IntervalTask extends Task {

	private final long interval;

	private final long initialDelay;


	/**
	 * Create a new {@code IntervalTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often in milliseconds the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 */
	public IntervalTask(Runnable runnable, long interval, long initialDelay) {
		super(runnable);
		this.interval = interval;
		this.initialDelay = initialDelay;
	}

	/**
	 * Create a new {@code IntervalTask} with no initial delay.
	 * @param runnable the underlying task to execute
	 * @param interval how often in milliseconds the task should be executed
	 */
	public IntervalTask(Runnable runnable, long interval) {
		this(runnable, interval, 0);
	}


	/**
	 * Return how often in milliseconds the task should be executed.
	 */
	public long getInterval() {
		return this.interval;
	}

	/**
	 * Return the initial delay before first execution of the task.
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

}
