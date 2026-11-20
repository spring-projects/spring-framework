/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * A {@link org.springframework.core.task.TaskExecutor} extension exposing
 * scheduling characteristics that are relevant to potential task submitters.
 *
 * <p>Scheduling clients are encouraged to submit
 * {@link Runnable Runnables} that match the exposed preferences
 * of the {@code TaskExecutor} implementation in use.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulingAwareRunnable
 * @see org.springframework.core.task.TaskExecutor
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

	/**
	 * Does this {@code TaskExecutor} prefer short-lived tasks over long-lived tasks?
	 * <p>A {@code SchedulingTaskExecutor} implementation can indicate whether it
	 * prefers submitted tasks to perform as little work as it can within a single
	 * task execution. For example, submitted tasks might break a repeated loop into
	 * individual subtasks which submit a follow-up task afterwards (if feasible).
	 * <p>This should be considered a hint. Of course {@code TaskExecutor} clients
	 * are free to ignore this flag and hence the {@code SchedulingTaskExecutor}
	 * interface overall. However, thread pools will usually indicate a preference
	 * for short-lived tasks, allowing for more fine-grained scheduling.
	 * @return {@code true} if this executor prefers short-lived tasks (the default),
	 * {@code false} otherwise (for treatment like a regular {@code TaskExecutor})
	 */
	default boolean prefersShortLivedTasks() {
		return true;
	}

}
