/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
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

	private final @Nullable InjectionPoint injectionPoint;


	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param msg the detail message
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, @Nullable String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = null;
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 */
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, BeansException ex) {

		this(resourceDescription, beanName, propertyName, ex.getMessage());
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
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, @Nullable String msg) {

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
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, BeansException ex) {

		this(resourceDescription, beanName, injectionPoint, ex.getMessage());
		initCause(ex);
	}


	/**
	 * Return the injection point (field or method/constructor parameter), if known.
	 * @since 4.3
	 */
	public @Nullable InjectionPoint getInjectionPoint() {
		return this.injectionPoint;
	}

}
