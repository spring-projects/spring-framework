/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

import javax.servlet.ServletException;

import org.springframework.core.NestedExceptionUtils;

/**
 * Subclass of {@link ServletException} that properly handles a root cause in terms
 * of message and stacktrace, just like NestedChecked/RuntimeException does.
 *
 * <p>Note that the plain ServletException doesn't expose its root cause at all,
 * neither in the exception message nor in printed stack traces! While this might
 * be fixed in later Servlet API variants (which even differ per vendor for the
 * same API version), it is not reliably available on Servlet 2.4 (the minimum
 * version required by Spring 3.x), which is why we need to do it ourselves.
 *
 * <p>The similarity between this class and the NestedChecked/RuntimeException
 * class is unavoidable, as this class needs to derive from ServletException.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #getMessage
 * @see #printStackTrace
 * @see org.springframework.core.NestedCheckedException
 * @see org.springframework.core.NestedRuntimeException
 */
public class NestedServletException extends ServletException {

	/** Use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -5292377985529381145L;

	static {
		// Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
		// issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	/**
	 * Construct a {@code NestedServletException} with the specified detail message.
	 * @param msg the detail message
	 */
	public NestedServletException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code NestedServletException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public NestedServletException(String msg, Throwable cause) {
		super(msg, cause);
		// Set JDK 1.4 exception chain cause if not done by ServletException class already
		// (this differs between Servlet API versions).
		if (getCause() == null && cause!=null) {
			initCause(cause);
		}
	}


	/**
	 * Return the detail message, including the message from the nested exception
	 * if there is one.
	 */
	@Override
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
