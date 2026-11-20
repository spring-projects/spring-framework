/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

/**
 * Exception thrown when navigation of a valid nested property
 * path encounters a NullPointerException.
 *
 * <p>For example, navigating "spouse.age" could fail because the
 * spouse property of the target object has a null value.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class NullValueInNestedPathException extends InvalidPropertyException {

	/**
	 * Create a new NullValueInNestedPathException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName, "Value of nested property '" + propertyName + "' is null");
	}

	/**
	 * Create a new NullValueInNestedPathException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 * @param msg the detail message
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * Create a new NullValueInNestedPathException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.3.2
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

}
