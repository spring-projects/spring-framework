/*
 * Copyright 2002-2012 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import test.advice.CountingBeforeAdvice;
import test.advice.MethodCounter;
import test.beans.ITestBean;
import test.interceptor.NopInterceptor;

/**
 * Integration tests for auto proxy creation by advisor recognition working in
 * conjunction with transaction managment resources.
 *
 * @see org.springframework.aop.framework.autoproxy.AdvisorAutoProxyCreatorTests;
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class AdvisorAutoProxyCreatorIntegrationTests {

	private static final Class<?> CLASS = AdvisorAutoProxyCreatorIntegrationTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String DEFAULT_CONTEXT = CLASSNAME + "-context.xml";

	private static final String ADVISOR_APC_BEAN_NAME = "aapc";
	private static final String TXMANAGER_BEAN_NAME = "txManager";

	/**
	 * Return a bean factory with attributes and EnterpriseServices configured.
	 */
	protected BeanFactory getBeanFactory() throws IOException {
		return new ClassPathXmlApplicationContext(DEFAULT_CONTEXT, CLASS);
	}

	@Test
	public void testDefaultExclusionPrefix() throws Exception {
		DefaultAdvisorAutoProxyCreator aapc = (DefaultAdvisorAutoProxyCreator) getBeanFactory().getBean(ADVISOR_APC_BEAN_NAME);
		assertEquals(ADVISOR_APC_BEAN_NAME + DefaultAdvisorAutoProxyCreator.SEPARATOR, aapc.getAdvisorBeanNamePrefix());
		assertFalse(aapc.isUsePrefix());
	}

	/**
	 * If no pointcuts match (no attrs) there should be proxying.
	 */
	@Test
	public void testNoProxy() throws Exception {
		BeanFactory bf = getBeanFactory();
		Object o = bf.getBean("noSetters");
		assertFalse(AopUtils.isAopProxy(o));
	}

	@Test
	public void testTxIsProxied() throws Exception {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		assertTrue(AopUtils.isAopProxy(test));
	}

	@Test
	public void testRegexpApplied() throws Exception {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		MethodCounter counter = (MethodCounter) bf.getBean("countingAdvice");
		assertEquals(0, counter.getCalls());
		test.getName();
		assertEquals(1, counter.getCalls());
	}

	@Test
	public void testTransactionAttributeOnMethod() throws Exception {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
		OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
		assertEquals(0, txc.getCountingBeforeAdvice().getCalls());

		assertEquals(0, txMan.commits);
		assertEquals("Initial value was correct", 4, test.getAge());
		int newAge = 5;
		test.setAge(newAge);
		assertEquals(1, txc.getCountingBeforeAdvice().getCalls());

		assertEquals("New value set correctly", newAge, test.getAge());
		assertEquals("Transaction counts match", 1, txMan.commits);
	}

	/**
	 * Should not roll back on servlet exception.
	 */
	@Test
	public void testRollbackRulesOnMethodCauseRollback() throws Exception {
		BeanFactory bf = getBeanFactory();
		Rollback rb = (Rollback) bf.getBean("rollback");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);
		OrderedTxCheckAdvisor txc = (OrderedTxCheckAdvisor) bf.getBean("orderedBeforeTransaction");
		assertEquals(0, txc.getCountingBeforeAdvice().getCalls());

		assertEquals(0, txMan.commits);
		rb.echoException(null);
		// Fires only on setters
		assertEquals(0, txc.getCountingBeforeAdvice().getCalls());
		assertEquals("Transaction counts match", 1, txMan.commits);

		assertEquals(0, txMan.rollbacks);
		Exception ex = new Exception();
		try {
			rb.echoException(ex);
		}
		catch (Exception actual) {
			assertEquals(ex, actual);
		}
		assertEquals("Transaction counts match", 1, txMan.rollbacks);
	}

	@Test
	public void testRollbackRulesOnMethodPreventRollback() throws Exception {
		BeanFactory bf = getBeanFactory();
		Rollback rb = (Rollback) bf.getBean("rollback");

		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

		assertEquals(0, txMan.commits);
		// Should NOT roll back on ServletException
		try {
			rb.echoException(new ServletException());
		}
		catch (ServletException ex) {

		}
		assertEquals("Transaction counts match", 1, txMan.commits);
	}

	@Test
	public void testProgrammaticRollback() throws Exception {
		BeanFactory bf = getBeanFactory();

		Object bean = bf.getBean(TXMANAGER_BEAN_NAME);
		assertTrue(bean instanceof CallCountingTransactionManager);
		CallCountingTransactionManager txMan = (CallCountingTransactionManager) bf.getBean(TXMANAGER_BEAN_NAME);

		Rollback rb = (Rollback) bf.getBean("rollback");
		assertEquals(0, txMan.commits);
		rb.rollbackOnly(false);
		assertEquals("Transaction counts match", 1, txMan.commits);
		assertEquals(0, txMan.rollbacks);
		// Will cause rollback only
		rb.rollbackOnly(true);
		assertEquals(1, txMan.rollbacks);
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
	public boolean matches(Method m, Class<?> targetClass) {
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
	public void afterPropertiesSet() throws Exception {
		setAdvice(new TxCountingBeforeAdvice());
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
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
	 * @param rollbackOnly
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
		if (ex != null)
			throw ex;
	}

}


@SuppressWarnings("serial")
class CallCountingTransactionManager extends AbstractPlatformTransactionManager {

	public TransactionDefinition lastDefinition;
	public int begun;
	public int commits;
	public int rollbacks;
	public int inflight;

	@Override
	protected Object doGetTransaction() {
		return new Object();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		this.lastDefinition = definition;
		++begun;
		++inflight;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		++commits;
		--inflight;
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		++rollbacks;
		--inflight;
	}

	public void clear() {
		begun = commits = rollbacks = inflight = 0;
	}

}
