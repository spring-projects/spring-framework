/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.Pointcut;

/**
 * Interface to be implemented by pointcuts that use String expressions.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public interface ExpressionPointcut extends Pointcut {

	/**
	 * Return the String expression for this pointcut.
	 */
	@Nullable String getExpression();

}
