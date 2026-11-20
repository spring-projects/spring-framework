/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
