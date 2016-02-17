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

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;

/**
 * A specialization of {@link UnsatisfiedDependencyException} that is thrown
 * when a bean factory method has an unsatisfied dependency.
 * 
 * @author Andy Wilkinson
 * @since 4.3.0
 */
@SuppressWarnings("serial")
public class UnsatisfiedFactoryMethodDependencyException extends UnsatisfiedDependencyException {

	private final Method factoryMethod;

	private final Class<?> argumentType;

	private final int argumentIndex;

	/**
	 * Create a new UnsatisfiedFactoryMethodDependencyException.
	 * @param factoryMethod the factory method with the unsatisfied dependency
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param argIndex the index of the method argument that couldn't be satisfied
	 * @param argType the type of the method argument that couldn't be satisfied
	 * @param msg the detail message
	 */
	public UnsatisfiedFactoryMethodDependencyException(
			Method factoryMethod, String resourceDescription, String beanName, int argIndex, Class<?> argType, String msg) {
		this(factoryMethod, resourceDescription, beanName, argIndex, argType, msg, null);
	}

	/**
	 * Create a new UnsatisfiedFactoryMethodDependencyException.
	 * @param factoryMethod the factory method with the unsatisfied dependency
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param argIndex the index of the method argument that couldn't be satisfied
	 * @param argType the type of the method argument that couldn't be satisfied
	 * @param cause the cause
	 */
	public UnsatisfiedFactoryMethodDependencyException(
			Method factoryMethod, String resourceDescription, String beanName, int argIndex, Class<?> argType, BeansException cause) {
		this(factoryMethod, resourceDescription, beanName, argIndex, argType, null, cause);
	}

	private UnsatisfiedFactoryMethodDependencyException(
			Method factoryMethod, String resourceDescription, String beanName, int argIndex, Class<?> argType, String msg, BeansException cause) {
		super("Unsatisfied dependency expressed through argument with index " +
				argIndex + " of type [" + ClassUtils.getQualifiedName(argType) + "]" +
				(msg != null ? ": " + msg : ""), cause, resourceDescription, beanName);
		this.factoryMethod = factoryMethod;
		this.argumentType = argType;
		this.argumentIndex = argIndex;
	}

	/**
	 * Return the factory method with the dependency that couldn't be satisfied.
	 */
	public Method getFactoryMethod() {
		return factoryMethod;
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
