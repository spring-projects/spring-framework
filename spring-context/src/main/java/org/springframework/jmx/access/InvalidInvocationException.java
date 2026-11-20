/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.access;

import javax.management.JMRuntimeException;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when trying to invoke an operation on a proxy that is not exposed
 * by the proxied MBean resource's management interface.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 */
@SuppressWarnings("serial")
public class InvalidInvocationException extends JMRuntimeException {

	/**
	 * Create a new {@code InvalidInvocationException} with the supplied
	 * error message.
	 * @param msg the detail message
	 */
	public InvalidInvocationException(@Nullable String msg) {
		super(msg);
	}

}
