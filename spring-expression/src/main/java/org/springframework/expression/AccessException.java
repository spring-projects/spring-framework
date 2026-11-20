/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

/**
 * An AccessException is thrown by an accessor if it has an unexpected problem.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class AccessException extends Exception {

	/**
	 * Create an AccessException with a specific message.
	 * @param message the message
	 */
	public AccessException(String message) {
		super(message);
	}

	/**
	 * Create an AccessException with a specific message and cause.
	 * @param message the message
	 * @param cause the cause
	 */
	public AccessException(String message, Exception cause) {
		super(message, cause);
	}

}
