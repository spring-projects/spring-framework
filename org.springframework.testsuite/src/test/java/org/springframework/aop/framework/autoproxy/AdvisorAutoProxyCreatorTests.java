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

import java.io.IOException;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.CountingBeforeAdvice;
import org.springframework.aop.framework.Lockable;
import org.springframework.aop.framework.MethodCounter;
import org.springframework.aop.interceptor.NopInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.CommonsPoolTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;
import org.springframework.beans.ITestBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.CallCountingTransactionManager;

/**
 * Tests for auto proxy creation by advisor recognition.
 *
 * @author Rod Johnson
 */
public class AdvisorAutoProxyCreatorTests extends TestCase {

	private static final String ADVISOR_APC_BEAN_NAME = "aapc";

	private static final String TXMANAGER_BEAN_NAME = "txManager";

	/**
	 * Return a bean factory with attributes and EnterpriseServices configured.
	 */
	protected BeanFactory getBeanFactory() throws IOException {
		return new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/advisorAutoProxyCreator.xml");
	}

	public void testDefaultExclusionPrefix() throws Exception {
		DefaultAdvisorAutoProxyCreator aapc = (DefaultAdvisorAutoProxyCreator) getBeanFactory().getBean(ADVISOR_APC_BEAN_NAME);
		assertEquals(ADVISOR_APC_BEAN_NAME + DefaultAdvisorAutoProxyCreator.SEPARATOR, aapc.getAdvisorBeanNamePrefix());
		assertFalse(aapc.isUsePrefix());
	}

	/**
	 * If no pointcuts match (no attrs) there should be proxying.
	 */
	public void testNoProxy() throws Exception {
		BeanFactory bf = getBeanFactory();
		Object o = bf.getBean("noSetters");
		assertFalse(AopUtils.isAopProxy(o));
	}

	public void testTxIsProxied() throws Exception {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		assertTrue(AopUtils.isAopProxy(test));
	}

	public void testRegexpApplied() throws Exception {
		BeanFactory bf = getBeanFactory();
		ITestBean test = (ITestBean) bf.getBean("test");
		MethodCounter counter = (MethodCounter) bf.getBean("countingAdvice");
		assertEquals(0, counter.getCalls());
		test.getName();
		assertEquals(1, counter.getCalls());
	}

	/**
	 * Check that we can provide a common interceptor that will
	 * appear in the chain before "specific" interceptors,
	 * which are sourced from matching advisors
	 */
	public void testCommonInterceptorAndAdvisor() throws Exception {
		BeanFactory bf = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/advisorAutoProxyCreatorWithCommonInterceptors.xml");
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
	public void testCustomTargetSourceNoMatch() throws Exception {
		BeanFactory bf = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/customTargetSource.xml");
		ITestBean test = (ITestBean) bf.getBean("test");
		assertFalse(AopUtils.isAopProxy(test));
		assertEquals("Rod", test.getName());
		assertEquals("Kerry", test.getSpouse().getName());
	}

	public void testCustomPrototypeTargetSource() throws Exception {
		CountingTestBean.count = 0;
		BeanFactory bf = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/customTargetSource.xml");
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

	public void testLazyInitTargetSource() throws Exception {
		CountingTestBean.count = 0;
		BeanFactory bf = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/customTargetSource.xml");
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

	public void testQuickTargetSourceCreator() throws Exception {
		ClassPathXmlApplicationContext bf =
				new ClassPathXmlApplicationContext("/org/springframework/aop/framework/autoproxy/quickTargetSource.xml");
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
	
	/*
	public void testIntroductionIsProxied() throws Exception {
		BeanFactory bf = getBeanFactory();
		Object modifiable = bf.getBean("modifiable1");
		// We can tell it's a CGLIB proxy by looking at the class name
		System.out.println(modifiable.getClass().getName());
		assertFalse(modifiable.getClass().getName().equals(ModifiableTestBean.class.getName()));
	}
	*/

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

	public void testWithOptimizedProxy() throws Exception {
		BeanFactory beanFactory = new ClassPathXmlApplicationContext("org/springframework/aop/framework/autoproxy/optimizedAutoProxyCreator.xml");

		ITestBean testBean = (ITestBean) beanFactory.getBean("optimizedTestBean");
		assertTrue(AopUtils.isAopProxy(testBean));

		CountingBeforeAdvice beforeAdvice = (CountingBeforeAdvice) beanFactory.getBean("countingAdvice");

		testBean.setAge(23);
		testBean.getAge();

		assertEquals("Incorrect number of calls to proxy", 2, beforeAdvice.getCalls());
	}

	
	/**
	 * Tests an introduction pointcut. This is a prototype, so that it can add
	 * a Modifiable mixin. Tests that the autoproxy infrastructure can create
	 * advised objects with independent interceptor instances.
	 * The Modifiable behaviour of each instance of TestBean should be distinct.
	 */
	/*
	public void testIntroductionViaPrototype() throws Exception {
		BeanFactory bf = getBeanFactory();

		Object o = bf.getBean("modifiable1");
		ITestBean modifiable1 = (ITestBean) bf.getBean("modifiable1");
		ITestBean modifiable2 = (ITestBean) bf.getBean("modifiable2");
		
		Advised pc = (Advised) modifiable1;
		System.err.println(pc.toProxyConfigString());
		
		// For convenience only
		Modifiable mod1 = (Modifiable) modifiable1;  
		Modifiable mod2 = (Modifiable) modifiable2;  
		
		assertFalse(mod1.isModified());
		assertFalse(mod2.isModified());
		
		int newAge = 33;
		modifiable1.setAge(newAge);
		assertTrue(mod1.isModified());
		// Changes to one shouldn't have affected the other
		assertFalse("Instances of prototype introduction pointcut don't seem distinct", mod2.isModified());
		mod1.acceptChanges();
		assertFalse(mod1.isModified());
		assertEquals(modifiable1.getAge(), newAge);
		assertFalse(mod1.isModified());
	}
	*/

}
