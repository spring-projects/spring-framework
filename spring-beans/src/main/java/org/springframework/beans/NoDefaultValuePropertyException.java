/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans;

/**
 * Exception is thrown on attempted to bind a default value of null to a target parameter.
 *
 * @author Dillon McMahon
 */
@SuppressWarnings("serial")
public class NoDefaultValuePropertyException extends InvalidPropertyException {


	/**
	 * Create a new NoDefaultValuePropertyException.
	 *
	 * @param beanClass    the offending bean class
	 * @param propertyName the offending property name
	 */
	public NoDefaultValuePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' does not have a default value. Default value must not be null");
	}
}
