/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

/**
 * Thrown when a URL string cannot be parsed.
 *
 * @author Arjen Poutsma
 * @since 6.2
 */
public class InvalidUrlException extends IllegalArgumentException {

	private static final long serialVersionUID = 7409308391039105562L;


	/**
	 * Construct a {@code InvalidUrlException} with the specified detail message.
	 * @param msg the detail message
	 */
	public InvalidUrlException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code InvalidUrlException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public InvalidUrlException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
