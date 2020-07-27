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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test cases for AOP transaction management.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 23.04.2003
 */
public class BeanFactoryTransactionTests {

	private DefaultListableBeanFactory factory;


	@BeforeEach
	public void setUp() {
		this.factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(
				new ClassPathResource("transactionalBeanFactory.xml", getClass()));
	}


	@Test
	public void testGetsAreNotTransactionalWithProxyFactory1() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory1");
		assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
		boolean condition = testBean instanceof TransactionalProxy;
		assertThat(condition).isFalse();
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory2DynamicProxy() {
		this.factory.preInstantiateSingletons();
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2DynamicProxy");
		assertThat(Proxy.isProxyClass(testBean.getClass())).as("testBean is a dynamic proxy").isTrue();
		boolean condition = testBean instanceof TransactionalProxy;
		assertThat(condition).isTrue();
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory2Cglib() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Cglib");
		assertThat(AopUtils.isCglibProxy(testBean)).as("testBean is CGLIB advised").isTrue();
		boolean condition = testBean instanceof TransactionalProxy;
		assertThat(condition).isTrue();
		doTestGetsAreNotTransactional(testBean);
	}

	@Test
	public void testProxyFactory2Lazy() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory2Lazy");
		assertThat(factory.containsSingleton("target")).isFalse();
		assertThat(testBean.getAge()).isEqualTo(666);
		assertThat(factory.containsSingleton("target")).isTrue();
	}

	@Test
	public void testCglibTransactionProxyImplementsNoInterfaces() {
		ImplementsNoInterfaces ini = (ImplementsNoInterfaces) factory.getBean("cglibNoInterfaces");
		assertThat(AopUtils.isCglibProxy(ini)).as("testBean is CGLIB advised").isTrue();
		boolean condition = ini instanceof TransactionalProxy;
		assertThat(condition).isTrue();
		String newName = "Gordon";

		// Install facade
		CallCountingTransactionManager ptm = new CallCountingTransactionManager();
		PlatformTransactionManagerFacade.delegate = ptm;

		ini.setName(newName);
		assertThat(ini.getName()).isEqualTo(newName);
		assertThat(ptm.commits).isEqualTo(2);
	}

	@Test
	public void testGetsAreNotTransactionalWithProxyFactory3() {
		ITestBean testBean = (ITestBean) factory.getBean("proxyFactory3");
		boolean condition = testBean instanceof DerivedTestBean;
		assertThat(condition).as("testBean is a full proxy").isTrue();
		boolean condition1 = testBean instanceof TransactionalProxy;
		assertThat(condition1).isTrue();
		InvocationCounterPointcut txnCounter = (InvocationCounterPointcut) factory.getBean("txnInvocationCounterPointcut");
		InvocationCounterInterceptor preCounter = (InvocationCounterInterceptor) factory.getBean("preInvocationCounterInterceptor");
		InvocationCounterInterceptor postCounter = (InvocationCounterInterceptor) factory.getBean("postInvocationCounterInterceptor");
		txnCounter.counter = 0;
		preCounter.counter = 0;
		postCounter.counter = 0;
		doTestGetsAreNotTransactional(testBean);
		// Can't assert it's equal to 4 as the pointcut may be optimized and only invoked once
		assertThat(0 < txnCounter.counter && txnCounter.counter <= 4).isTrue();
		assertThat(preCounter.counter).isEqualTo(4);
		assertThat(postCounter.counter).isEqualTo(4);
	}

	private void doTestGetsAreNotTransactional(final ITestBean testBean) {
		// Install facade
		PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
		PlatformTransactionManagerFacade.delegate = ptm;

		assertThat(testBean.getAge() == 666).as("Age should not be " + testBean.getAge()).isTrue();

		// Expect no methods
		verifyNoInteractions(ptm);

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
				assertThat(status == ts).isTrue();
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
		assertThat(testBean.getAge() == age).isTrue();
	}

	@Test
	public void testGetBeansOfTypeWithAbstract() {
		Map<String, ITestBean> beansOfType = factory.getBeansOfType(ITestBean.class, true, true);
		assertThat(beansOfType).isNotNull();
	}

	/**
	 * Check that we fail gracefully if the user doesn't set any transaction attributes.
	 */
	@Test
	public void testNoTransactionAttributeSource() {
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() -> {
				DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
				new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource("noTransactionAttributeSource.xml", getClass()));
				bf.getBean("noTransactionAttributeSource");
		});
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
		assertThat(tb.getAge()).isEqualTo(666);
		int newAge = 557;
		tb.setAge(newAge);
		assertThat(tb.getAge()).isEqualTo(newAge);

		TestBean target2 = new TestBean();
		target2.setAge(65);
		HotSwappableTargetSource ts = (HotSwappableTargetSource) factory.getBean("swapper");
		ts.swap(target2);
		assertThat(tb.getAge()).isEqualTo(target2.getAge());
		tb.setAge(newAge);
		assertThat(target2.getAge()).isEqualTo(newAge);

		assertThat(txMan.inflight).isEqualTo(0);
		assertThat(txMan.commits).isEqualTo(2);
		assertThat(txMan.rollbacks).isEqualTo(0);
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
