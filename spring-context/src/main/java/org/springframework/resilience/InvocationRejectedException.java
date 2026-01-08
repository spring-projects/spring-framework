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

package org.springframework.resilience;

import java.util.concurrent.RejectedExecutionException;

/**
 * Exception thrown when a target will not get invoked due to a resilience policy,
 * such as the concurrency limit having been reached for a class/method annotated with
 * {@link org.springframework.resilience.annotation.ConcurrencyLimit @ConcurrencyLimit}.
 *
 * <p>Extends {@link RejectedExecutionException} as a common base class
 * with {@link org.springframework.core.task.TaskRejectedException},
 * allowing for custom catch blocks to cover both Spring scenarios and
 * {@link java.util.concurrent.ExecutorService} rejection exceptions.
 *
 * @author Juergen Hoeller
 * @since 7.0.3
 * @see org.springframework.resilience.annotation.ConcurrencyLimit.ThrottlePolicy#REJECT
 * @see org.springframework.core.task.TaskRejectedException
 */
@SuppressWarnings("serial")
public class InvocationRejectedException extends RejectedExecutionException {

	private final Object target;


	/**
	 * Create a new {@code InvocationRejectedException}
	 * with the specified detail message and target instance.
	 * @param msg the detail message
	 * @param target the target instance that was about to be invoked
	 */
	public InvocationRejectedException(String msg, Object target) {
		super(msg);
		this.target = target;
	}


	/**
	 * Return the target instance that was about to be invoked.
	 */
	public Object getTarget() {
		return this.target;
	}

}
