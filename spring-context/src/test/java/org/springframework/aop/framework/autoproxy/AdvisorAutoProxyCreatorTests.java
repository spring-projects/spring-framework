/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.autoproxy.target.AbstractBeanFactoryBasedTargetSourceCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.CommonsPoolTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import test.advice.CountingBeforeAdvice;
import test.interceptor.NopInterceptor;
import test.mixin.Lockable;

/**
 * Tests for auto proxy creation by advisor recognition.
 *
 * @see org.springframework.aop.framework.autoproxy.AdvisorAutoProxyCreatorIntegrationTests;
 *
 * @author Rod Johnson
 * @author Dave Syer
 * @author Chris Beams
 */
public final class AdvisorAutoProxyCreatorTests {

	private static final Class<?> CLASS = AdvisorAutoProxyCreatorTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String DEFAULT_CONTEXT = CLASSNAME + "-context.xml";
	private static final String COMMON_INTERCEPTORS_CONTEXT = CLASSNAME + "-common-interceptors.xml";
	private static final String CUSTOM_TARGETSOURCE_CONTEXT = CLASSNAME + "-custom-targetsource.xml";
	private static final String QUICK_TARGETSOURCE_CONTEXT = CLASSNAME + "-quick-targetsource.xml";
	private static final String OPTIMIZED_CONTEXT = CLASSNAME + "-optimized.xml";

	private static final String ADVISOR_APC_BEAN_NAME = "aapc";
	private static final String TXMANAGER_BEAN_NAME = "txManager";

	/**
	 * Return a bean factory with attributes and EnterpriseServices configured.
	 */
	protected BeanFactory getBeanFactory() throws IOException {
		return new ClassPathXmlApplicationContext(DEFAULT_CONTEXT, CLASS);
	}

	/**
	 * Check that we can provide a common interceptor that will
	 * appear in the chain before "specific" interceptors,
	 * which are sourced from matching advisors
	 */
	@Test
	public void testCommonInterceptorAndAdvisor() throws Exception {
		BeanFactory bf = new ClassPathXmlApplicationContext(COMMON_INTERCEPTORS_CONTEXT, CLASS);
		ITestBean test1 = (ITestBean) bf.getBean("test1");
		assertTrue(AopUtils.isAopProxy(test1));

		Lockable lockable1 = (Lockable) test1;
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertEquals(0, nop.getCount());

		ITestBean test2 = (ITestBean) bf.getBean("test2");
		Lockable lockable2 = (Lockable) test2;

		// Locking should be independent; nop is shared
		assertFalse(lockable1.locked());
		assertFalse(lockable2.locked());
		// equals 2 calls on shared nop, because it's first
		// and sees calls against the Lockable interface introduced
		// by the specific advisor
		assertEquals(2, nop.getCount());
		lockable1.lock();
		assertTrue(lockable1.locked());
		assertFalse(lockable2.locked());
		assertEquals(5, nop.getCount());
	}

	/**
	 * We have custom TargetSourceCreators but there's no match, and
	 * hence no proxying, for this bean
	 */
	@Test
	public void testCustomTargetSourceNoMatch() throws Exception {
		BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
		ITestBean test = (ITestBean) bf.getBean("test");
		assertFalse(AopUtils.isAopProxy(test));
		assertEquals("Rod", test.getName());
		assertEquals("Kerry", test.getSpouse().getName());
	}

	@Test
	public void testCustomPrototypeTargetSource() throws Exception {
		CountingTestBean.count = 0;
		BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
		ITestBean test = (ITestBean) bf.getBean("prototypeTest");
		assertTrue(AopUtils.isAopProxy(test));
		Advised advised = (Advised) test;
		assertTrue(advised.getTargetSource() instanceof PrototypeTargetSource);
		assertEquals("Rod", test.getName());
		// Check that references survived prototype creation
		assertEquals("Kerry", test.getSpouse().getName());
		assertEquals("Only 2 CountingTestBeans instantiated", 2, CountingTestBean.count);
		CountingTestBean.count = 0;
	}

	@Test
	public void testLazyInitTargetSource() throws Exception {
		CountingTestBean.count = 0;
		BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
		ITestBean test = (ITestBean) bf.getBean("lazyInitTest");
		assertTrue(AopUtils.isAopProxy(test));
		Advised advised = (Advised) test;
		assertTrue(advised.getTargetSource() instanceof LazyInitTargetSource);
		assertEquals("No CountingTestBean instantiated yet", 0, CountingTestBean.count);
		assertEquals("Rod", test.getName());
		assertEquals("Kerry", test.getSpouse().getName());
		assertEquals("Only 1 CountingTestBean instantiated", 1, CountingTestBean.count);
		CountingTestBean.count = 0;
	}

	@Test
	public void testQuickTargetSourceCreator() throws Exception {
		ClassPathXmlApplicationContext bf =
				new ClassPathXmlApplicationContext(QUICK_TARGETSOURCE_CONTEXT, CLASS);
		ITestBean test = (ITestBean) bf.getBean("test");
		assertFalse(AopUtils.isAopProxy(test));
		assertEquals("Rod", test.getName());
		// Check that references survived pooling
		assertEquals("Kerry", test.getSpouse().getName());

		// Now test the pooled one
		test = (ITestBean) bf.getBean(":test");
		assertTrue(AopUtils.isAopProxy(test));
		Advised advised = (Advised) test;
		assertTrue(advised.getTargetSource() instanceof CommonsPoolTargetSource);
		assertEquals("Rod", test.getName());
		// Check that references survived pooling
		assertEquals("Kerry", test.getSpouse().getName());

		// Now test the ThreadLocal one
		test = (ITestBean) bf.getBean("%test");
		assertTrue(AopUtils.isAopProxy(test));
		advised = (Advised) test;
		assertTrue(advised.getTargetSource() instanceof ThreadLocalTargetSource);
		assertEquals("Rod", test.getName());
		// Check that references survived pooling
		assertEquals("Kerry", test.getSpouse().getName());

		// Now test the Prototype TargetSource
		test = (ITestBean) bf.getBean("!test");
		assertTrue(AopUtils.isAopProxy(test));
		advised = (Advised) test;
		assertTrue(advised.getTargetSource() instanceof PrototypeTargetSource);
		assertEquals("Rod", test.getName());
		// Check that references survived pooling
		assertEquals("Kerry", test.getSpouse().getName());


		ITestBean test2 = (ITestBean) bf.getBean("!test");
		assertFalse("Prototypes cannot be the same object", test == test2);
		assertEquals("Rod", test2.getName());
		assertEquals("Kerry", test2.getSpouse().getName());
		bf.close();
	}

	@Test
	public void testWithOptimizedProxy() throws Exception {
		BeanFactory beanFactory = new ClassPathXmlApplicationContext(OPTIMIZED_CONTEXT, CLASS);

		ITestBean testBean = (ITestBean) beanFactory.getBean("optimizedTestBean");
		assertTrue(AopUtils.isAopProxy(testBean));

		CountingBeforeAdvice beforeAdvice = (CountingBeforeAdvice) beanFactory.getBean("countingAdvice");

		testBean.setAge(23);
		testBean.getAge();

		assertEquals("Incorrect number of calls to proxy", 2, beforeAdvice.getCalls());
	}

}


class CountingTestBean extends TestBean {

	public static int count = 0;

	public CountingTestBean() {
		count++;
	}

}


class SelectivePrototypeTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {
		if (!beanName.startsWith("prototype")) {
			return null;
		}
		return new PrototypeTargetSource();
	}

}

