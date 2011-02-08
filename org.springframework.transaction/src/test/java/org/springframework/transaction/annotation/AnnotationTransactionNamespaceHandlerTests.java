/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CallCountingTransactionManager;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AnnotationTransactionNamespaceHandlerTests extends TestCase {

	private ConfigurableApplicationContext context;

	public void setUp() {
		this.context = new ClassPathXmlApplicationContext(
				"org/springframework/transaction/annotation/annotationTransactionNamespaceHandlerTests.xml");
	}

	protected void tearDown() {
		this.context.close();
	}

	public void testIsProxy() throws Exception {
		TransactionalTestBean bean = getTestBean();
		assertTrue("testBean is not a proxy", AopUtils.isAopProxy(bean));
		Map services = this.context.getBeansWithAnnotation(Service.class);
		assertTrue("Stereotype annotation not visible", services.containsKey("testBean"));
	}

	public void testInvokeTransactional() throws Exception {
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
	
	public void testNonPublicMethodsNotAdvised() {
		TransactionalTestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		assertEquals("Should not have any started transactions", 0, ptm.begun);
		testBean.annotationsOnProtectedAreIgnored();
		assertEquals("Should not have any started transactions", 0, ptm.begun);		
	}

	public void testMBeanExportAlsoWorks() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		assertEquals("done",
				server.invoke(ObjectName.getInstance("test:type=TestBean"), "doSomething", new Object[0], new String[0]));
	}

	private TransactionalTestBean getTestBean() {
		return (TransactionalTestBean) context.getBean("testBean");
	}


	@Service
	@ManagedResource("test:type=TestBean")
	public static class TransactionalTestBean {

		@Transactional(readOnly = true)
		public Collection findAllFoos() {
			return null;
		}

		@Transactional
		public void saveFoo() {
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
