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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.task.TaskDecorator;
import org.springframework.util.Assert;

/**
 * Composite {@link TaskDecorator} that delegates to other task decorators.
 *
 * @author Tadaya Tsuyukubo
 * @since 6.1
 */
public class CompositeTaskDecorator implements TaskDecorator {

	private final List<TaskDecorator> taskDecorators;

	/**
	 * Create a new instance.
	 * @param taskDecorators the taskDecorators to delegate to
	 */
	public CompositeTaskDecorator(Collection<? extends TaskDecorator> taskDecorators) {
		Assert.notNull(taskDecorators, "TaskDecorators must not be null");
		this.taskDecorators = new ArrayList<>(taskDecorators);
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		Assert.notNull(runnable, "Runnable must not be null");
		for (TaskDecorator taskDecorator : this.taskDecorators) {
			runnable = taskDecorator.decorate(runnable);
		}
		return runnable;
	}

}
