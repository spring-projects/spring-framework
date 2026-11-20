/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling;

import org.springframework.core.NestedRuntimeException;

/**
 * General exception to be thrown on scheduling failures,
 * such as the scheduler already having shut down.
 * Unchecked since scheduling failures are usually fatal.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class SchedulingException extends NestedRuntimeException {

	/**
	 * Constructor for SchedulingException.
	 * @param msg the detail message
	 */
	public SchedulingException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for SchedulingException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * scheduling API such as Quartz)
	 */
	public SchedulingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
