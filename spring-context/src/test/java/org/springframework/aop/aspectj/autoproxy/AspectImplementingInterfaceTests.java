/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.autoproxy;

import static org.junit.Assert.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Test for ensuring the aspects aren't advised. See SPR-3893 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public final class AspectImplementingInterfaceTests {

	@Test
	public void testProxyCreation() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		ITestBean testBean = (ITestBean) ctx.getBean("testBean");
		AnInterface interfaceExtendingAspect = (AnInterface) ctx.getBean("interfaceExtendingAspect");

		assertTrue(testBean instanceof Advised);
		assertFalse(interfaceExtendingAspect instanceof Advised);
	}

}


interface AnInterface {
	public void interfaceMethod();
}


class InterfaceExtendingAspect implements AnInterface {
	public void increment(ProceedingJoinPoint pjp) throws Throwable {
		pjp.proceed();
	}

	public void interfaceMethod() {
	}
}
