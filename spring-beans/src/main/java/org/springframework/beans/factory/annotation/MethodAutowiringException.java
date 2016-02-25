/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanCreationException;

/**
 * Exception thrown when a failure occurs when attempting to autowire a method.
 * 
 * @author Andy Wilkinson
 * @since 4.3.0
 */
@SuppressWarnings("serial")
public class MethodAutowiringException extends BeanCreationException {

	private final Method method;

	/**
	 * Create a new MethodAutowiringException.
	 * @param method the method that could not be autowired
	 * @param cause the cause of the failure
	 */
	public MethodAutowiringException(Method method, Throwable cause) {
		super("Could not autowire method: " + method, cause);
		this.method = method;
	}

	/**
	 * Returns the method that could not be autowired.
	 */
	public Method getMethod() {
		return method;
	}

}
