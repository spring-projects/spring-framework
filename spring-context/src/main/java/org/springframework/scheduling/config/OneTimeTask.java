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
