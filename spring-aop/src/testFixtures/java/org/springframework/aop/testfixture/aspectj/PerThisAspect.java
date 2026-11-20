/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect("perthis(org.springframework.aop.testfixture.aspectj.CommonPointcuts.getAgeExecution())")
public class PerThisAspect {

	private int invocations = 0;

	public int getInvocations() {
		return this.invocations;
	}

	@Around("org.springframework.aop.testfixture.aspectj.CommonPointcuts.getAgeExecution()")
	public int changeAge(ProceedingJoinPoint pjp) {
		return this.invocations++;
	}

}
