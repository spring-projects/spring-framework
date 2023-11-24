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

package org.springframework.core.task.support;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} that {@link ContextSnapshot#wrap(Runnable) wraps the execution}
 * of tasks, assisting with context propagation.
 *
 * <p>This operation is only useful when the task execution is scheduled on a different
 * thread than the original call stack; this depends on the choice of
 * {@link org.springframework.core.task.TaskExecutor}. This is particularly useful for
 * restoring a logging context or an observation context for the task execution. Note that
 * this decorator will cause some overhead for task execution and is not recommended for
 * applications that run lots of very small tasks.
 *
 * @author Brian Clozel
 * @since 6.1
 * @see CompositeTaskDecorator
 */
public class ContextPropagatingTaskDecorator implements TaskDecorator {

	private final ContextSnapshotFactory factory;


	/**
	 * Create a new decorator that uses a default instance of the {@link ContextSnapshotFactory}.
	 */
	public ContextPropagatingTaskDecorator() {
		this(ContextSnapshotFactory.builder().build());
	}

	/**
	 * Create a new decorator using the given {@link ContextSnapshotFactory}.
	 * @param factory the context snapshot factory to use.
	 */
	public ContextPropagatingTaskDecorator(ContextSnapshotFactory factory) {
		this.factory = factory;
	}


	@Override
	public Runnable decorate(Runnable runnable) {
		return this.factory.captureAll().wrap(runnable);
	}

}
