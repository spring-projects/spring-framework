/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.advice;

import java.lang.reflect.Method;

import org.springframework.aop.AfterReturningAdvice;

/**
 * Simple before advice example that we can use for counting checks.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CountingAfterReturningAdvice extends MethodCounter implements AfterReturningAdvice {

	@Override
	public void afterReturning(Object o, Method m, Object[] args, Object target) throws Throwable {
		count(m);
	}

}
