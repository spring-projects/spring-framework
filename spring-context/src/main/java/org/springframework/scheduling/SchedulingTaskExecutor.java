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
