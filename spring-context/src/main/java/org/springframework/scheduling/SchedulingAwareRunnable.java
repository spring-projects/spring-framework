/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Extension of the Runnable interface, adding special callbacks
 * for long-running operations.
 *
 * <p>This interface closely corresponds to the CommonJ Work interface,
 * but is kept separate to avoid a required CommonJ dependency.
 *
 * <p>Scheduling-capable TaskExecutors are encouraged to check a submitted
 * Runnable, detecting whether this interface is implemented and reacting
 * as appropriately as they are able to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see commonj.work.Work
 * @see org.springframework.core.task.TaskExecutor
 * @see SchedulingTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingAwareRunnable extends Runnable {

	/**
	 * Return whether the Runnable's operation is long-lived
	 * ({@code true}) versus short-lived ({@code false}).
	 * <p>In the former case, the task will not allocate a thread from the thread
	 * pool (if any) but rather be considered as long-running background thread.
	 * <p>This should be considered a hint. Of course TaskExecutor implementations
	 * are free to ignore this flag and the SchedulingAwareRunnable interface overall.
	 */
	boolean isLongLived();

}
