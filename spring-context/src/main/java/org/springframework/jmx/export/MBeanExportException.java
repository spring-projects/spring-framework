/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export;

import org.springframework.jmx.JmxException;

/**
 * Exception thrown in case of failure when exporting an MBean.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see MBeanExportOperations
 */
@SuppressWarnings("serial")
public class MBeanExportException extends JmxException {

	/**
	 * Create a new {@code MBeanExportException} with the
	 * specified error message.
	 * @param msg the detail message
	 */
	public MBeanExportException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code MBeanExportException} with the
	 * specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public MBeanExportException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
