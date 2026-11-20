/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception that gets thrown on illegal AOP configuration arguments.
 *
 * @author Rod Johnson
 * @since 13.03.2003
 */
@SuppressWarnings("serial")
public class AopConfigException extends NestedRuntimeException {

	/**
	 * Constructor for AopConfigException.
	 * @param msg the detail message
	 */
	public AopConfigException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for AopConfigException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public AopConfigException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
