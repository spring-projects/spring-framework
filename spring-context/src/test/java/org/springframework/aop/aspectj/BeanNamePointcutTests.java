/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for correct application of the bean() PCD for XML-based AspectJ aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class BeanNamePointcutTests {

	private ITestBean testBean1;
	private ITestBean testBean2;
	private ITestBean testBeanContainingNestedBean;
	private Map<?, ?> testFactoryBean1;
	private Map<?, ?> testFactoryBean2;
	private Counter counterAspect;

	private ITestBean interceptThis;
	private ITestBean dontInterceptThis;
	private TestInterceptor testInterceptor;

	private ClassPathXmlApplicationContext ctx;


	@BeforeEach
	void setup() {
		ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean1 = (ITestBean) ctx.getBean("testBean1");
		testBean2 = (ITestBean) ctx.getBean("testBean2");
		testBeanContainingNestedBean = (ITestBean) ctx.getBean("testBeanContainingNestedBean");
		testFactoryBean1 = (Map<?, ?>) ctx.getBean("testFactoryBean1");
		testFactoryBean2 = (Map<?, ?>) ctx.getBean("testFactoryBean2");
		counterAspect = (Counter) ctx.getBean("counterAspect");
		interceptThis = (ITestBean) ctx.getBean("interceptThis");
		dontInterceptThis = (ITestBean) ctx.getBean("dontInterceptThis");
		testInterceptor = (TestInterceptor) ctx.getBean("testInterceptor");

		counterAspect.reset();
	}


	// We don't need to test all combination of pointcuts due to BeanNamePointcutMatchingTests

	@Test
	void testMatchingBeanName() {
		boolean condition = this.testBean1 instanceof Advised;
		assertThat(condition).as("Matching bean must be advised (proxied)").isTrue();
		// Call two methods to test for SPR-3953-like condition
		this.testBean1.setAge(20);
		this.testBean1.setName("");
		assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
	}

	@Test
	void testNonMatchingBeanName() {
		boolean condition = this.testBean2 instanceof Advised;
		assertThat(condition).as("Non-matching bean must *not* be advised (proxied)").isFalse();
		this.testBean2.setAge(20);
		assertThat(this.counterAspect.getCount()).as("Advice must *not* have been executed").isEqualTo(0);
	}

	@Test
	void testNonMatchingNestedBeanName() {
		boolean condition = this.testBeanContainingNestedBean.getDoctor() instanceof Advised;
		assertThat(condition).as("Non-matching bean must *not* be advised (proxied)").isFalse();
	}

	@Test
	void testMatchingFactoryBeanObject() {
		boolean condition1 = this.testFactoryBean1 instanceof Advised;
		assertThat(condition1).as("Matching bean must be advised (proxied)").isTrue();
		assertThat(this.testFactoryBean1.get("myKey")).isEqualTo("myValue");
		assertThat(this.testFactoryBean1.get("myKey")).isEqualTo("myValue");
		assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
		FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean1");
		boolean condition = !(fb instanceof Advised);
		assertThat(condition).as("FactoryBean itself must *not* be advised").isTrue();
	}

	@Test
	void testMatchingFactoryBeanItself() {
		boolean condition1 = !(this.testFactoryBean2 instanceof Advised);
		assertThat(condition1).as("Matching bean must *not* be advised (proxied)").isTrue();
		FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean2");
		boolean condition = fb instanceof Advised;
		assertThat(condition).as("FactoryBean itself must be advised").isTrue();
		assertThat(Map.class.isAssignableFrom(fb.getObjectType())).isTrue();
		assertThat(Map.class.isAssignableFrom(fb.getObjectType())).isTrue();
		assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
	}

	@Test
	void testPointcutAdvisorCombination() {
		boolean condition = this.interceptThis instanceof Advised;
		assertThat(condition).as("Matching bean must be advised (proxied)").isTrue();
		boolean condition1 = this.dontInterceptThis instanceof Advised;
		assertThat(condition1).as("Non-matching bean must *not* be advised (proxied)").isFalse();
		interceptThis.setAge(20);
		assertThat(testInterceptor.interceptionCount).isEqualTo(1);
		dontInterceptThis.setAge(20);
		assertThat(testInterceptor.interceptionCount).isEqualTo(1);
	}


	public static class TestInterceptor implements MethodBeforeAdvice {

		private int interceptionCount;

		@Override
		public void before(Method method, Object[] args, @Nullable Object target) {
			interceptionCount++;
		}
	}

}
