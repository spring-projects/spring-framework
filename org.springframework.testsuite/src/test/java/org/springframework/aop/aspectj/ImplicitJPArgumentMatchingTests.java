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
package org.springframework.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.TestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests to check if the first implicit join point argument is correctly processed.
 * See SPR-3723 for more details.
 *   
 * @author Ramnivas Laddad
 */
public class ImplicitJPArgumentMatchingTests extends AbstractDependencyInjectionSpringContextTests {
	protected TestBean testBean; 
	
	public ImplicitJPArgumentMatchingTests() {
		setPopulateProtectedVariables(true);
	}

	protected String getConfigPath() {
		return "implicit-jp-argument-matching-tests.xml";
	}

	public void testAspect() {
		// nothing to really test; it is enough if we don't get error while creating app context
		testBean.setCountry("US");
	}
	
	public static class CounterAspect {
		public void increment(ProceedingJoinPoint pjp, Object bean, Object argument) throws Throwable {
			pjp.proceed();
		}
	}
}

