/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.config;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed after a given
 * millisecond delay.
 *
 * @author Alex Panchenko
 * @since 5.x
 * @see ScheduledTaskRegistrar#addStartupTask(StartupTask)
 */
public class StartupTask extends Task {

	private final long initialDelay;

	/**
	 * Create a new {@code IntervalTask}.
	 * @param runnable the underlying task to execute
	 * @param initialDelay the initial delay before first execution of the task
	 */
	public StartupTask(Runnable runnable, long initialDelay) {
		super(runnable);
		this.initialDelay = initialDelay;
	}

	/**
	 * Return the initial delay before first execution of the task.
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

}
