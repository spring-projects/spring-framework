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

import javax.management.JMRuntimeException;

/**
 * Thrown when trying to invoke an operation on a proxy that is not exposed
 * by the proxied MBean resource's management interface.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 */
public class InvalidInvocationException extends JMRuntimeException {

	/**
	 * Create a new <code>InvalidInvocationException</code> with the supplied
	 * error message.
	 * @param msg the detail message
	 */
	public InvalidInvocationException(String msg) {
		super(msg);
	}

}
