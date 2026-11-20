/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.aopalliance.aop;

/**
 * Superclass for all AOP infrastructure exceptions.
 * Unchecked, as such exceptions are fatal and end user
 * code shouldn't be forced to catch them.
 *
 * @author Rod Johnson
 * @author Bob Lee
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class AspectException extends RuntimeException {

	/**
	 * Constructor for AspectException.
	 * @param message the exception message
	 */
	public AspectException(String message) {
		super(message);
	}

	/**
	 * Constructor for AspectException.
	 * @param message the exception message
	 * @param cause the root cause, if any
	 */
	public AspectException(String message, Throwable cause) {
		super(message, cause);
	}

}
