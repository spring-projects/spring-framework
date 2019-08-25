/*
 * Copyright 2002-2019 the original author or authors.
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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for correct application of the bean() PCD for &#64;AspectJ-based aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanNamePointcutAtAspectTests {

	private ITestBean testBean1;

	private ITestBean testBean3;

	private CounterAspect counterAspect;


	@org.junit.jupiter.api.BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		testBean1 = (ITestBean) ctx.getBean("testBean1");
		testBean3 = (ITestBean) ctx.getBean("testBean3");
	}


	@Test
	public void testMatchingBeanName() {
		boolean condition = testBean1 instanceof Advised;
		assertThat(condition).as("Expected a proxy").isTrue();

		// Call two methods to test for SPR-3953-like condition
		testBean1.setAge(20);
		testBean1.setName("");
		assertThat(counterAspect.count).isEqualTo(2);
	}

	@Test
	public void testNonMatchingBeanName() {
		boolean condition = testBean3 instanceof Advised;
		assertThat(condition).as("Didn't expect a proxy").isFalse();

		testBean3.setAge(20);
		assertThat(counterAspect.count).isEqualTo(0);
	}

	@Test
	public void testProgrammaticProxyCreation() {
		ITestBean testBean = new TestBean();

		AspectJProxyFactory factory = new AspectJProxyFactory();
		factory.setTarget(testBean);

		CounterAspect myCounterAspect = new CounterAspect();
		factory.addAspect(myCounterAspect);

		ITestBean proxyTestBean = factory.getProxy();

		boolean condition = proxyTestBean instanceof Advised;
		assertThat(condition).as("Expected a proxy").isTrue();
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
