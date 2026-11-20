/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

/**
 * Exception thrown on an attempt to get the value of a property
 * that isn't readable, because there's no getter method.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
@SuppressWarnings("serial")
public class NotReadablePropertyException extends InvalidPropertyException {

	/**
	 * Create a new NotReadablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' is not readable or has an invalid getter method: " +
				"Does the return type of the getter match the parameter type of the setter?");
	}

	/**
	 * Create a new NotReadablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 * @param msg the detail message
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * Create a new NotReadablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property
	 * @param msg the detail message
	 * @param cause the root cause
	 * @since 4.0.9
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

}
