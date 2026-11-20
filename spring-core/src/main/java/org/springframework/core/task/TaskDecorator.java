/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

/**
 * A callback interface for a decorator to be applied to any {@link Runnable}
 * about to be executed.
 *
 * <p>Note that such a decorator is not necessarily being applied to the
 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
 * execution callback (which may be a wrapper around the user-supplied task).
 *
 * <p>The primary use case is to set some execution context around the task's
 * invocation, or to provide some monitoring/statistics for task execution.
 *
 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
 * may be limited. Specifically in case of a {@code Future}-based operation,
 * the exposed {@code Runnable} will be a wrapper which does not propagate
 * any exceptions from its {@code run} method.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see TaskExecutor#execute(Runnable)
 * @see SimpleAsyncTaskExecutor#setTaskDecorator
 * @see org.springframework.core.task.support.TaskExecutorAdapter#setTaskDecorator
 */
@FunctionalInterface
public interface TaskDecorator {

	/**
	 * Decorate the given {@code Runnable}, returning a potentially wrapped
	 * {@code Runnable} for actual execution, internally delegating to the
	 * original {@link Runnable#run()} implementation.
	 * @param runnable the original {@code Runnable}
	 * @return the decorated {@code Runnable}
	 */
	Runnable decorate(Runnable runnable);

}
