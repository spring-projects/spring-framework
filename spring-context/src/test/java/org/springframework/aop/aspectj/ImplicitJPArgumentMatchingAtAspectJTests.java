/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests to check if the first implicit join point argument is correctly processed.
 * See SPR-3723 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class ImplicitJPArgumentMatchingAtAspectJTests {

	@Test
	void aspect() {
		// nothing to really test; it is enough if we don't get error while creating the app context
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
	}

	@Aspect
	static class CounterAtAspectJAspect {
		@Around(value="execution(* org.springframework.beans.testfixture.beans.TestBean.*(..)) and this(bean) and args(argument)",
				argNames="bean,argument")
		public void increment(ProceedingJoinPoint pjp, TestBean bean, Object argument) throws Throwable {
			pjp.proceed();
		}
	}
}

