/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * Thrown if an exception is encountered when trying to retrieve
 * MBean metadata.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 * @see MBeanProxyFactoryBean
 */
@SuppressWarnings("serial")
public class MBeanInfoRetrievalException extends JmxException {

	/**
	 * Create a new {@code MBeanInfoRetrievalException} with the
	 * specified error message.
	 * @param msg the detail message
	 */
	public MBeanInfoRetrievalException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code MBeanInfoRetrievalException} with the
	 * specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public MBeanInfoRetrievalException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
