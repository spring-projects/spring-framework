/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Composite {@link TaskDecorator} that delegates to other task decorators.
 *
 * @author Tadaya Tsuyukubo
 * @since 5.2
 */
public class CompositeTaskDecorator implements TaskDecorator {

	private final List<TaskDecorator> taskDecorators = new ArrayList<>();

	/**
	 * Create a new {@link CompositeTaskDecorator}.
	 *
	 * @param taskDecorators the taskDecorators to delegate to
	 * @throws IllegalArgumentException if {@code taskDecorators} is {@code null}
	 */
	public CompositeTaskDecorator(Collection<? extends TaskDecorator> taskDecorators) {
		Assert.notNull(taskDecorators, "taskDecorators must not be null");
		this.taskDecorators.addAll(taskDecorators);
	}

	/**
	 * Create a new {@link CompositeTaskDecorator}.
	 *
	 * @param taskDecorators the taskDecorators to delegate to
	 * @throws IllegalArgumentException if {@code taskDecorators} is {@code null}
	 */
	public CompositeTaskDecorator(TaskDecorator... taskDecorators) {
		Assert.notNull(taskDecorators, "taskDecorators must not be null");
		this.taskDecorators.addAll(Arrays.asList(taskDecorators));
	}

	/**
	 * Add a delegating {@link TaskDecorator}.
	 *
	 * @param taskDecorator a taskDecorator to delegate to
	 * @return {@code true} when the taskDecorator is successfully added
	 * @throws IllegalArgumentException if {@code taskDecorator} is {@code null}
	 */
	public boolean add(TaskDecorator taskDecorator) {
		Assert.notNull(taskDecorator, "taskDecorator must not be null");
		return this.taskDecorators.add(taskDecorator);
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		Assert.notNull(runnable, "runnable must not be null");

		for (TaskDecorator taskDecorator : this.taskDecorators) {
			runnable = taskDecorator.decorate(runnable);
		}
		return runnable;
	}

}
