/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Sam Brannen
 */
class ThisAndTargetSelectionOnlyPointcutsAtAspectJTests {

	private ClassPathXmlApplicationContext ctx;

	private TestInterface testBean;

	private TestInterface testAnnotatedClassBean;

	private TestInterface testAnnotatedMethodBean;

	private Counter counter;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean = (TestInterface) ctx.getBean("testBean");
		testAnnotatedClassBean = (TestInterface) ctx.getBean("testAnnotatedClassBean");
		testAnnotatedMethodBean = (TestInterface) ctx.getBean("testAnnotatedMethodBean");
		counter = (Counter) ctx.getBean("counter");
		counter.reset();
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void thisAsClassDoesNotMatch() {
		testBean.doIt();
		assertThat(counter.thisAsClassCounter).isEqualTo(0);
	}

	@Test
	void thisAsInterfaceMatch() {
		testBean.doIt();
		assertThat(counter.thisAsInterfaceCounter).isEqualTo(1);
	}

	@Test
	void targetAsClassDoesMatch() {
		testBean.doIt();
		assertThat(counter.targetAsClassCounter).isEqualTo(1);
	}

	@Test
	void targetAsInterfaceMatch() {
		testBean.doIt();
		assertThat(counter.targetAsInterfaceCounter).isEqualTo(1);
	}

	@Test
	void thisAsClassAndTargetAsClassCounterNotMatch() {
		testBean.doIt();
		assertThat(counter.thisAsClassAndTargetAsClassCounter).isEqualTo(0);
	}

	@Test
	void thisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		testBean.doIt();
		assertThat(counter.thisAsInterfaceAndTargetAsInterfaceCounter).isEqualTo(1);
	}

	@Test
	void thisAsInterfaceAndTargetAsClassCounterMatch() {
		testBean.doIt();
		assertThat(counter.thisAsInterfaceAndTargetAsClassCounter).isEqualTo(1);
	}


	@Test
	void atTargetClassAnnotationMatch() {
		testAnnotatedClassBean.doIt();
		assertThat(counter.atTargetClassAnnotationCounter).isEqualTo(1);
	}

	@Test
	void atAnnotationMethodAnnotationMatch() {
		testAnnotatedMethodBean.doIt();
		assertThat(counter.atAnnotationMethodAnnotationCounter).isEqualTo(1);
	}

	interface TestInterface {
		void doIt();
	}

	static class TestImpl implements TestInterface {
		@Override
		public void doIt() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation {
	}

	@TestAnnotation
	static class AnnotatedClassTestImpl implements TestInterface {
		@Override
		public void doIt() {
		}
	}

	static class AnnotatedMethodTestImpl implements TestInterface {
		@Override
		@TestAnnotation
		public void doIt() {
		}
	}

	@Aspect
	static class Counter {
		int thisAsClassCounter;
		int thisAsInterfaceCounter;
		int targetAsClassCounter;
		int targetAsInterfaceCounter;
		int thisAsClassAndTargetAsClassCounter;
		int thisAsInterfaceAndTargetAsInterfaceCounter;
		int thisAsInterfaceAndTargetAsClassCounter;
		int atTargetClassAnnotationCounter;
		int atAnnotationMethodAnnotationCounter;

		public void reset() {
			thisAsClassCounter = 0;
			thisAsInterfaceCounter = 0;
			targetAsClassCounter = 0;
			targetAsInterfaceCounter = 0;
			thisAsClassAndTargetAsClassCounter = 0;
			thisAsInterfaceAndTargetAsInterfaceCounter = 0;
			thisAsInterfaceAndTargetAsClassCounter = 0;
			atTargetClassAnnotationCounter = 0;
			atAnnotationMethodAnnotationCounter = 0;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsClassCounter() {
			thisAsClassCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementThisAsInterfaceCounter() {
			thisAsInterfaceCounter++;
		}

		@Before("target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementTargetAsClassCounter() {
			targetAsClassCounter++;
		}

		@Before("target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementTargetAsInterfaceCounter() {
			targetAsInterfaceCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsClassAndTargetAsClassCounter() {
			thisAsClassAndTargetAsClassCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementThisAsInterfaceAndTargetAsInterfaceCounter() {
			thisAsInterfaceAndTargetAsInterfaceCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsInterfaceAndTargetAsClassCounter() {
			thisAsInterfaceAndTargetAsClassCounter++;
		}

		@Before("@target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
		public void incrementAtTargetClassAnnotationCounter() {
			atTargetClassAnnotationCounter++;
		}

		@Before("@annotation(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
		public void incrementAtAnnotationMethodAnnotationCounter() {
			atAnnotationMethodAnnotationCounter++;
		}

	}

}
