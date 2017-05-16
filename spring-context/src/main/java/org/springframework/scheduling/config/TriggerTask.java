/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.scheduling.Trigger;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed
 * according to a given {@link Trigger}.
 *
 * @author Chris Beams
 * @since 3.2
 * @see Trigger#nextExecutionTime(org.springframework.scheduling.TriggerContext)
 * @see ScheduledTaskRegistrar#setTriggerTasksList(java.util.List)
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
		this.trigger = trigger;
	}


	public Trigger getTrigger() {
		return this.trigger;
	}

}
