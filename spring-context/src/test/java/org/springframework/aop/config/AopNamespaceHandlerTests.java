/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.aop.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.aop.advice.CountingBeforeAdvice;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

/**
 * Unit tests for aop namespace.
 *
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AopNamespaceHandlerTests {

	private ApplicationContext context;


	@Before
	public void setUp() {
		this.context =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}

	protected ITestBean getTestBean() {
		return (ITestBean) this.context.getBean("testBean");
	}


	@Test
	public void testIsProxy() throws Exception {
		ITestBean bean = getTestBean();

		assertTrue("Bean is not a proxy", AopUtils.isAopProxy(bean));

		// check the advice details
		Advised advised = (Advised) bean;
		Advisor[] advisors = advised.getAdvisors();

		assertTrue("Advisors should not be empty", advisors.length > 0);
	}

	@Test
	public void testAdviceInvokedCorrectly() throws Exception {
		CountingBeforeAdvice getAgeCounter = (CountingBeforeAdvice) this.context.getBean("getAgeCounter");
		CountingBeforeAdvice getNameCounter = (CountingBeforeAdvice) this.context.getBean("getNameCounter");

		ITestBean bean = getTestBean();

		assertEquals("Incorrect initial getAge count", 0, getAgeCounter.getCalls("getAge"));
		assertEquals("Incorrect initial getName count", 0, getNameCounter.getCalls("getName"));

		bean.getAge();

		assertEquals("Incorrect getAge count on getAge counter", 1, getAgeCounter.getCalls("getAge"));
		assertEquals("Incorrect getAge count on getName counter", 0, getNameCounter.getCalls("getAge"));

		bean.getName();

		assertEquals("Incorrect getName count on getName counter", 1, getNameCounter.getCalls("getName"));
		assertEquals("Incorrect getName count on getAge counter", 0, getAgeCounter.getCalls("getName"));
	}

	@Test
	public void testAspectApplied() throws Exception {
		ITestBean bean = getTestBean();

		CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

		assertEquals("Incorrect before count", 0, advice.getBeforeCount());
		assertEquals("Incorrect after count", 0, advice.getAfterCount());

		bean.setName("Sally");

		assertEquals("Incorrect before count", 1, advice.getBeforeCount());
		assertEquals("Incorrect after count", 1, advice.getAfterCount());

		bean.getName();

		assertEquals("Incorrect before count", 1, advice.getBeforeCount());
		assertEquals("Incorrect after count", 1, advice.getAfterCount());
	}

	@Test
	public void testAspectAppliedForInitializeBean() {
		ITestBean bean = (ITestBean) this.context.getAutowireCapableBeanFactory().initializeBean(new TestBean(), null);

		CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

		assertEquals("Incorrect before count", 0, advice.getBeforeCount());
		assertEquals("Incorrect after count", 0, advice.getAfterCount());

		bean.setName("Sally");

		assertEquals("Incorrect before count", 1, advice.getBeforeCount());
		assertEquals("Incorrect after count", 1, advice.getAfterCount());

		bean.getName();

		assertEquals("Incorrect before count", 1, advice.getBeforeCount());
		assertEquals("Incorrect after count", 1, advice.getAfterCount());
	}
}


class CountingAspectJAdvice {

	private int beforeCount;

	private int afterCount;

	private int aroundCount;

	public void myBeforeAdvice() throws Throwable {
		this.beforeCount++;
	}

	public void myAfterAdvice() throws Throwable {
		this.afterCount++;
	}

	public void myAroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
		this.aroundCount++;
		pjp.proceed();
	}

	public void myAfterReturningAdvice(int age) {
		this.afterCount++;
	}

	public void myAfterThrowingAdvice(RuntimeException ex) {
		this.afterCount++;
	}

	public void mySetAgeAdvice(int newAge, ITestBean bean) {
		// no-op
	}

	public int getBeforeCount() {
		return this.beforeCount;
	}

	public int getAfterCount() {
		return this.afterCount;
	}

	public int getAroundCount() {
		return this.aroundCount;
	}

}
