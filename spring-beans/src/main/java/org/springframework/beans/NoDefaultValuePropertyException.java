package org.springframework.beans;

/**
 * Exception is thrown on attempted to bind a default value of null to a target parameter
 *
 * @author dillonm79
 */
@SuppressWarnings("serial")
public class NoDefaultValuePropertyException extends InvalidPropertyException {


	/**
	 * Create a new NoDefaultValuePropertyException.
	 *
	 * @param beanClass    the offending bean class
	 * @param propertyName the offending property name
	 */
	public NoDefaultValuePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' does not have a default value. Default value must not be null");
	}
}
