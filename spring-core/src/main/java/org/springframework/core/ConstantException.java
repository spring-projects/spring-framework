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

package org.springframework.core;

/**
 * Exception thrown when the {@link Constants} class is asked for
 * an invalid constant name.
 *
 * @author Rod Johnson
 * @since 28.04.2003
 * @see org.springframework.core.Constants
 */
@SuppressWarnings("serial")
public class ConstantException extends IllegalArgumentException {

	/**
	 * Thrown when an invalid constant name is requested.
	 * @param className name of the class containing the constant definitions
	 * @param field invalid constant name
	 * @param message description of the problem
	 */
	public ConstantException(String className, String field, String message) {
		super("Field '" + field + "' " + message + " in class [" + className + "]");
	}

	/**
	 * Thrown when an invalid constant value is looked up.
	 * @param className name of the class containing the constant definitions
	 * @param namePrefix prefix of the searched constant names
	 * @param value the looked up constant value
	 */
	public ConstantException(String className, String namePrefix, Object value) {
		super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
	}

}
