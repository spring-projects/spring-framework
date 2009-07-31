/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.web.util.NestedServletException;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 *
 * <p>Extends ServletException for convenient throwing in any Servlet resource
 * (such as a Filter), and NestedServletException for proper root cause handling
 * (as the plain ServletException doesn't expose its root cause at all).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ServletRequestBindingException extends NestedServletException {

	/**
	 * Constructor for ServletRequestBindingException.
	 * @param msg the detail message
	 */
	public ServletRequestBindingException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for ServletRequestBindingException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public ServletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
