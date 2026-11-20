/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.subpkg;

/**
 * Used for testing pointcut matching.
 *
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcutTests#testWithinRootAndSubpackages()
 *
 * @author Chris Beams
 */
public class DeepBean {
	public void aMethod(String foo) {
		// no-op
	}
}
