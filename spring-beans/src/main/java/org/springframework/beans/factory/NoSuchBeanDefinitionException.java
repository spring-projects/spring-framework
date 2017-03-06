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

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean instance for which it
 * cannot find a definition. This may point to a non-existing bean, a non-unique bean,
 * or a manually registered singleton instance without an associated bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @see NoUniqueBeanDefinitionException
 */
@SuppressWarnings("serial")
public class NoSuchBeanDefinitionException extends BeansException {

	private String beanName;

	private ResolvableType resolvableType;


	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param name the name of the missing bean
	 */
	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' available");
		this.beanName = name;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param name the name of the missing bean
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' available: " + message);
		this.beanName = name;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type required type of the missing bean
	 */
	public NoSuchBeanDefinitionException(Class<?> type) {
		this(ResolvableType.forClass(type));
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type required type of the missing bean
	 * @param message detailed message describing the problem
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String message) {
		this(ResolvableType.forClass(type), message);
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type full type declaration of the missing bean
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type) {
		super("No qualifying bean of type '" + type + "' available");
		this.resolvableType = type;
	}

	/**
	 * Create a new {@code NoSuchBeanDefinitionException}.
	 * @param type full type declaration of the missing bean
	 * @param message detailed message describing the problem
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type, String message) {
		super("No qualifying bean of type '" + type + "' available: " + message);
		this.resolvableType = type;
	}


	/**
	 * Return the name of the missing bean, if it was a lookup <em>by name</em> that failed.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the required type of the missing bean, if it was a lookup <em>by type</em>
	 * that failed.
	 */
	public Class<?> getBeanType() {
		return (this.resolvableType != null ? this.resolvableType.resolve() : null);
	}

	/**
	 * Return the required {@link ResolvableType} of the missing bean, if it was a lookup
	 * <em>by type</em> that failed.
	 * @since 4.3.4
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * Return the number of beans found when only one matching bean was expected.
	 * For a regular NoSuchBeanDefinitionException, this will always be 0.
	 * @see NoUniqueBeanDefinitionException
	 */
	public int getNumberOfBeansFound() {
		return 0;
	}

}
