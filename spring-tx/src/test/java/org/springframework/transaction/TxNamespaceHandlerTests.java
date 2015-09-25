/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.transaction;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Adrian Colyer
 */
public class TxNamespaceHandlerTests {

	private ApplicationContext context;

	private Method getAgeMethod;

	private Method setAgeMethod;


	@Before
	public void setUp() throws Exception {
		this.context = new ClassPathXmlApplicationContext("txNamespaceHandlerTests.xml", getClass());
		this.getAgeMethod = ITestBean.class.getMethod("getAge", new Class[0]);
		this.setAgeMethod = ITestBean.class.getMethod("setAge", new Class[] {int.class});
	}

	@Test
	public void isProxy() throws Exception {
		ITestBean bean = getTestBean();
		assertTrue("testBean is not a proxy", AopUtils.isAopProxy(bean));
	}

	@Test
	public void invokeTransactional() throws Exception {
		ITestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		// try with transactional
		assertEquals("Should not have any started transactions", 0, ptm.begun);
		testBean.getName();
		assertTrue(ptm.lastDefinition.isReadOnly());
		assertEquals("Should have 1 started transaction", 1, ptm.begun);
		assertEquals("Should have 1 committed transaction", 1, ptm.commits);

		// try with non-transaction
		testBean.haveBirthday();
		assertEquals("Should not have started another transaction", 1, ptm.begun);

		// try with exceptional
		try {
			testBean.exceptional(new IllegalArgumentException("foo"));
			fail("Should NEVER get here");
		}
		catch (Throwable throwable) {
			assertEquals("Should have another started transaction", 2, ptm.begun);
			assertEquals("Should have 1 rolled back transaction", 1, ptm.rollbacks);
		}
	}

	@Test
	public void rollbackRules() {
		TransactionInterceptor txInterceptor = (TransactionInterceptor) context.getBean("txRollbackAdvice");
		TransactionAttributeSource txAttrSource = txInterceptor.getTransactionAttributeSource();
		TransactionAttribute txAttr = txAttrSource.getTransactionAttribute(getAgeMethod,ITestBean.class);
		assertTrue("should be configured to rollback on Exception",txAttr.rollbackOn(new Exception()));

		txAttr = txAttrSource.getTransactionAttribute(setAgeMethod, ITestBean.class);
		assertFalse("should not rollback on RuntimeException",txAttr.rollbackOn(new RuntimeException()));
	}

	private ITestBean getTestBean() {
		return (ITestBean)context.getBean("testBean");
	}

}
