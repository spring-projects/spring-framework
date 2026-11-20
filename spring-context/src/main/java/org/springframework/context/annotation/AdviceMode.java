/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

/**
 * Enumeration used to determine whether JDK/CGLIB proxy-based or
 * AspectJ weaving-based advice should be applied.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.scheduling.annotation.AsyncConfigurationSelector#selectImports
 * @see org.springframework.scheduling.annotation.EnableAsync#mode()
 */
public enum AdviceMode {

	/**
	 * JDK/CGLIB proxy-based advice.
	 */
	PROXY,

	/**
	 * AspectJ weaving-based advice.
	 */
	ASPECTJ

}
