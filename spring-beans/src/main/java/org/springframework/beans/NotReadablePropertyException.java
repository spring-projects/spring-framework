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

/**
 * Exception thrown on an attempt to get the value of a property
 * that isn't readable, because there's no getter method.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
@SuppressWarnings("serial")
public class NotReadablePropertyException extends InvalidPropertyException {

	/**
	 * Create a new NotReadablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 */
	public NotReadablePropertyException(Class beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' is not readable or has an invalid getter method: " +
				"Does the return type of the getter match the parameter type of the setter?");
	}

	/**
	 * Create a new NotReadablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 * @param msg the detail message
	 */
	public NotReadablePropertyException(Class beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

}
