/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for target selection matching (see SPR-3783).
 * <p>Thanks to Tomasz Blachowicz for the bug report!
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class TargetPointcutSelectionTests {

	private ClassPathXmlApplicationContext ctx;

	private TestInterface testImpl1;

	private TestInterface testImpl2;

	private TestAspect testAspectForTestImpl1;

	private TestAspect testAspectForAbstractTestImpl;

	private TestInterceptor testInterceptor;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testImpl1 = (TestInterface) ctx.getBean("testImpl1");
		testImpl2 = (TestInterface) ctx.getBean("testImpl2");
		testAspectForTestImpl1 = (TestAspect) ctx.getBean("testAspectForTestImpl1");
		testAspectForAbstractTestImpl = (TestAspect) ctx.getBean("testAspectForAbstractTestImpl");
		testInterceptor = (TestInterceptor) ctx.getBean("testInterceptor");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void targetSelectionForMatchedType() {
		testImpl1.interfaceMethod();
		assertThat(testAspectForTestImpl1.count).as("Should have been advised by POJO advice for impl").isEqualTo(1);
		assertThat(testAspectForAbstractTestImpl.count).as("Should have been advised by POJO advice for base type").isEqualTo(1);
		assertThat(testInterceptor.count).as("Should have been advised by advisor").isEqualTo(1);
	}

	@Test
	void targetNonSelectionForMismatchedType() {
		testImpl2.interfaceMethod();
		assertThat(testAspectForTestImpl1.count).as("Shouldn't have been advised by POJO advice for impl").isZero();
		assertThat(testAspectForAbstractTestImpl.count).as("Should have been advised by POJO advice for base type").isEqualTo(1);
		assertThat(testInterceptor.count).as("Shouldn't have been advised by advisor").isZero();
	}


	interface TestInterface {
		void interfaceMethod();
	}

	// Reproducing bug requires that the class specified in target() pointcut doesn't
	// include the advised method's implementation (instead a base class should include it)
	abstract static class AbstractTestImpl implements TestInterface {

		@Override
		public void interfaceMethod() {
		}
	}

	static class TestImpl1 extends AbstractTestImpl {
	}

	static class TestImpl2 extends AbstractTestImpl {
	}

	static class TestAspect {

		int count;

		void increment() {
			count++;
		}
	}

	static class TestInterceptor extends TestAspect implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			increment();
			return mi.proceed();
		}
	}

}
