/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.beans.factory.access;

import org.springframework.beans.FatalBeanException;

/**
 * Exception thrown if a bean factory could not be loaded by a bootstrap class.
 *
 * @author Rod Johnson
 * @since 02.12.2002
 */
public class BootstrapException extends FatalBeanException {

	/**
	 * Create a new BootstrapException with the specified message.
	 * @param msg the detail message
	 */
	public BootstrapException(String msg) {
		super(msg);
	}

	/**
	 * Create a new BootstrapException with the specified message
	 * and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BootstrapException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
