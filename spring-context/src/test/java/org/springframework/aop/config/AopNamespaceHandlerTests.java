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

package org.springframework.aop.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for aop namespace.
 *
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AopNamespaceHandlerTests {

	private ApplicationContext context;


	@BeforeEach
	public void setup() {
		this.context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}

	protected ITestBean getTestBean() {
		return (ITestBean) this.context.getBean("testBean");
	}


	@Test
	public void testIsProxy() throws Exception {
		ITestBean bean = getTestBean();

		assertThat(AopUtils.isAopProxy(bean)).as("Bean is not a proxy").isTrue();

		// check the advice details
		Advised advised = (Advised) bean;
		Advisor[] advisors = advised.getAdvisors();

		assertThat(advisors).as("Advisors should not be empty").isNotEmpty();
	}

	@Test
	public void testAdviceInvokedCorrectly() throws Exception {
		CountingBeforeAdvice getAgeCounter = (CountingBeforeAdvice) this.context.getBean("getAgeCounter");
		CountingBeforeAdvice getNameCounter = (CountingBeforeAdvice) this.context.getBean("getNameCounter");

		ITestBean bean = getTestBean();

		assertThat(getAgeCounter.getCalls("getAge")).as("Incorrect initial getAge count").isEqualTo(0);
		assertThat(getNameCounter.getCalls("getName")).as("Incorrect initial getName count").isEqualTo(0);

		bean.getAge();

		assertThat(getAgeCounter.getCalls("getAge")).as("Incorrect getAge count on getAge counter").isEqualTo(1);
		assertThat(getNameCounter.getCalls("getAge")).as("Incorrect getAge count on getName counter").isEqualTo(0);

		bean.getName();

		assertThat(getNameCounter.getCalls("getName")).as("Incorrect getName count on getName counter").isEqualTo(1);
		assertThat(getAgeCounter.getCalls("getName")).as("Incorrect getName count on getAge counter").isEqualTo(0);
	}

	@Test
	public void testAspectApplied() throws Exception {
		ITestBean bean = getTestBean();

		CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

		bean.setName("Sally");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

		bean.getName();

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
	}

	@Test
	public void testAspectAppliedForInitializeBeanWithEmptyName() {
		ITestBean bean = (ITestBean) this.context.getAutowireCapableBeanFactory().initializeBean(new TestBean(), "");

		CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

		bean.setName("Sally");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

		bean.getName();

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
	}

	@Test
	public void testAspectAppliedForInitializeBeanWithNullName() {
		ITestBean bean = (ITestBean) this.context.getAutowireCapableBeanFactory().initializeBean(new TestBean(), null);

		CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

		bean.setName("Sally");

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

		bean.getName();

		assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
		assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
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
