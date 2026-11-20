/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a {@link TaskExecutor} rejects to accept
 * a given task for execution.
 *
 * <p>Extends {@link RejectedExecutionException} as a common base class,
 * allowing for custom catch blocks to cover both Spring scenarios and
 * {@link java.util.concurrent.ExecutorService} rejection exceptions.
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
