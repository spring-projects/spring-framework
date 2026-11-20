/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm;

/**
 * Exception thrown on unmarshalling failure.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UnmarshallingFailureException extends MarshallingException {

	/**
	 * Construct a {@code MarshallingFailureException} with the specified detail message.
	 * @param msg the detail message
	 */
	public UnmarshallingFailureException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code MarshallingFailureException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public UnmarshallingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
