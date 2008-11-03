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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Ramnivas Laddad
 */
public class ThisAndTargetSelectionOnlyPointcutsAtAspectJTests extends AbstractDependencyInjectionSpringContextTests {
	protected TestInterface testBean;
	protected TestInterface testAnnotatedClassBean;
	protected TestInterface testAnnotatedMethodBean;
	
	protected Counter counter;
	
	public ThisAndTargetSelectionOnlyPointcutsAtAspectJTests() {
		setPopulateProtectedVariables(true);
	}
	
	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		counter.reset();
	}

	protected String getConfigPath() {
		return "this-and-target-selectionOnly-pointcuts-atAspectJ-tests.xml";
	}
	
	public void testThisAsClassDoesNotMatch() {
		testBean.doIt();
		assertEquals(0, counter.thisAsClassCounter);
	}

	public void testThisAsInterfaceMatch() {
		testBean.doIt();
		assertEquals(1, counter.thisAsInterfaceCounter);
	}

	public void testTargetAsClassDoesMatch() {
		testBean.doIt();
		assertEquals(1, counter.targetAsClassCounter);
	}

	public void testTargetAsInterfaceMatch() {
		testBean.doIt();
		assertEquals(1, counter.targetAsInterfaceCounter);
	}

	public void testThisAsClassAndTargetAsClassCounterNotMatch() {
		testBean.doIt();
		assertEquals(0, counter.thisAsClassAndTargetAsClassCounter);
	}

	public void testThisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		testBean.doIt();
		assertEquals(1, counter.thisAsInterfaceAndTargetAsInterfaceCounter);
	}

	public void testThisAsInterfaceAndTargetAsClassCounterMatch() {
		testBean.doIt();
		assertEquals(1, counter.thisAsInterfaceAndTargetAsInterfaceCounter);
	}
	
	
	public void testAtTargetClassAnnotationMatch() {
		testAnnotatedClassBean.doIt();
		assertEquals(1, counter.atTargetClassAnnotationCounter);
	}

	public void testAtAnnotationMethodAnnotationMatch() {
		testAnnotatedMethodBean.doIt();
		assertEquals(1, counter.atAnnotationMethodAnnotationCounter);
	}
	
	public static interface TestInterface {
		public void doIt();
	}

	public static class TestImpl implements TestInterface {
		public void doIt() {
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface TestAnnotation {
		
	}
	
	@TestAnnotation
	public static class AnnotatedClassTestImpl implements TestInterface {
		public void doIt() {
		}
	}

	public static class AnnotatedMethodTestImpl implements TestInterface {
		@TestAnnotation
		public void doIt() {
		}
	}
	
	@Aspect
	public static class Counter {
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
