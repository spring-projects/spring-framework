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

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.ITestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Test for ensuring the aspects aren't advised. See SPR-3893 for more details.
 *
 * @author Ramnivas Laddad
 */
public class AspectImplementingInterfaceTests extends AbstractDependencyInjectionSpringContextTests {
	protected ITestBean testBean;
	protected AnInterface interfaceExtendingAspect;

	public AspectImplementingInterfaceTests() {
		setPopulateProtectedVariables(true);
	}

	protected String getConfigPath() {
		return "aspect-implementing-interface-tests.xml";
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
	}

	public void testProxyCreation() {
		assertTrue(testBean instanceof Advised);
		assertFalse(interfaceExtendingAspect instanceof Advised);
	}

	public static interface AnInterface {
		public void interfaceMethod();
	}
	
	public static class InterfaceExtendingAspect implements AnInterface {
		public void increment(ProceedingJoinPoint pjp) throws Throwable {
			pjp.proceed();
		}

		public void interfaceMethod() {
		}
	}
}
