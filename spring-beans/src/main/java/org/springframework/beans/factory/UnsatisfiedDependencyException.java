/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.util.StringUtils;

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

	private InjectionPoint injectionPoint;


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
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
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

		this(resourceDescription, beanName, propertyName, "");
		initCause(ex);
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param injectionPoint the injection point (field or method/constructor parameter)
	 * @param msg the detail message
	 * @since 4.3
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, InjectionPoint injectionPoint, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through " + injectionPoint +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = injectionPoint;
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param injectionPoint the injection point (field or method/constructor parameter)
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 * @since 4.3
	 */
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, InjectionPoint injectionPoint, BeansException ex) {

		this(resourceDescription, beanName, injectionPoint, "");
		initCause(ex);
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param ctorArgIndex the index of the constructor argument that couldn't be satisfied
	 * @param ctorArgType the type of the constructor argument that couldn't be satisfied
	 * @param msg the detail message
	 * @deprecated in favor of {@link #UnsatisfiedDependencyException(String, String, InjectionPoint, String)}
	 */
	@Deprecated
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
	 * @deprecated in favor of {@link #UnsatisfiedDependencyException(String, String, InjectionPoint, BeansException)}
	 */
	@Deprecated
	public UnsatisfiedDependencyException(
			String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, BeansException ex) {

		this(resourceDescription, beanName, ctorArgIndex, ctorArgType, (ex != null ? ex.getMessage() : ""));
		initCause(ex);
	}


	/**
	 * Return the injection point (field or method/constructor parameter), if known.
	 * @since 4.3
	 */
	public InjectionPoint getInjectionPoint() {
		return this.injectionPoint;
	}

}
