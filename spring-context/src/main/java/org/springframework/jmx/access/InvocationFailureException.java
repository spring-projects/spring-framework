/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * Thrown when an invocation on an MBean resource failed with an exception (either
 * a reflection exception or an exception thrown by the target method itself).
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 */
@SuppressWarnings("serial")
public class InvocationFailureException extends JmxException {

	/**
	 * Create a new {@code InvocationFailureException} with the supplied
	 * error message.
	 * @param msg the detail message
	 */
	public InvocationFailureException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code InvocationFailureException} with the
	 * specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public InvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
