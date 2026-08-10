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

import java.lang.reflect.Type;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Yanming Zhou
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

	/** The name of the instance that was of the wrong type. */
	private final String beanName;

	/** The required type. */
	private final Type genericRequiredType;

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
		this(beanName, (Type) requiredType, actualType);
	}

	/**
	 * Create a new BeanNotOfRequiredTypeException.
	 * @param beanName the name of the bean requested
	 * @param requiredType the required type
	 * @param actualType the actual type returned, which did not match
	 * the expected type
	 * @since 7.1
	 */
	public BeanNotOfRequiredTypeException(String beanName, Type requiredType, Class<?> actualType) {
		super("Bean named '" + beanName + "' is expected to be of type '" + requiredType.getTypeName() +
				"' but was actually of type '" + actualType.getTypeName() + "'");
		this.beanName = beanName;
		this.genericRequiredType = requiredType;
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
		return (this.genericRequiredType instanceof Class<?> clazz ? clazz : ResolvableType.forType(this.genericRequiredType).toClass());
	}

	/**
	 * Return the expected generic type for the bean.
	 * @since 7.1
	 */
	public Type getGenericRequiredType() {
		return this.genericRequiredType;
	}

	/**
	 * Return the actual type of the instance found.
	 */
	public Class<?> getActualType() {
		return this.actualType;
	}

}
