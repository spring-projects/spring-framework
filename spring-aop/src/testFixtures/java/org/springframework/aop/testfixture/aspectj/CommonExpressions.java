/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.aspectj;

/**
 * Common expressions that are used in tests.
 *
 * @author Stephane Nicoll
 */
public class CommonExpressions {

	/**
	 * An expression pointcut that matches all methods
	 */
	public static final String MATCH_ALL_METHODS = "execution(* *(..))";

}
