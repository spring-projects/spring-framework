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

package org.springframework.beans;

import java.beans.PropertyChangeEvent;

/**
 * Thrown when a bean property getter or setter method throws an exception,
 * analogous to an InvocationTargetException.
 *
 * @author Rod Johnson
 */
public class MethodInvocationException extends PropertyAccessException {

	/**
	 * Error code that a method invocation error will be registered with.
	 */
	public static final String ERROR_CODE = "methodInvocation";


	/**
	 * Create a new MethodInvocationException.
	 * @param propertyChangeEvent PropertyChangeEvent that resulted in an exception
	 * @param cause the Throwable raised by the invoked method
	 */
	public MethodInvocationException(PropertyChangeEvent propertyChangeEvent, Throwable cause) {
		super(propertyChangeEvent, "Property '" + propertyChangeEvent.getPropertyName() + "' threw exception", cause);
	}

	public String getErrorCode() {
		return ERROR_CODE;
	}

}
