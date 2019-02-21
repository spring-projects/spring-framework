/*
 * Copyright 2002-2018 the original author or authors.
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
 * <p>Note: {@link SchedulingTaskExecutor} implementations are encouraged to also
 * implement the {@link org.springframework.core.task.AsyncListenableTaskExecutor}
 * interface. This is not required due to the dependency on Spring 4.0's new
 * {@link org.springframework.util.concurrent.ListenableFuture} interface,
 * which would make it impossible for third-party executor implementations
 * to remain compatible with both Spring 4.0 and Spring 3.x.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulingAwareRunnable
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

	/**
	 * Does this {@code TaskExecutor} prefer short-lived tasks over long-lived tasks?
	 * <p>A {@code SchedulingTaskExecutor} implementation can indicate whether it
	 * prefers submitted tasks to perform as little work as they can within a single
	 * task execution. For example, submitted tasks might break a repeated loop into
	 * individual subtasks which submit a follow-up task afterwards (if feasible).
	 * <p>This should be considered a hint. Of course {@code TaskExecutor} clients
	 * are free to ignore this flag and hence the {@code SchedulingTaskExecutor}
	 * interface overall. However, thread pools will usually indicated a preference
	 * for short-lived tasks, allowing for more fine-grained scheduling.
	 * @return {@code true} if this executor prefers short-lived tasks (the default),
	 * {@code false} otherwise (for treatment like a regular {@code TaskExecutor})
	 */
	default boolean prefersShortLivedTasks() {
		return true;
	}

}
