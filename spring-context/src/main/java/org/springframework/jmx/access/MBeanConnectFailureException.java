/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * Thrown when an invocation failed because of an I/O problem on the
 * MBeanServerConnection.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see MBeanClientInterceptor
 */
@SuppressWarnings("serial")
public class MBeanConnectFailureException extends JmxException {

	/**
	 * Create a new {@code MBeanConnectFailureException}
	 * with the specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public MBeanConnectFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
