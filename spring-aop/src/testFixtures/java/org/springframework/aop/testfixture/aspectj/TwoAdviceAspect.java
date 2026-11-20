/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class TwoAdviceAspect {

	private int totalCalls;

	@Around("execution(* org.springframework.beans.testfixture.beans.ITestBean.age())")
	public int returnCallCount(ProceedingJoinPoint pjp) throws Exception {
		return totalCalls;
	}

	@Before("execution(* org.springframework.beans.testfixture.beans.ITestBean.setAge(int)) && args(newAge)")
	public void countSet(int newAge) throws Exception {
		++totalCalls;
	}

}
