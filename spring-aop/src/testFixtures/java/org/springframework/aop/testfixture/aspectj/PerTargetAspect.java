/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.aspectj;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import org.springframework.core.Ordered;

@Aspect("pertarget(execution(* *.getSpouse()))")
public class PerTargetAspect implements Ordered {

	public int count;

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Around("execution(int *.getAge())")
	public int returnCountAsAge() {
		return count++;
	}

	@Before("execution(void *.set*(int))")
	public void countSetter() {
		++count;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
