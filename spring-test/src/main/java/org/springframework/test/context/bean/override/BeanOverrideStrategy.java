/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

/**
 * Strategies for bean override processing.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
public enum BeanOverrideStrategy {

	/**
	 * Replace a given bean, immediately preparing a singleton instance.
	 * <p>Fails if the original bean does not exist. To create a new bean
	 * in such a case, use {@link #REPLACE_OR_CREATE} instead.
	 */
	REPLACE,

	/**
	 * Replace or create a given bean, immediately preparing a singleton instance.
	 * <p>Contrary to {@link #REPLACE}, this strategy creates a new bean if the
	 * target bean does not exist rather than failing.
	 */
	REPLACE_OR_CREATE,

	/**
	 * Intercept and process an early bean reference, allowing variants of bean
	 * overriding to wrap the original bean instance &mdash; for example, to
	 * delegate to actual methods in the context of a mocking "spy".
	 */
	WRAP

}
