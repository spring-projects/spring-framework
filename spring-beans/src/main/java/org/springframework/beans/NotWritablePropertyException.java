/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on an attempt to set the value of a property that
 * is not writable (typically because there is no setter method).
 *
 * @author Rod Johnson
 * @author Alef Arendsen
 * @author Arjen Poutsma
 */
@SuppressWarnings("serial")
public class NotWritablePropertyException extends InvalidPropertyException {

	private final String @Nullable [] possibleMatches;


	/**
	 * Create a new NotWritablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property name
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' is not writable or has an invalid setter method: " +
				"Does the return type of the getter match the parameter type of the setter?");
		this.possibleMatches = null;
	}

	/**
	 * Create a new NotWritablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property name
	 * @param msg the detail message
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
		this.possibleMatches = null;
	}

	/**
	 * Create a new NotWritablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property name
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
		this.possibleMatches = null;
	}

	/**
	 * Create a new NotWritablePropertyException.
	 * @param beanClass the offending bean class
	 * @param propertyName the offending property name
	 * @param msg the detail message
	 * @param possibleMatches suggestions for actual bean property names
	 * that closely match the invalid property name
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, String[] possibleMatches) {
		super(beanClass, propertyName, msg);
		this.possibleMatches = possibleMatches;
	}


	/**
	 * Return suggestions for actual bean property names that closely match
	 * the invalid property name, if any.
	 */
	public String @Nullable [] getPossibleMatches() {
		return this.possibleMatches;
	}

}
