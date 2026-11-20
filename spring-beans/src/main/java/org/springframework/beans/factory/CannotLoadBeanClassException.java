/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.FatalBeanException;

/**
 * Exception thrown when the BeanFactory cannot load the specified class
 * of a given bean.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class CannotLoadBeanClassException extends FatalBeanException {

	private final @Nullable String resourceDescription;

	private final String beanName;

	private final @Nullable String beanClassName;


	/**
	 * Create a new CannotLoadBeanClassException.
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param beanClassName the name of the bean class
	 * @param cause the root cause
	 */
	public CannotLoadBeanClassException(@Nullable String resourceDescription, String beanName,
			@Nullable String beanClassName, ClassNotFoundException cause) {

		super("Cannot find class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}

	/**
	 * Create a new CannotLoadBeanClassException.
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param beanClassName the name of the bean class
	 * @param cause the root cause
	 */
	public CannotLoadBeanClassException(@Nullable String resourceDescription, String beanName,
			@Nullable String beanClassName, LinkageError cause) {

		super("Error loading class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") +
				": problem with class file or dependent class", cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}


	/**
	 * Return the description of the resource that the bean
	 * definition came from.
	 */
	public @Nullable String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * Return the name of the bean requested.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the name of the class we were trying to load.
	 */
	public @Nullable String getBeanClassName() {
		return this.beanClassName;
	}

}
