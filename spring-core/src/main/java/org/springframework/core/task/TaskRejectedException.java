/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link TaskExecutor} rejects to accept
 * a given task for execution.
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see TaskExecutor#execute(Runnable)
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

	/**
	 * Create a new {@code TaskRejectedException}
	 * with the specified detail message and no root cause.
	 * @param msg the detail message
	 */
	public TaskRejectedException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code TaskRejectedException}
	 * with the specified detail message and the given root cause.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * API such as the {@code java.util.concurrent} package)
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskRejectedException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Create a new {@code TaskRejectedException}
	 * with a default message for the given executor and task.
	 * @param executor the {@code Executor} that rejected the task
	 * @param task the task object that got rejected
	 * @param cause the original {@link RejectedExecutionException}
	 * @since 6.1
	 * @see ExecutorService#isShutdown()
	 * @see java.util.concurrent.RejectedExecutionException
	 */
	public TaskRejectedException(Executor executor, Object task, RejectedExecutionException cause) {
		super(executorDescription(executor) + " did not accept task: " + task, cause);
	}


	private static String executorDescription(Executor executor) {
		if (executor instanceof ExecutorService executorService) {
			try {
				return "ExecutorService in " + (executorService.isShutdown() ? "shutdown" : "active") + " state";
			}
			catch (Exception ex) {
				// UnsupportedOperationException/IllegalStateException from ManagedExecutorService.isShutdown()
				// Falling back to toString() below.
			}
		}
		return executor.toString();
	}

}
