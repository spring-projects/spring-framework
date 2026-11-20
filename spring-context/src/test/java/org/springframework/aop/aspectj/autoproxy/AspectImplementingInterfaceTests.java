/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for ensuring the aspects aren't advised. See SPR-3893 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Sam Brannen
 */
class AspectImplementingInterfaceTests {

	@Test
	void proxyCreation() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		ITestBean testBean = ctx.getBean("testBean", ITestBean.class);
		AnInterface interfaceExtendingAspect = ctx.getBean("interfaceExtendingAspect", AnInterface.class);

		assertThat(testBean).isInstanceOf(Advised.class);
		assertThat(interfaceExtendingAspect).isNotInstanceOf(Advised.class);
		ctx.close();
	}

}


interface AnInterface {
	void interfaceMethod();
}


class InterfaceExtendingAspect implements AnInterface {
	public void increment(ProceedingJoinPoint pjp) throws Throwable {
		pjp.proceed();
	}

	@Override
	public void interfaceMethod() {
	}
}
