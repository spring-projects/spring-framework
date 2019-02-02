/*
 * Copyright 2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Composite {@link TaskDecorator}.
 *
 * <p>Delegate given {@link Runnable} to the list of {@link TaskDecorator TaskDecorators}
 * for decoration.
 *
 * @author Tadaya Tsuyukubo
 * @since 5.2.0
 */
public class CompositeTaskDecorator implements TaskDecorator {

	private final List<TaskDecorator> taskDecorators = new ArrayList<>();

	public CompositeTaskDecorator(@Nullable Collection<? extends TaskDecorator> taskDecorators) {
		if (taskDecorators != null) {
			this.taskDecorators.addAll(taskDecorators);
		}
	}

	public CompositeTaskDecorator(TaskDecorator... taskDecorators) {
		Collections.addAll(this.taskDecorators, taskDecorators);
	}

	public boolean add(TaskDecorator taskDecorator) {
		Assert.notNull(taskDecorator, "taskDecorator must not be null");
		return this.taskDecorators.add(taskDecorator);
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		Runnable toDecorate = runnable;
		for (TaskDecorator taskDecorator : this.taskDecorators) {
			toDecorate = taskDecorator.decorate(toDecorate);
		}
		return toDecorate;
	}

}
