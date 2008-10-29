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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for target selection matching (see SPR-3783).
 * Thanks to Tomasz Blachowicz for the bug report!
 *
 * @author Ramnivas Laddad
 */
public class TargetPointcutSelectionTests extends AbstractDependencyInjectionSpringContextTests {

	protected TestInterface testImpl1;
	protected TestInterface testImpl2;
	protected TestAspect testAspectForTestImpl1;
	protected TestAspect testAspectForAbstractTestImpl;
	protected TestInterceptor testInterceptor;
	

	public TargetPointcutSelectionTests() {
		setPopulateProtectedVariables(true);
	}
	
	protected String getConfigPath() {
		return "targetPointcutSelectionTests.xml";
	}
	
	protected void onSetUp() throws Exception {
		testAspectForTestImpl1.count = 0;
		testAspectForAbstractTestImpl.count = 0;
		testInterceptor.count = 0;
		super.onSetUp();
	}


	public void testTargetSelectionForMatchedType() {
		testImpl1.interfaceMethod();
		assertEquals("Should have been advised by POJO advice for impl", 1, testAspectForTestImpl1.count);
		assertEquals("Should have been advised by POJO advice for base type", 1, testAspectForAbstractTestImpl.count);
		assertEquals("Should have been advised by advisor", 1, testInterceptor.count);
	}

	public void testTargetNonSelectionForMismatchedType() {
		testImpl2.interfaceMethod();
		assertEquals("Shouldn't have been advised by POJO advice for impl", 0, testAspectForTestImpl1.count);
		assertEquals("Should have been advised by POJO advice for base type", 1, testAspectForAbstractTestImpl.count);
		assertEquals("Shouldn't have been advised by advisor", 0, testInterceptor.count);
	}


	public static interface TestInterface {

		public void interfaceMethod();
	}
	

	// Reproducing bug requires that the class specified in target() pointcut doesn't
	// include the advised method's implementation (instead a base class should include it)
	public static abstract class AbstractTestImpl implements TestInterface {

		public void interfaceMethod() {
		}
	}
	

	public static class TestImpl1 extends AbstractTestImpl {
	}


	public static class TestImpl2 extends AbstractTestImpl {
	}


	public static class TestAspect {

		public int count;
		
		public void increment() {
			count++;
		}
	}
	

	public static class TestInterceptor extends TestAspect implements MethodInterceptor {

		public Object invoke(MethodInvocation mi) throws Throwable {
			increment();
			return mi.proceed();
		}
	}

}
