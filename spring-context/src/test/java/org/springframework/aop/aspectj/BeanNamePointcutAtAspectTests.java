/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for correct application of the bean() PCD for &#64;AspectJ-based aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class BeanNamePointcutAtAspectTests {

	private ClassPathXmlApplicationContext ctx;

	private ITestBean testBean1;

	private ITestBean testBean3;

	private CounterAspect counterAspect;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		testBean1 = (ITestBean) ctx.getBean("testBean1");
		testBean3 = (ITestBean) ctx.getBean("testBean3");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}



	@Test
	void matchingBeanName() {
		assertThat(testBean1).as("Expected a proxy").isInstanceOf(Advised.class);

		// Call two methods to test for SPR-3953-like condition
		testBean1.setAge(20);
		testBean1.setName("");
		assertThat(counterAspect.count).isEqualTo(2);
	}

	@Test
	void nonMatchingBeanName() {
		assertThat(testBean3).as("Didn't expect a proxy").isNotInstanceOf(Advised.class);

		testBean3.setAge(20);
		assertThat(counterAspect.count).isEqualTo(0);
	}

	@Test
	void programmaticProxyCreation() {
		ITestBean testBean = new TestBean();

		AspectJProxyFactory factory = new AspectJProxyFactory();
		factory.setTarget(testBean);

		CounterAspect myCounterAspect = new CounterAspect();
		factory.addAspect(myCounterAspect);

		ITestBean proxyTestBean = factory.getProxy();

		assertThat(proxyTestBean).as("Expected a proxy").isInstanceOf(Advised.class);
		proxyTestBean.setAge(20);
		assertThat(myCounterAspect.count).as("Programmatically created proxy shouldn't match bean()").isEqualTo(0);
	}

}


@Aspect
class CounterAspect {

	int count;

	@Before("execution(* set*(..)) && bean(testBean1)")
	public void increment1ForAnonymousPointcut() {
		count++;
	}

}
