/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.core.task;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering an overloaded {@link #execute(Runnable, long)} variant with
 * start timeout parameter.
 *
 * <p>Implementing this interface also indicates that the {@link #execute(Runnable)}
 * method will not execute its Runnable in the caller's thread but rather
 * asynchronously in some other thread (at least usually).
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/** Constant that indicates immediate execution */
	long TIMEOUT_IMMEDIATE = 0;

	/** Constant that indicates no time limit */
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * Execute the given <code>task</code>.
	 * @param task the <code>Runnable</code> to execute (never <code>null</code>)
	 * @param startTimeout the time duration within which the task is supposed to start.
	 * This is intended as a hint to the executor, allowing for preferred handling
	 * of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE} or
	 * {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
	 * @throws TaskTimeoutException in case of the task being rejected because
	 * of the timeout (i.e. it cannot be started in time)
	 * @throws TaskRejectedException if the given task was not accepted
	 */
	void execute(Runnable task, long startTimeout);

}
