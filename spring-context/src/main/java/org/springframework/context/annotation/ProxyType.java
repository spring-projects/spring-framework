/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

/**
 * Common enum for indicating a desired proxy type.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see Proxyable#value()
 */
public enum ProxyType {

	/**
	 * Default is a JDK dynamic proxy, or potentially a class-based CGLIB proxy
	 * when globally configured.
	 */
	DEFAULT,

	/**
	 * Suggest a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
	 * the class of the target object. Overrides a globally configured default.
	 */
	INTERFACES,

	/**
	 * Suggest a class-based CGLIB proxy. Overrides a globally configured default.
	 */
	TARGET_CLASS

}
