/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx;

/**
 * Exception thrown when we cannot locate an instance of an {@code MBeanServer},
 * or when more than one instance is found.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jmx.support.JmxUtils#locateMBeanServer
 */
@SuppressWarnings("serial")
public class MBeanServerNotFoundException extends JmxException {

	/**
	 * Create a new {@code MBeanServerNotFoundException} with the
	 * supplied error message.
	 * @param msg the error message
	 */
	public MBeanServerNotFoundException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code MBeanServerNotFoundException} with the
	 * specified error message and root cause.
	 * @param msg the error message
	 * @param cause the root cause
	 */
	public MBeanServerNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
