/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
