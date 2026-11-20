/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

/**
 * Exception thrown in case of a bean being requested despite
 * bean creation currently not being allowed (for example, during
 * the shutdown phase of a bean factory).
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class BeanCreationNotAllowedException extends BeanCreationException {

	/**
	 * Create a new BeanCreationNotAllowedException.
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationNotAllowedException(String beanName, String msg) {
		super(beanName, msg);
	}

}
