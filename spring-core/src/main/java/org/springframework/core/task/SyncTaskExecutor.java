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

import java.io.Serializable;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * {@link TaskExecutor} implementation that executes each task <i>synchronously</i>
 * in the calling thread. This can be used for testing purposes but also for
 * bounded execution in a Virtual Threads setup, relying on concurrency throttling
 * as inherited from the base class: see {@link #setConcurrencyLimit} (as of 7.0).
 *
 * <p>Execution in the calling thread does have the advantage of participating
 * in its thread context, for example the thread context class loader or the
 * thread's current transaction association. That said, in many cases,
 * asynchronous execution will be preferable: choose an asynchronous
 * {@code TaskExecutor} instead for such scenarios.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAsyncTaskExecutor
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor extends ConcurrencyThrottleSupport implements TaskExecutor, Serializable {

	/**
	 * Execute the given {@code task} synchronously, through direct
	 * invocation of its {@link Runnable#run() run()} method.
	 * @throws RuntimeException if propagated from the given {@code Runnable}
	 */
	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Task must not be null");
		if (isThrottleActive()) {
			beforeAccess();
			try {
				task.run();
			}
			finally {
				afterAccess();
			}
		}
		else {
			task.run();
		}
	}

	/**
	 * Execute the given {@code task} synchronously, through direct
	 * invocation of its {@link TaskCallback#call() call()} method.
	 * @param <V> the returned value type, if any
	 * @param <E> the exception propagated, if any
	 * @throws E if propagated from the given {@code TaskCallback}
	 * @since 7.0
	 */
	public <V, E extends Exception> V execute(TaskCallback<V, E> task) throws E {
		Assert.notNull(task, "Task must not be null");
		if (isThrottleActive()) {
			beforeAccess();
			try {
				return task.call();
			}
			finally {
				afterAccess();
			}
		}
		else {
			return task.call();
		}
	}

}
