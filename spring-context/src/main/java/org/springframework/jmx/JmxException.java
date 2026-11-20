/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx;

import org.springframework.core.NestedRuntimeException;

/**
 * General base exception to be thrown on JMX errors.
 * Unchecked since JMX failures are usually fatal.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class JmxException extends NestedRuntimeException {

	/**
	 * Constructor for JmxException.
	 * @param msg the detail message
	 */
	public JmxException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for JmxException.
	 * @param msg the detail message
	 * @param cause the root cause (usually a raw JMX API exception)
	 */
	public JmxException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
