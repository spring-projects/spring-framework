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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AfterReturningAdviceBindingTests {

	private ClassPathXmlApplicationContext ctx;

	private AfterReturningAdviceBindingTestAspect afterAdviceAspect;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	private AfterReturningAdviceBindingCollaborator mockCollaborator = mock();


	@BeforeEach
	void setup() throws Exception {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		afterAdviceAspect = (AfterReturningAdviceBindingTestAspect) ctx.getBean("testAspect");

		afterAdviceAspect.setCollaborator(mockCollaborator);

		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();

		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised)testBeanProxy).getTargetSource().getTarget();
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void oneIntArg() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntArg(5);
	}

	@Test
	void oneObjectArg() {
		testBeanProxy.getAge();
		verify(mockCollaborator).oneObjectArg(this.testBeanProxy);
	}

	@Test
	void oneIntAndOneObjectArgs() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntAndOneObject(5,this.testBeanProxy);
	}

	@Test
	void needsJoinPoint() {
		testBeanProxy.getAge();
		verify(mockCollaborator).needsJoinPoint("getAge");
	}

	@Test
	void needsJoinPointStaticPart() {
		testBeanProxy.getAge();
		verify(mockCollaborator).needsJoinPointStaticPart("getAge");
	}

	@Test
	void returningString() {
		testBeanProxy.setName("adrian");
		testBeanProxy.getName();
		verify(mockCollaborator).oneString("adrian");
	}

	@Test
	void returningObject() {
		testBeanProxy.returnsThis();
		verify(mockCollaborator).oneObjectArg(this.testBeanTarget);
	}

	@Test
	void returningBean() {
		testBeanProxy.returnsThis();
		verify(mockCollaborator).oneTestBeanArg(this.testBeanTarget);
	}

	@Test
	void returningBeanArray() {
		this.testBeanTarget.setSpouse(new TestBean());
		ITestBean[] spouses = this.testBeanTarget.getSpouses();
		testBeanProxy.getSpouses();
		verify(mockCollaborator).testBeanArrayArg(spouses);
	}

	@Test
	void noInvokeWhenReturningParameterTypeDoesNotMatch() {
		testBeanProxy.setSpouse(this.testBeanProxy);
		testBeanProxy.getSpouse();
		verifyNoInteractions(mockCollaborator);
	}

	@Test
	void returningByType() {
		testBeanProxy.returnsThis();
		verify(mockCollaborator).objectMatchNoArgs();
	}

	@Test
	void returningPrimitive() {
		testBeanProxy.setAge(20);
		testBeanProxy.haveBirthday();
		verify(mockCollaborator).oneInt(20);
	}

}


final class AfterReturningAdviceBindingTestAspect extends AdviceBindingTestAspect {

	private AfterReturningAdviceBindingCollaborator getCollaborator() {
		return (AfterReturningAdviceBindingCollaborator) this.collaborator;
	}

	public void oneString(String name) {
		getCollaborator().oneString(name);
	}

	public void oneTestBeanArg(TestBean bean) {
		getCollaborator().oneTestBeanArg(bean);
	}

	public void testBeanArrayArg(ITestBean[] beans) {
		getCollaborator().testBeanArrayArg(beans);
	}

	public void objectMatchNoArgs() {
		getCollaborator().objectMatchNoArgs();
	}

	public void stringMatchNoArgs() {
		getCollaborator().stringMatchNoArgs();
	}

	public void oneInt(int result) {
		getCollaborator().oneInt(result);
	}

	interface AfterReturningAdviceBindingCollaborator extends AdviceBindingCollaborator {

		void oneString(String s);
		void oneTestBeanArg(TestBean b);
		void testBeanArrayArg(ITestBean[] b);
		void objectMatchNoArgs();
		void stringMatchNoArgs();
		void oneInt(int result);
	}

}
