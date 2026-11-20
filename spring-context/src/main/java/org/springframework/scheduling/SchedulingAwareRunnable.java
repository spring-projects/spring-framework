/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling;

import org.jspecify.annotations.Nullable;

/**
 * Extension of the {@link Runnable} interface, adding special callbacks
 * for long-running operations.
 *
 * <p>Scheduling-capable TaskExecutors are encouraged to check a submitted
 * Runnable, detecting whether this interface is implemented and reacting
 * as appropriately as they are able to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.task.TaskExecutor
 * @see SchedulingTaskExecutor
 */
public interface SchedulingAwareRunnable extends Runnable {

	/**
	 * Return whether the Runnable's operation is long-lived
	 * ({@code true}) versus short-lived ({@code false}).
	 * <p>In the former case, the task will not allocate a thread from the thread
	 * pool (if any) but rather be considered as long-running background thread.
	 * <p>This should be considered a hint. Of course TaskExecutor implementations
	 * are free to ignore this flag and the SchedulingAwareRunnable interface overall.
	 * <p>The default implementation returns {@code false}, as of 6.1.
	 */
	default boolean isLongLived() {
		return false;
	}

	/**
	 * Return a qualifier associated with this Runnable.
	 * <p>The default implementation returns {@code null}.
	 * <p>May be used for custom purposes depending on the scheduler implementation.
	 * {@link org.springframework.scheduling.config.TaskSchedulerRouter} introspects
	 * this qualifier in order to determine the target scheduler to be used
	 * for a given Runnable, matching the qualifier value (or the bean name)
	 * of a specific {@link org.springframework.scheduling.TaskScheduler} or
	 * {@link java.util.concurrent.ScheduledExecutorService} bean definition.
	 * @since 6.1
	 * @see org.springframework.scheduling.annotation.Scheduled#scheduler()
	 */
	default @Nullable String getQualifier() {
		return null;
	}

}
