/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

/**
 * Exception thrown in case of a reference to a bean that's currently in creation.
 * Typically happens when constructor autowiring matches the currently constructed bean.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class BeanCurrentlyInCreationException extends BeanCreationException {

	/**
	 * Create a new BeanCurrentlyInCreationException,
	 * with a default error message that indicates a circular reference.
	 * @param beanName the name of the bean requested
	 */
	public BeanCurrentlyInCreationException(String beanName) {
		super(beanName, "Requested bean is currently in creation: "+
				"Is there an unresolvable circular reference or an asynchronous initialization dependency?");
	}

	/**
	 * Create a new BeanCurrentlyInCreationException.
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCurrentlyInCreationException(String beanName, String msg) {
		super(beanName, msg);
	}

}
