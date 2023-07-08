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

package org.springframework.scheduling.support;

import java.lang.reflect.Method;

import io.micrometer.observation.Observation;

import org.springframework.util.ClassUtils;

/**
 * Context that holds information for observation metadata collection during the
 * {@link ScheduledTaskObservationDocumentation#TASKS_SCHEDULED_EXECUTION execution of scheduled tasks}.
 *
 * @author Brian Clozel
 * @since 6.1
 */
public class ScheduledTaskObservationContext extends Observation.Context {

	private final Class<?> targetClass;

	private final Method method;

	private boolean complete;


	/**
	 * Create a new observation context for a task, given the target object
	 * and the method to be called.
	 * @param target the target object that is called for task execution
	 * @param method the method that is called for task execution
	 */
	public ScheduledTaskObservationContext(Object target, Method method) {
		this.targetClass = ClassUtils.getUserClass(target);
		this.method = method;
	}


	/**
	 * Return the type of the target object.
	 */
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * Return the method that is called for task execution.
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return whether the task execution is complete.
	 * <p>If an observation has ended and the task is not complete, this means
	 * that an {@link #getError() error} was raised or that the task execution got cancelled
	 * during its execution.
	 */
	public boolean isComplete() {
		return this.complete;
	}

	/**
	 * Set whether the task execution has completed.
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

}
