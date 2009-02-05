/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.task.support;

import java.util.concurrent.Executor;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Adapter that exposes the {@link java.util.concurrent.Executor} interface
 * for any Spring {@link org.springframework.core.task.TaskExecutor}.
 *
 * <p>This is less useful as of Spring 3.0, since TaskExecutor itself
 * extends the Executor interface. The adapter is only relevant for
 * <em>hiding</em> the TaskExecutor nature of a given object now,
 * solely exposing the standard Executor interface to a client.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.util.concurrent.Executor
 * @see org.springframework.core.task.TaskExecutor
 */
public class ConcurrentExecutorAdapter implements Executor {

	private final TaskExecutor taskExecutor;


	/**
	 * Create a new ConcurrentExecutorAdapter for the given Spring TaskExecutor.
	 * @param taskExecutor the Spring TaskExecutor to wrap
	 */
	public ConcurrentExecutorAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	public void execute(Runnable command) {
		this.taskExecutor.execute(command);
	}

}
