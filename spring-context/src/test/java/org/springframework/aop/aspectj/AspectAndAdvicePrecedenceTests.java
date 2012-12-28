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

import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.ITestBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public final class AspectAndAdvicePrecedenceTests {

	private PrecedenceTestAspect highPrecedenceAspect;

	private PrecedenceTestAspect lowPrecedenceAspect;

	private SimpleSpringBeforeAdvice highPrecedenceSpringAdvice;

	private SimpleSpringBeforeAdvice lowPrecedenceSpringAdvice;

	private ITestBean testBean;


	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		highPrecedenceAspect = (PrecedenceTestAspect) ctx.getBean("highPrecedenceAspect");
		lowPrecedenceAspect = (PrecedenceTestAspect) ctx.getBean("lowPrecedenceAspect");
		highPrecedenceSpringAdvice = (SimpleSpringBeforeAdvice) ctx.getBean("highPrecedenceSpringAdvice");
		lowPrecedenceSpringAdvice = (SimpleSpringBeforeAdvice) ctx.getBean("lowPrecedenceSpringAdvice");
		testBean = (ITestBean) ctx.getBean("testBean");
	}

	// ========== end of test case set up, start of tests proper ===================

	@Test
	public void testAdviceOrder() {
		PrecedenceTestAspect.Collaborator collaborator = new PrecedenceVerifyingCollaborator();
		this.highPrecedenceAspect.setCollaborator(collaborator);
		this.lowPrecedenceAspect.setCollaborator(collaborator);
		this.highPrecedenceSpringAdvice.setCollaborator(collaborator);
		this.lowPrecedenceSpringAdvice.setCollaborator(collaborator);
		this.testBean.getAge();
	}


	private static class PrecedenceVerifyingCollaborator implements PrecedenceTestAspect.Collaborator {

		private static final String[] EXPECTED = {
			// this order confirmed by running the same aspects (minus the Spring AOP advisors)
			// through AspectJ...
			"beforeAdviceOne(highPrecedenceAspect)",  	       // 1
			"beforeAdviceTwo(highPrecedenceAspect)",           // 2
			"aroundAdviceOne(highPrecedenceAspect)",           // 3, before proceed
			  "aroundAdviceTwo(highPrecedenceAspect)",         // 4, before proceed
			    "beforeAdviceOne(highPrecedenceSpringAdvice)", // 5
			    "beforeAdviceOne(lowPrecedenceSpringAdvice)",  // 6
			    "beforeAdviceOne(lowPrecedenceAspect)",        // 7
			    "beforeAdviceTwo(lowPrecedenceAspect)",        // 8
			    "aroundAdviceOne(lowPrecedenceAspect)",        // 9, before proceed
			      "aroundAdviceTwo(lowPrecedenceAspect)",      // 10, before proceed
			      "aroundAdviceTwo(lowPrecedenceAspect)",      // 11, after proceed
			    "aroundAdviceOne(lowPrecedenceAspect)",        // 12, after proceed
			    "afterAdviceOne(lowPrecedenceAspect)",         // 13
			    "afterAdviceTwo(lowPrecedenceAspect)",         // 14
			  "aroundAdviceTwo(highPrecedenceAspect)",         // 15, after proceed
			"aroundAdviceOne(highPrecedenceAspect)",           // 16, after proceed
			"afterAdviceOne(highPrecedenceAspect)",            // 17
			"afterAdviceTwo(highPrecedenceAspect)"             // 18
		};

		private int adviceInvocationNumber = 0;

		private void checkAdvice(String whatJustHappened) {
			//System.out.println("[" + adviceInvocationNumber + "] " + whatJustHappened + " ==> " + EXPECTED[adviceInvocationNumber]);
			if (adviceInvocationNumber > (EXPECTED.length - 1)) {
				fail("Too many advice invocations, expecting " + EXPECTED.length
						+ " but had " + adviceInvocationNumber);
			}
			String expecting = EXPECTED[adviceInvocationNumber++];
			if (!whatJustHappened.equals(expecting)) {
				fail("Expecting '" + expecting + "' on advice invocation " + adviceInvocationNumber +
						" but got '" + whatJustHappened + "'");
			}
		}

		public void beforeAdviceOne(String beanName) {
			checkAdvice("beforeAdviceOne(" + beanName + ")");
		}

		public void beforeAdviceTwo(String beanName) {
			checkAdvice("beforeAdviceTwo(" + beanName + ")");
		}

		public void aroundAdviceOne(String beanName) {
			checkAdvice("aroundAdviceOne(" + beanName + ")");
		}

		public void aroundAdviceTwo(String beanName) {
			checkAdvice("aroundAdviceTwo(" + beanName + ")");
		}

		public void afterAdviceOne(String beanName) {
			checkAdvice("afterAdviceOne(" + beanName + ")");
		}

		public void afterAdviceTwo(String beanName) {
			checkAdvice("afterAdviceTwo(" + beanName + ")");
		}
	}

}


class PrecedenceTestAspect implements BeanNameAware, Ordered {

	private String name;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Collaborator collaborator;


	public void setBeanName(String name) {
		this.name = name;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	public void setCollaborator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	public void beforeAdviceOne() {
		this.collaborator.beforeAdviceOne(this.name);
	}

	public void beforeAdviceTwo() {
		this.collaborator.beforeAdviceTwo(this.name);
	}

	public int aroundAdviceOne(ProceedingJoinPoint pjp) {
		int ret = -1;
		this.collaborator.aroundAdviceOne(this.name);
		try {
			ret = ((Integer)pjp.proceed()).intValue();
		}
		catch(Throwable t) { throw new RuntimeException(t); }
		this.collaborator.aroundAdviceOne(this.name);
		return ret;
	}

	public int aroundAdviceTwo(ProceedingJoinPoint pjp) {
		int ret = -1;
		this.collaborator.aroundAdviceTwo(this.name);
		try {
			ret = ((Integer)pjp.proceed()).intValue();
		}
		catch(Throwable t) {throw new RuntimeException(t);}
		this.collaborator.aroundAdviceTwo(this.name);
		return ret;
	}

	public void afterAdviceOne() {
		this.collaborator.afterAdviceOne(this.name);
	}

	public void afterAdviceTwo() {
		this.collaborator.afterAdviceTwo(this.name);
	}


	public interface Collaborator {

		void beforeAdviceOne(String beanName);
		void beforeAdviceTwo(String beanName);
		void aroundAdviceOne(String beanName);
		void aroundAdviceTwo(String beanName);
		void afterAdviceOne(String beanName);
		void afterAdviceTwo(String beanName);
	}

}


class SimpleSpringBeforeAdvice implements MethodBeforeAdvice, BeanNameAware {

	private PrecedenceTestAspect.Collaborator collaborator;
	private String name;

	/* (non-Javadoc)
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	public void before(Method method, Object[] args, Object target)
			throws Throwable {
		this.collaborator.beforeAdviceOne(this.name);
	}

	public void setCollaborator(PrecedenceTestAspect.Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

}
