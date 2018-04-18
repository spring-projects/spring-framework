/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Exception thrown when instantiation of a bean failed.
 * Carries the offending bean class.
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 */
@SuppressWarnings("serial")
public class BeanInstantiationException extends FatalBeanException {

	private Class<?> beanClass;

	@Nullable
	private Constructor<?> constructor;

	@Nullable
	private Method constructingMethod;


	/**
	 * Create a new BeanInstantiationException.
	 * @param beanClass the offending bean class
	 * @param msg the detail message
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg) {
		this(beanClass, msg, null);
	}

	/**
	 * Create a new BeanInstantiationException.
	 * @param beanClass the offending bean class
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
	}

	/**
	 * Create a new BeanInstantiationException.
	 * @param constructor the offending constructor
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Constructor<?> constructor, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
		this.beanClass = constructor.getDeclaringClass();
		this.constructor = constructor;
	}

	/**
	 * Create a new BeanInstantiationException.
	 * @param constructingMethod the delegate for bean construction purposes
	 * (typically, but not necessarily, a static factory method)
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Method constructingMethod, String msg, @Nullable Throwable cause) {
		super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
		this.beanClass = constructingMethod.getReturnType();
		this.constructingMethod = constructingMethod;
	}


	/**
	 * Return the offending bean class (never {@code null}).
	 * @return the class that was to be instantiated
	 */
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	/**
	 * Return the offending constructor, if known.
	 * @return the constructor in use, or {@code null} in case of a
	 * factory method or in case of default instantiation
	 * @since 4.3
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * Return the delegate for bean construction purposes, if known.
	 * @return the method in use (typically a static factory method),
	 * or {@code null} in case of constructor-based instantiation
	 * @since 4.3
	 */
	@Nullable
	public Method getConstructingMethod() {
		return this.constructingMethod;
	}

}
