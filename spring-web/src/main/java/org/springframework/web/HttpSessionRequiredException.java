/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web;

import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an HTTP request handler requires a pre-existing session.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpSessionRequiredException extends ServletException {

	private final @Nullable String expectedAttribute;


	/**
	 * Create a new HttpSessionRequiredException.
	 * @param msg the detail message
	 */
	public HttpSessionRequiredException(String msg) {
		super(msg);
		this.expectedAttribute = null;
	}

	/**
	 * Create a new HttpSessionRequiredException.
	 * @param msg the detail message
	 * @param expectedAttribute the name of the expected session attribute
	 * @since 4.3
	 */
	public HttpSessionRequiredException(String msg, String expectedAttribute) {
		super(msg);
		this.expectedAttribute = expectedAttribute;
	}


	/**
	 * Return the name of the expected session attribute, if any.
	 * @since 4.3
	 */
	public @Nullable String getExpectedAttribute() {
		return this.expectedAttribute;
	}

}
