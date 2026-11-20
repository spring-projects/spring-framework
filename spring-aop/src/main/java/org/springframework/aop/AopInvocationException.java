/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception that gets thrown when an AOP invocation failed
 * because of misconfiguration or unexpected runtime issues.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AopInvocationException extends NestedRuntimeException {

	/**
	 * Constructor for AopInvocationException.
	 * @param msg the detail message
	 */
	public AopInvocationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for AopInvocationException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public AopInvocationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
