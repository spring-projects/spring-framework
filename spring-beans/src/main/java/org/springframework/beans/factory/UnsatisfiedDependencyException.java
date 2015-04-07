/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;

/**
 * Exception thrown when a bean depends on other beans or simple properties
 * that were not specified in the bean factory definition, although
 * dependency checking was enabled.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 03.09.2003
 */
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends BeanCreationException {

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param msg the detail message
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, String propertyName, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
				(msg != null ? ": " + msg : ""));
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, String propertyName, BeansException ex) {

		this(resourceDescription, beanName, propertyName, (ex != null ? ": " + ex.getMessage() : ""));
		initCause(ex);
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param ctorArgIndex the index of the constructor argument that couldn't be satisfied
	 * @param ctorArgType the type of the constructor argument that couldn't be satisfied
	 * @param msg the detail message
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through constructor argument with index " +
				ctorArgIndex + " of type [" + ClassUtils.getQualifiedName(ctorArgType) + "]" +
				(msg != null ? ": " + msg : ""));
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param ctorArgIndex the index of the constructor argument that couldn't be satisfied
	 * @param ctorArgType the type of the constructor argument that couldn't be satisfied
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, BeansException ex) {

		this(resourceDescription, beanName, ctorArgIndex, ctorArgType, (ex != null ? ": " + ex.getMessage() : ""));
		initCause(ex);
	}

}
