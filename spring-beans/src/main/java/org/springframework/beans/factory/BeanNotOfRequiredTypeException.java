/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

	/** The name of the instance that was of the wrong type. */
	private final String beanName;

	/** The required type. */
	private final Class<?> requiredType;

	/** The offending type. */
	private final Class<?> actualType;


	/**
	 * Create a new BeanNotOfRequiredTypeException.
	 * @param beanName the name of the bean requested
	 * @param requiredType the required type
	 * @param actualType the actual type returned, which did not match
	 * the expected type
	 */
	public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
		super("Bean named '" + beanName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
				"' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
		this.beanName = beanName;
		this.requiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * Return the name of the instance that was of the wrong type.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the expected type for the bean.
	 */
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * Return the actual type of the instance found.
	 */
	public Class<?> getActualType() {
		return this.actualType;
	}

}
