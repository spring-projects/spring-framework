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

import org.springframework.beans.ITestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Adrian Colyer
 */
public class AspectAndAdvicePrecedenceTests extends AbstractDependencyInjectionSpringContextTests {

	private PrecedenceTestAspect highPrecedenceAspect;
	private PrecedenceTestAspect lowPrecedenceAspect;
	private SimpleSpringBeforeAdvice lowPrecedenceSpringAdvice;
	private SimpleSpringBeforeAdvice highPrecedenceSpringAdvice;
	private ITestBean testBean;
	

	public AspectAndAdvicePrecedenceTests() {
		setAutowireMode(AUTOWIRE_BY_NAME);
	}

	public void setHighPrecedenceAspect(PrecedenceTestAspect highPrecedenceAspect) {
		this.highPrecedenceAspect = highPrecedenceAspect;
	}

	public void setLowPrecedenceAspect(PrecedenceTestAspect lowPrecedenceAspect) {
		this.lowPrecedenceAspect = lowPrecedenceAspect;
	}

	public void setLowPrecedenceSpringAdvice(SimpleSpringBeforeAdvice lowPrecedenceSpringAdvice) {
		this.lowPrecedenceSpringAdvice = lowPrecedenceSpringAdvice;
	}

	public void setHighPrecedenceSpringAdvice(SimpleSpringBeforeAdvice highPrecedenceSpringAdvice) {
		this.highPrecedenceSpringAdvice = highPrecedenceSpringAdvice;
	}
	
	public void setTestBean(ITestBean testBean) {
		this.testBean = testBean;
	}

	protected String getConfigPath() {
		return "advice-precedence-tests.xml";
	}


	// ========== end of test case set up, start of tests proper ===================

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
