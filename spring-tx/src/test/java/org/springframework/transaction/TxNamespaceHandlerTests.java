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

package org.springframework.transaction;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Adrian Colyer
 */
public class TxNamespaceHandlerTests {

	private ApplicationContext context;

	private Method getAgeMethod;

	private Method setAgeMethod;


	@BeforeEach
	public void setup() throws Exception {
		this.context = new ClassPathXmlApplicationContext("txNamespaceHandlerTests.xml", getClass());
		this.getAgeMethod = ITestBean.class.getMethod("getAge");
		this.setAgeMethod = ITestBean.class.getMethod("setAge", int.class);
	}


	@Test
	public void isProxy() {
		ITestBean bean = getTestBean();
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
	}

	@Test
	public void invokeTransactional() {
		ITestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		// try with transactional
		assertThat(ptm.begun).as("Should not have any started transactions").isEqualTo(0);
		testBean.getName();
		assertThat(ptm.lastDefinition.isReadOnly()).isTrue();
		assertThat(ptm.begun).as("Should have 1 started transaction").isEqualTo(1);
		assertThat(ptm.commits).as("Should have 1 committed transaction").isEqualTo(1);

		// try with non-transaction
		testBean.haveBirthday();
		assertThat(ptm.begun).as("Should not have started another transaction").isEqualTo(1);

		// try with exceptional
		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				testBean.exceptional(new IllegalArgumentException("foo")));
		assertThat(ptm.begun).as("Should have another started transaction").isEqualTo(2);
		assertThat(ptm.rollbacks).as("Should have 1 rolled back transaction").isEqualTo(1);
	}

	@Test
	public void rollbackRules() {
		TransactionInterceptor txInterceptor = (TransactionInterceptor) context.getBean("txRollbackAdvice");
		TransactionAttributeSource txAttrSource = txInterceptor.getTransactionAttributeSource();
		TransactionAttribute txAttr = txAttrSource.getTransactionAttribute(getAgeMethod,ITestBean.class);
		assertThat(txAttr.rollbackOn(new Exception())).as("should be configured to rollback on Exception").isTrue();

		txAttr = txAttrSource.getTransactionAttribute(setAgeMethod, ITestBean.class);
		assertThat(txAttr.rollbackOn(new RuntimeException())).as("should not rollback on RuntimeException").isFalse();
	}

	private ITestBean getTestBean() {
		return (ITestBean) context.getBean("testBean");
	}

}
