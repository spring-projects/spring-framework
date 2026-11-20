/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test.self_inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import org.springframework.stereotype.Component;

@Aspect
@Component
public class MyAspect {

	@Before("within(org.springframework.context.event.test.self_inject.MyEventListener)")
	public void myAdvice(JoinPoint joinPoint) {
		// System.out.println(joinPoint);
	}

}
