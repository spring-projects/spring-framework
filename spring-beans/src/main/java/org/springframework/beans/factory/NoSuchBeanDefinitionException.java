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
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean
 * instance for which it cannot find a definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class NoSuchBeanDefinitionException extends BeansException {

	/** Name of the missing bean. */
	private String beanName;

	/** Required bean type */
	private Class<?> beanType;


	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param name the name of the missing bean
	 */
	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' is defined");
		this.beanName = name;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param name the name of the missing bean
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' is defined: " + message);
		this.beanName = name;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type required type of the missing bean
	 */
	public NoSuchBeanDefinitionException(Class<?> type) {
		super("No unique bean of type [" + type.getName() + "] is defined");
		this.beanType = type;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type required type of the missing bean
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String message) {
		super("No unique bean of type [" + type.getName() + "] is defined: " + message);
		this.beanType = type;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type required type of the missing bean
	 * @param dependencyDescription a description of the originating dependency
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String dependencyDescription, String message) {
		super("No matching bean of type [" + type.getName() + "] found for dependency" +
				(StringUtils.hasLength(dependencyDescription) ? " [" + dependencyDescription + "]" : "") +
				": " + message);
		this.beanType = type;
	}


	/**
	 * Return the name of the missing bean, if it was a lookup <em>by name</em>
	 * that failed.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the required type of the missing bean, if it was a lookup
	 * <em>by type</em> that failed.
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

}
