/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	public void interfaceMethod();
}


class InterfaceExtendingAspect implements AnInterface {
	public void increment(ProceedingJoinPoint pjp) throws Throwable {
		pjp.proceed();
	}

	@Override
	public void interfaceMethod() {
	}
}
