/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class AfterReturningAdviceBindingTests {

	private AfterReturningAdviceBindingTestAspect afterAdviceAspect;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	private AfterReturningAdviceBindingCollaborator mockCollaborator;


	public void setAfterReturningAdviceAspect(AfterReturningAdviceBindingTestAspect anAspect) {
		this.afterAdviceAspect = anAspect;
	}

	@Before
	public void setUp() throws Exception {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		afterAdviceAspect = (AfterReturningAdviceBindingTestAspect) ctx.getBean("testAspect");

		mockCollaborator = createNiceMock(AfterReturningAdviceBindingCollaborator.class);
		afterAdviceAspect.setCollaborator(mockCollaborator);

		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(testBeanProxy));

		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised)testBeanProxy).getTargetSource().getTarget();
	}


	@Test
	public void testOneIntArg() {
		mockCollaborator.oneIntArg(5);
		replay(mockCollaborator);
		testBeanProxy.setAge(5);
		verify(mockCollaborator);
	}

	@Test
	public void testOneObjectArg() {
		mockCollaborator.oneObjectArg(this.testBeanProxy);
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

	@Test
	public void testOneIntAndOneObjectArgs() {
		mockCollaborator.oneIntAndOneObject(5,this.testBeanProxy);
		replay(mockCollaborator);
		testBeanProxy.setAge(5);
		verify(mockCollaborator);
	}

	@Test
	public void testNeedsJoinPoint() {
		mockCollaborator.needsJoinPoint("getAge");
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

	@Test
	public void testNeedsJoinPointStaticPart() {
		mockCollaborator.needsJoinPointStaticPart("getAge");
		replay(mockCollaborator);
		testBeanProxy.getAge();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningString() {
		mockCollaborator.oneString("adrian");
		replay(mockCollaborator);
		testBeanProxy.setName("adrian");
		testBeanProxy.getName();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningObject() {
		mockCollaborator.oneObjectArg(this.testBeanTarget);
		replay(mockCollaborator);
		testBeanProxy.returnsThis();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningBean() {
		mockCollaborator.oneTestBeanArg(this.testBeanTarget);
		replay(mockCollaborator);
		testBeanProxy.returnsThis();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningBeanArray() {
		this.testBeanTarget.setSpouse(new TestBean());
		ITestBean[] spouses = this.testBeanTarget.getSpouses();
		mockCollaborator.testBeanArrayArg(spouses);
		replay(mockCollaborator);
		testBeanProxy.getSpouses();
		verify(mockCollaborator);
	}

	@Test
	public void testNoInvokeWhenReturningParameterTypeDoesNotMatch() {
		// we need a strict mock for this...
		mockCollaborator = createMock(AfterReturningAdviceBindingCollaborator.class);
		afterAdviceAspect.setCollaborator(mockCollaborator);

		replay(mockCollaborator);
		testBeanProxy.setSpouse(this.testBeanProxy);
		testBeanProxy.getSpouse();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningByType() {
		mockCollaborator.objectMatchNoArgs();
		replay(mockCollaborator);
		testBeanProxy.returnsThis();
		verify(mockCollaborator);
	}

	@Test
	public void testReturningPrimitive() {
		mockCollaborator.oneInt(20);
		replay(mockCollaborator);
		testBeanProxy.setAge(20);
		testBeanProxy.haveBirthday();
		verify(mockCollaborator);
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
