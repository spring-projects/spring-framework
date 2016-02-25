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

package org.springframework.beans.factory;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;

/**
 * A specialization of {@link UnsatisfiedDependencyException} that is thrown
 * when a bean constructor has an unsatisfied dependency.
 * 
 * @author Andy Wilkinson
 * @since 4.3.0
 */
@SuppressWarnings("serial")
public class UnsatisfiedConstructorDependencyException extends UnsatisfiedDependencyException {

	private final Constructor<?> constructor;

	private final Class<?> argumentType;

	private final int argumentIndex;

	/**
	 * Create a new UnsatisfiedConstructorDependencyException.
	 * @param constructor the constructor with the unsatisfied dependency
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param argIndex the index of the constructor argument that couldn't be satisfied
	 * @param argType the type of the constructor argument that couldn't be satisfied
	 * @param cause the cause
	 */
	public UnsatisfiedConstructorDependencyException(
			Constructor<?> constructor, String resourceDescription, String beanName, int argIndex, Class<?> argType, BeansException cause) {
		super(resourceDescription, beanName, argIndex, argType, cause);
		this.constructor = constructor;
		this.argumentIndex = argIndex;
		this.argumentType = argType;
	}

	/**
	 * Create a new UnsatisfiedConstructorDependencyException.
	 * @param constructor the constructor with the unsatisfied dependency
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param argIndex the index of the constructor argument that couldn't be satisfied
	 * @param argType the type of the constructor argument that couldn't be satisfied
	 * @param msg the detail message
	 */
	public UnsatisfiedConstructorDependencyException(
			Constructor<?> constructor, String resourceDescription, String beanName, int argIndex, Class<?> argType, String msg) {
		super(resourceDescription, beanName, argIndex, argType, msg);
		this.constructor = constructor;
		this.argumentType = argType;
		this.argumentIndex = argIndex;
	}

	/**
	 * Return the constructor with the dependency that couldn't be satisfied.
	 */
	public Constructor<?> getConstructor() {
		return constructor;
	}

	/**
	 * Return the type of the argument the couldn't be satisfied.
	 */
	public Class<?> getArgumentType() {
		return argumentType;
	}

	/**
	 * Return the index of the argument that couldn't be satisfied.
	 * @return the parameterIndex
	 */
	public int getArgumentIndex() {
		return argumentIndex;
	}

}
