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

import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * A representation of a scheduled task at runtime,
 * used as a return value for scheduling methods.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see ScheduledTaskRegistrar#scheduleCronTask(CronTask)
 * @see ScheduledTaskRegistrar#scheduleFixedRateTask(FixedRateTask)
 * @see ScheduledTaskRegistrar#scheduleFixedDelayTask(FixedDelayTask)
 */
public final class ScheduledTask {

	private final Task task;

	@Nullable
	volatile ScheduledFuture<?> future;


	ScheduledTask(Task task) {
		this.task = task;
	}


	/**
	 * Return the underlying task (typically a {@link CronTask},
	 * {@link FixedRateTask} or {@link FixedDelayTask}).
	 * @since 5.0.2
	 */
	public Task getTask() {
		return this.task;
	}

	/**
	 * Trigger cancellation of this scheduled task.
	 */
	public void cancel() {
		ScheduledFuture<?> future = this.future;
		if (future != null) {
			future.cancel(true);
		}
	}

	@Override
	public String toString() {
		return this.task.toString();
	}

}
