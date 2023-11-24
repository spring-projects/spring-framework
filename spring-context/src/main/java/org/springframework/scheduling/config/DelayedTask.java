/*
 * Copyright 2002-2023 the original author or authors.
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
