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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.AroundAdviceBindingTestAspect.AroundAdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AroundAdviceBindingTests {

	private AroundAdviceBindingCollaborator mockCollaborator;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	protected ApplicationContext ctx;

	@Before
	public void onSetUp() throws Exception {
		ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		AroundAdviceBindingTestAspect  aroundAdviceAspect = ((AroundAdviceBindingTestAspect) ctx.getBean("testAspect"));

		ITestBean injectedTestBean = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(injectedTestBean));

		this.testBeanProxy = injectedTestBean;
		// we need the real target too, not just the proxy...

		this.testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

		mockCollaborator = mock(AroundAdviceBindingCollaborator.class);
		aroundAdviceAspect.setCollaborator(mockCollaborator);
	}

	@Test
	public void testOneIntArg() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntArg(5);
	}

	@Test
	public void testOneObjectArgBoundToTarget() {
		testBeanProxy.getAge();
		verify(mockCollaborator).oneObjectArg(this.testBeanTarget);;
	}

	@Test
	public void testOneIntAndOneObjectArgs() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntAndOneObject(5, this.testBeanProxy);;
	}

	@Test
	public void testJustJoinPoint() {
		testBeanProxy.getAge();
		verify(mockCollaborator).justJoinPoint("getAge");;
	}

}


class AroundAdviceBindingTestAspect {

	private AroundAdviceBindingCollaborator collaborator = null;

	public void setCollaborator(AroundAdviceBindingCollaborator aCollaborator) {
		this.collaborator = aCollaborator;
	}

	// "advice" methods
	public void oneIntArg(ProceedingJoinPoint pjp, int age) throws Throwable {
		this.collaborator.oneIntArg(age);
		pjp.proceed();
	}

	public int oneObjectArg(ProceedingJoinPoint pjp, Object bean) throws Throwable {
		this.collaborator.oneObjectArg(bean);
		return ((Integer) pjp.proceed()).intValue();
	}

	public void oneIntAndOneObject(ProceedingJoinPoint pjp, int x , Object o) throws Throwable {
		this.collaborator.oneIntAndOneObject(x,o);
		pjp.proceed();
	}

	public int justJoinPoint(ProceedingJoinPoint pjp) throws Throwable {
		this.collaborator.justJoinPoint(pjp.getSignature().getName());
		return ((Integer) pjp.proceed()).intValue();
	}

	/**
	 * Collaborator interface that makes it easy to test this aspect
	 * is working as expected through mocking.
	 */
	public interface AroundAdviceBindingCollaborator {

		void oneIntArg(int x);

		void oneObjectArg(Object o);

		void oneIntAndOneObject(int x, Object o);

		void justJoinPoint(String s);
	}

}
