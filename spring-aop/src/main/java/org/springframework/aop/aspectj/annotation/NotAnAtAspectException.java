/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.framework.AopConfigException;

/**
 * Extension of AopConfigException thrown when trying to perform
 * an advisor generation operation on a class that is not an
 * AspectJ annotation-style aspect.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@SuppressWarnings("serial")
public class NotAnAtAspectException extends AopConfigException {

	private final Class<?> nonAspectClass;


	/**
	 * Create a new NotAnAtAspectException for the given class.
	 * @param nonAspectClass the offending class
	 */
	public NotAnAtAspectException(Class<?> nonAspectClass) {
		super(nonAspectClass.getName() + " is not an @AspectJ aspect");
		this.nonAspectClass = nonAspectClass;
	}

	/**
	 * Returns the offending class.
	 */
	public Class<?> getNonAspectClass() {
		return this.nonAspectClass;
	}

}
