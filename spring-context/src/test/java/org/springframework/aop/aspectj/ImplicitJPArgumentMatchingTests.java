/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests to check if the first implicit join point argument is correctly processed.
 * See SPR-3723 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class ImplicitJPArgumentMatchingTests {

	@Test
	@SuppressWarnings("resource")
	void aspect() {
		// nothing to really test; it is enough if we don't get error while creating app context
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
	}

	static class CounterAspect {
		public void increment(ProceedingJoinPoint pjp, Object bean, Object argument) throws Throwable {
			pjp.proceed();
		}
	}
}

