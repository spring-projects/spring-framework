/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for {@link AopNamespaceHandler}.
 *
 * @author Rob Harrop
 * @author Chris Beams
 */
class AopNamespaceHandlerTests {

	private ApplicationContext context;


	@BeforeEach
	void setup() {
		this.context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}

	protected ITestBean getTestBean() {
		return (ITestBean) this.context.getBean("testBean");
	}


	@Test
	void isProxy() {
		ITestBean bean = getTestBean();

		assertThat(AopUtils.isAopProxy(bean)).as("Bean is not a proxy").isTrue();

		// check the advice details
		Advised advised = (Advised) bean;
		Advisor[] advisors = advised.getAdvisors();

		assertThat(advisors).as("Advisors should not be empty").isNotEmpty();
	}

	@Test
	void adviceInvokedCorrectly() {
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
	void aspectApplied() {
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
	void aspectAppliedForInitializeBeanWithEmptyName() {
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
	void aspectAppliedForInitializeBeanWithNullName() {
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

	public void myBeforeAdvice() {
		this.beforeCount++;
	}

	public void myAfterAdvice() {
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
