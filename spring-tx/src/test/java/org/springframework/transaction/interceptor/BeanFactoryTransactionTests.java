/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test cases for AOP transaction management.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 23.04.2003
 */
public class BeanFactoryTransactionTests {

	private DefaultListableBeanFactory factory;


	@Before
	public void setUp() {
		this.factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(
				new ClassPathResource("transactionalBeanFactory.xml", getClass()));
	}


	@Test
	public void testGetsAreNotTransactionalWithProxyFactory1() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory1");
		assertTrue("testBean is a dynamic proxy", Proxy.isProxyClass(testBean.getClass()));
		assertFalse(testBean instanceof TransactionalProxy);
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory2DynamicProxy() {
		this.factory.preInstantiateSingletons();
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2DynamicProxy");
		assertTrue("testBean is a dynamic proxy", Proxy.isProxyClass(testBean.getClass()));
		assertTrue(testBean instanceof TransactionalProxy);
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory2Cglib() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Cglib");
		assertTrue("testBean is CGLIB advised", AopUtils.isCglibProxy(testBean));
		assertTrue(testBean instanceof TransactionalProxy);
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testProxyFactory2Lazy() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Lazy");
		assertFalse(factory.containsSingleton("target"));
		assertEquals(666, testBean.getAge());
		assertTrue(factory.containsSingleton("target"));
	}

	@Test
	public void testCglibTransactionProxyImplementsNoInterfaces() {
		ImplementsNoInterfaces ini = (ImplementsNoInterfaces) factory.getBean("cglibNoInterfaces");
		assertTrue("testBean is CGLIB advised", AopUtils.isCglibProxy(ini));
		assertTrue(ini instanceof TransactionalProxy);
		String newName = "Gordon";

		// Install facade
		CallCountingTransactionManager ptm = new CallCountingTransactionManager();
		PlatformTransactionManagerFacade.delegate = ptm;

		ini.setName(newName);
		assertEquals(newName, ini.getName());
		assertEquals(2, ptm.commits);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory3() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory3");
		assertTrue("testBean is a full proxy", testBean instanceof DerivedTestBean);
		assertTrue(testBean instanceof TransactionalProxy);
		InvocationCounterPointcut txnCounter = (InvocationCounterPointcut) factory.getBean("txnInvocationCounterPointcut");
		InvocationCounterInterceptor preCounter = (InvocationCounterInterceptor) factory.getBean("preInvocationCounterInterceptor");
		InvocationCounterInterceptor postCounter = (InvocationCounterInterceptor) factory.getBean("postInvocationCounterInterceptor");
		txnCounter.counter = 0;
		preCounter.counter = 0;
		postCounter.counter = 0;
		doTestGetsAreNotTransactional(testBean);
		// Can't assert it's equal to 4 as the pointcut may be optimized and only invoked once
		assertTrue(0 < txnCounter.counter && txnCounter.counter <= 4);
		assertEquals(4, preCounter.counter);
		assertEquals(4, postCounter.counter);
	}

	private void doTestGetsAreNotTransactional(final ITestBean testBean) {
		// Install facade
		PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
		PlatformTransactionManagerFacade.delegate = ptm;

		assertTrue("Age should not be " + testBean.getAge(), testBean.getAge() == 666);

		// Expect no methods
		verifyZeroInteractions(ptm);

		// Install facade expecting a call
		final TransactionStatus ts = mock(TransactionStatus.class);
		ptm = new PlatformTransactionManager() {
			private boolean invoked;
			@Override
			public TransactionStatus getTransaction(@Nullable TransactionDefinition def) throws TransactionException {
				if (invoked) {
					throw new IllegalStateException("getTransaction should not get invoked more than once");
				}
				invoked = true;
				if (!(def.getName().contains(DerivedTestBean.class.getName()) && def.getName().contains("setAge"))) {
					throw new IllegalStateException(
							"transaction name should contain class and method name: " + def.getName());
				}
				return ts;
			}
			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				assertTrue(status == ts);
			}
			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				throw new IllegalStateException("rollback should not get invoked");
			}
		};
		PlatformTransactionManagerFacade.delegate = ptm;

		// TODO same as old age to avoid ordering effect for now
		int age = 666;
		testBean.setAge(age);
		assertTrue(testBean.getAge() == age);
	}

	@Test
	public void testGetBeansOfTypeWithAbstract() {
		Map<String, ITestBean> beansOfType = factory.getBeansOfType(ITestBean.class, true, true);
		assertNotNull(beansOfType);
	}

	/**
	 * Check that we fail gracefully if the user doesn't set any transaction attributes.
	 */
	@Test
	public void testNoTransactionAttributeSource() {
		try {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource("noTransactionAttributeSource.xml", getClass()));
			bf.getBean("noTransactionAttributeSource");
			fail("Should require TransactionAttributeSource to be set");
		}
		catch (FatalBeanException ex) {
			// Ok
		}
	}

	/**
	 * Test that we can set the target to a dynamic TargetSource.
	 */
	@Test
	public void testDynamicTargetSource() {
		// Install facade
		CallCountingTransactionManager txMan = new CallCountingTransactionManager();
		PlatformTransactionManagerFacade.delegate = txMan;

		TestBean tb = (TestBean) factory.getBean("hotSwapped");
		assertEquals(666, tb.getAge());
		int newAge = 557;
		tb.setAge(newAge);
		assertEquals(newAge, tb.getAge());

		TestBean target2 = new TestBean();
		target2.setAge(65);
		HotSwappableTargetSource ts = (HotSwappableTargetSource) factory.getBean("swapper");
		ts.swap(target2);
		assertEquals(target2.getAge(), tb.getAge());
		tb.setAge(newAge);
		assertEquals(newAge, target2.getAge());

		assertEquals(0, txMan.inflight);
		assertEquals(2, txMan.commits);
		assertEquals(0, txMan.rollbacks);
	}


	public static class InvocationCounterPointcut extends StaticMethodMatcherPointcut {

		int counter = 0;

		@Override
		public boolean matches(Method method, @Nullable Class<?> clazz) {
			counter++;
			return true;
		}
	}


	public static class InvocationCounterInterceptor implements MethodInterceptor {

		int counter = 0;

		@Override
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			counter++;
			return methodInvocation.proceed();
		}
	}

}
