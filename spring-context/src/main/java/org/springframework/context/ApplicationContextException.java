/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import org.springframework.beans.FatalBeanException;

/**
 * Exception thrown during application context initialization.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class ApplicationContextException extends FatalBeanException {

	/**
	 * Create a new {@code ApplicationContextException}
	 * with the specified detail message and no root cause.
	 * @param msg the detail message
	 */
	public ApplicationContextException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code ApplicationContextException}
	 * with the specified detail message and the given root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public ApplicationContextException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
