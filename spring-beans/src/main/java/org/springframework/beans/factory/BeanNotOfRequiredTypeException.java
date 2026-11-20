/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

import java.lang.reflect.Type;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Yanming Zhou
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

	/** The name of the instance that was of the wrong type. */
	private final String beanName;

	/** The required type. */
	private final Type genericRequiredType;

	/** The offending type. */
	private final Class<?> actualType;


	/**
	 * Create a new BeanNotOfRequiredTypeException.
	 * @param beanName the name of the bean requested
	 * @param requiredType the required type
	 * @param actualType the actual type returned, which did not match
	 * the expected type
	 */
	public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
		this(beanName, (Type) requiredType, actualType);
	}

	/**
	 * Create a new BeanNotOfRequiredTypeException.
	 * @param beanName the name of the bean requested
	 * @param requiredType the required type
	 * @param actualType the actual type returned, which did not match
	 * the expected type
	 * @since 7.1
	 */
	public BeanNotOfRequiredTypeException(String beanName, Type requiredType, Class<?> actualType) {
		super("Bean named '" + beanName + "' is expected to be of type '" + requiredType.getTypeName() +
				"' but was actually of type '" + actualType.getTypeName() + "'");
		this.beanName = beanName;
		this.genericRequiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * Return the name of the instance that was of the wrong type.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the expected type for the bean.
	 */
	public Class<?> getRequiredType() {
		return (this.genericRequiredType instanceof Class<?> clazz ? clazz : ResolvableType.forType(this.genericRequiredType).toClass());
	}

	/**
	 * Return the expected generic type for the bean.
	 * @since 7.1
	 */
	public Type getGenericRequiredType() {
		return this.genericRequiredType;
	}

	/**
	 * Return the actual type of the instance found.
	 */
	public Class<?> getActualType() {
		return this.actualType;
	}

}
