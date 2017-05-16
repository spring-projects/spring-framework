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

package org.springframework.transaction.annotation;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class AnnotationTransactionNamespaceHandlerTests {

	private final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
			"org/springframework/transaction/annotation/annotationTransactionNamespaceHandlerTests.xml");

	@After
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void isProxy() throws Exception {
		TransactionalTestBean bean = getTestBean();
		assertTrue("testBean is not a proxy", AopUtils.isAopProxy(bean));
		Map<String, Object> services = this.context.getBeansWithAnnotation(Service.class);
		assertTrue("Stereotype annotation not visible", services.containsKey("testBean"));
	}

	@Test
	public void invokeTransactional() throws Exception {
		TransactionalTestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		// try with transactional
		assertEquals("Should not have any started transactions", 0, ptm.begun);
		testBean.findAllFoos();
		assertEquals("Should have 1 started transaction", 1, ptm.begun);
		assertEquals("Should have 1 committed transaction", 1, ptm.commits);

		// try with non-transaction
		testBean.doSomething();
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
	public void nonPublicMethodsNotAdvised() {
		TransactionalTestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		assertEquals("Should not have any started transactions", 0, ptm.begun);
		testBean.annotationsOnProtectedAreIgnored();
		assertEquals("Should not have any started transactions", 0, ptm.begun);
	}

	@Test
	public void mBeanExportAlsoWorks() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		assertEquals("done",
				server.invoke(ObjectName.getInstance("test:type=TestBean"), "doSomething", new Object[0], new String[0]));
	}

	@Test
	public void transactionalEventListenerRegisteredProperly() {
		assertTrue(this.context.containsBean(TransactionManagementConfigUtils
				.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME));
		assertEquals(1, this.context.getBeansOfType(TransactionalEventListenerFactory.class).size());
	}

	private TransactionalTestBean getTestBean() {
		return (TransactionalTestBean) context.getBean("testBean");
	}


	@Service
	@ManagedResource("test:type=TestBean")
	public static class TransactionalTestBean {

		@Transactional(readOnly = true)
		public Collection<?> findAllFoos() {
			return null;
		}

		@Transactional
		public void saveFoo() {
		}

		@Transactional("qualifiedTransactionManager")
		public void saveQualifiedFoo() {
		}

		@Transactional(transactionManager = "qualifiedTransactionManager")
		public void saveQualifiedFooWithAttributeAlias() {
		}

		@Transactional
		public void exceptional(Throwable t) throws Throwable {
			throw t;
		}

		@ManagedOperation
		public String doSomething() {
			return "done";
		}

		@Transactional
		protected void annotationsOnProtectedAreIgnored() {
		}
	}

}
