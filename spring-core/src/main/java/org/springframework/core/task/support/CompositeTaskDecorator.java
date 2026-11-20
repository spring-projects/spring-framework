/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
