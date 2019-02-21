/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jndi;

import javax.naming.NamingException;

/**
 * Exception thrown if a type mismatch is encountered for an object
 * located in a JNDI environment. Thrown by JndiTemplate.
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 * @see JndiTemplate#lookup(String, Class)
 */
@SuppressWarnings("serial")
public class TypeMismatchNamingException extends NamingException {

	private final Class<?> requiredType;

	private final Class<?> actualType;


	/**
	 * Construct a new TypeMismatchNamingException,
	 * building an explanation text from the given arguments.
	 * @param jndiName the JNDI name
	 * @param requiredType the required type for the lookup
	 * @param actualType the actual type that the lookup returned
	 */
	public TypeMismatchNamingException(String jndiName, Class<?> requiredType, Class<?> actualType) {
		super("Object of type [" + actualType + "] available at JNDI location [" +
				jndiName + "] is not assignable to [" + requiredType.getName() + "]");
		this.requiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * Return the required type for the lookup, if available.
	 */
	public final Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * Return the actual type that the lookup returned, if available.
	 */
	public final Class<?> getActualType() {
		return this.actualType;
	}

}
