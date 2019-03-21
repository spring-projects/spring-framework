/*
 * Copyright 2002-2017 the original author or authors.
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

	private String resourceDescription;

	private String beanName;

	private String beanClassName;


	/**
	 * Create a new CannotLoadBeanClassException.
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param beanClassName the name of the bean class
	 * @param cause the root cause
	 */
	public CannotLoadBeanClassException(
			String resourceDescription, String beanName, String beanClassName, ClassNotFoundException cause) {

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
	public CannotLoadBeanClassException(
			String resourceDescription, String beanName, String beanClassName, LinkageError cause) {

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
	public String getResourceDescription() {
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
	public String getBeanClassName() {
		return this.beanClassName;
	}

}
