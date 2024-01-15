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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.aop.testfixture.advice.MethodCounter;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for auto proxy creation by advisor recognition working in
 * conjunction with transaction management resources.
 *
 * @see org.springframework.aop.framework.autoproxy.AdvisorAutoProxyCreatorTests
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
class AdvisorAutoProxyCreatorIntegrationTests {

	private static final Class<?> CLASS = AdvisorAutoProxyCreatorIntegrationTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String DEFAULT_CONTEXT = CLASSNAME + "-context.xml";

	private static final String ADVISOR_APC_BEAN_NAME = "aapc";
	private static final String TXMANAGER_BEAN_NAME = "txManager";

	/**
	 * Return a bean factory with attributes and EnterpriseServices configured.
	 */
	protected BeanFactory getBeanFactory() {
		return new ClassPathXmlApplicationContext(DEFAULT_CONTEXT, CLASS);
	}

	@Test
	void testDefaultExclusionPrefix() {
		DefaultAdvisorAutoProxyCreator aapc = (DefaultAdvisorAutoProxyCreator) getBeanFactory().getBean(ADVISOR_APC_BEAN_NAME);
		assertThat(aapc.getAdvisorBeanNamePrefix()).isEqualTo((ADVISOR_APC_BEAN_NAME + DefaultAdvisorAutoProxyCreator.SEPARATOR));
		assertThat(aapc.isUsePrefix()).isFalse();
	}

	/**
	 * If no pointcuts match (no attrs) there should be proxying.
	 */
	@Test
	void testNoProxy() {
		BeanFactory bf = getBeanFactory();
		Object o = bf.getBean("noSetters");
		assertThat(AopUtils.isAopProxy(o)).isFalse();
	}

	@Test
	void testTxIsProxied() {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		assertThat(AopUtils.isAopProxy(test)).isTrue();
	}

	@Test
	void testRegexpApplied() {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		MethodCounter counter = (MethodCounter) bf.getBean("countingAdvice");
		assertThat(counter.getCalls()).isEqualTo(0);
		test.getName();
		assertThat(counter.getCalls()).isEqualTo(1);
	}

	@Test
	void testTransactionAttributeOnMethod() {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
		OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
		assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);

		assertThat(txMan.commits).isEqualTo(0);
		assertThat(test.getAge()).as("Initial value was correct").isEqualTo(4);
		int newAge = 5;
		test.setAge(newAge);
		assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(1);

		assertThat(test.getAge()).as("New value set correctly").isEqualTo(newAge);
		assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
	}

	/**
	 * Should not roll back on servlet exception.
	 */
	@Test
	void testRollbackRulesOnMethodCauseRollback() throws Exception {
		BeanFactory bf = getBeanFactory();
		Rollback rb = (Rollback) bf.getBean("rollback");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
		OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
		assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);

		assertThat(txMan.commits).isEqualTo(0);
		rb.echoException(null);
		// Fires only on setters
		assertThat(txc.getCountingBeforeAdvice().getCalls()).isEqualTo(0);
		assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);

		assertThat(txMan.rollbacks).isEqualTo(0);
		Exception ex = new Exception();
		try {
			rb.echoException(ex);
		}
		catch (Exception actual) {
			assertThat(actual).isEqualTo(ex);
		}
		assertThat(txMan.rollbacks).as("Transaction counts match").isEqualTo(1);
	}

	@Test
	void testRollbackRulesOnMethodPreventRollback() throws Exception {
		BeanFactory bf = getBeanFactory();
		Rollback rb = (Rollback) bf.getBean("rollback");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

		assertThat(txMan.commits).isEqualTo(0);
		// Should NOT roll back on ServletException
		try {
			rb.echoException(new ServletException());
		}
		catch (ServletException ex) {

		}
		assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
	}

	@Test
	void testProgrammaticRollback() {
		BeanFactory bf = getBeanFactory();

		Object bean = bf.getBean(TXMANAGER_BEAN_NAME);
		boolean condition = bean instanceof CallCountingTransactionManager;
		assertThat(condition).isTrue();
		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

		Rollback rb = (Rollback) bf.getBean("rollback");
		assertThat(txMan.commits).isEqualTo(0);
		rb.rollbackOnly(false);
		assertThat(txMan.commits).as("Transaction counts match").isEqualTo(1);
		assertThat(txMan.rollbacks).isEqualTo(0);
		// Will cause rollback only
		rb.rollbackOnly(true);
		assertThat(txMan.rollbacks).isEqualTo(1);
	}

}


@SuppressWarnings("serial")
class NeverMatchAdvisor extends StaticMethodMatcherPointcutAdvisor {

	public NeverMatchAdvisor() {
		super(new NopInterceptor());
	}

	/**
	 * This method is solely to allow us to create a mixture of dependencies in
	 * the bean definitions. The dependencies don't have any meaning, and don't
	 * <b>do</b> anything.
	 */
	public void setDependencies(List<?> l) {

	}

	/**
	 * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	public boolean matches(Method m, @Nullable Class<?> targetClass) {
		return false;
	}

}


class NoSetters {

	public void A() {

	}

	public int getB() {
		return -1;
	}

}


@SuppressWarnings("serial")
class OrderedTxCheckAdvisor extends StaticMethodMatcherPointcutAdvisor implements InitializingBean {

	/**
	 * Should we insist on the presence of a transaction attribute or refuse to accept one?
	 */
	private boolean requireTransactionContext = false;


	public void setRequireTransactionContext(boolean requireTransactionContext) {
		this.requireTransactionContext = requireTransactionContext;
	}

	public boolean isRequireTransactionContext() {
		return requireTransactionContext;
	}


	public CountingBeforeAdvice getCountingBeforeAdvice() {
		return (CountingBeforeAdvice) getAdvice();
	}

	@Override
	public void afterPropertiesSet() {
		setAdvice(new TxCountingBeforeAdvice());
	}

	@Override
	public boolean matches(Method method, @Nullable Class<?> targetClass) {
		return method.getName().startsWith("setAge");
	}


	private class TxCountingBeforeAdvice extends CountingBeforeAdvice {

		@Override
		public void before(Method method, Object[] args, Object target) throws Throwable {
			// do transaction checks
			if (requireTransactionContext) {
				TransactionInterceptor.currentTransactionStatus();
			}
			else {
				try {
					TransactionInterceptor.currentTransactionStatus();
					throw new RuntimeException("Shouldn't have a transaction");
				}
				catch (NoTransactionException ex) {
					// this is Ok
				}
			}
			super.before(method, args, target);
		}
	}

}


class Rollback {

	/**
	 * Inherits transaction attribute.
	 * Illustrates programmatic rollback.
	 */
	public void rollbackOnly(boolean rollbackOnly) {
		if (rollbackOnly) {
			setRollbackOnly();
		}
	}

	/**
	 * Extracted in a protected method to facilitate testing
	 */
	protected void setRollbackOnly() {
		TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
	}

	/**
	 * @org.springframework.transaction.interceptor.RuleBasedTransaction ( timeout=-1 )
	 * @org.springframework.transaction.interceptor.RollbackRule ( "java.lang.Exception" )
	 * @org.springframework.transaction.interceptor.NoRollbackRule ( "ServletException" )
	 */
	public void echoException(Exception ex) throws Exception {
		if (ex != null) {
			throw ex;
		}
	}

}
