/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.tests.sample.beans.ITestBean;

import static org.junit.Assert.*;

/**
 * Test for correct application of the bean() PCD for XML-based AspectJ aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanNamePointcutTests {

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


	@Before
	public void setUp() {
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
	public void testMatchingBeanName() {
		assertTrue("Matching bean must be advised (proxied)", this.testBean1 instanceof Advised);
		// Call two methods to test for SPR-3953-like condition
		this.testBean1.setAge(20);
		this.testBean1.setName("");
		assertEquals("Advice not executed: must have been", 2, this.counterAspect.getCount());
	}

	@Test
	public void testNonMatchingBeanName() {
		assertFalse("Non-matching bean must *not* be advised (proxied)", this.testBean2 instanceof Advised);
		this.testBean2.setAge(20);
		assertEquals("Advice must *not* have been executed", 0, this.counterAspect.getCount());
	}

	@Test
	public void testNonMatchingNestedBeanName() {
		assertFalse("Non-matching bean must *not* be advised (proxied)", this.testBeanContainingNestedBean.getDoctor() instanceof Advised);
	}

	@Test
	public void testMatchingFactoryBeanObject() {
		assertTrue("Matching bean must be advised (proxied)", this.testFactoryBean1 instanceof Advised);
		assertEquals("myValue", this.testFactoryBean1.get("myKey"));
		assertEquals("myValue", this.testFactoryBean1.get("myKey"));
		assertEquals("Advice not executed: must have been", 2, this.counterAspect.getCount());
		FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean1");
		assertTrue("FactoryBean itself must *not* be advised", !(fb instanceof Advised));
	}

	@Test
	public void testMatchingFactoryBeanItself() {
		assertTrue("Matching bean must *not* be advised (proxied)", !(this.testFactoryBean2 instanceof Advised));
		FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean2");
		assertTrue("FactoryBean itself must be advised", fb instanceof Advised);
		assertTrue(Map.class.isAssignableFrom(fb.getObjectType()));
		assertTrue(Map.class.isAssignableFrom(fb.getObjectType()));
		assertEquals("Advice not executed: must have been", 2, this.counterAspect.getCount());
	}

	@Test
	public void testPointcutAdvisorCombination() {
		assertTrue("Matching bean must be advised (proxied)", this.interceptThis instanceof Advised);
		assertFalse("Non-matching bean must *not* be advised (proxied)", this.dontInterceptThis instanceof Advised);
		interceptThis.setAge(20);
		assertEquals(1, testInterceptor.interceptionCount);
		dontInterceptThis.setAge(20);
		assertEquals(1, testInterceptor.interceptionCount);
	}


	public static class TestInterceptor implements MethodBeforeAdvice {

		private int interceptionCount;

		@Override
		public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
			interceptionCount++;
		}
	}

}
