/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
