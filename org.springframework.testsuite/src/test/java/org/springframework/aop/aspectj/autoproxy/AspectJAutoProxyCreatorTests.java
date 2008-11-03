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

package org.springframework.aop.aspectj.autoproxy;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.annotation.AspectMetadata;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.INestedTestBean;
import org.springframework.beans.ITestBean;
import org.springframework.beans.NestedTestBean;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StopWatch;

/**
 * Tests for AspectJ auto-proxying. Includes mixing with Spring AOP Advisors
 * to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class AspectJAutoProxyCreatorTests extends TestCase {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);

	public void testAspectsAreApplied() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspects.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertEquals(68, tb.getAge());
		MethodInvokingFactoryBean factoryBean = (MethodInvokingFactoryBean) bf.getBean("&factoryBean");
		assertTrue(AopUtils.isAopProxy(factoryBean.getTargetObject()));
		assertEquals(68, ((ITestBean) factoryBean.getTargetObject()).getAge());
	}

	public void testMultipleAspectsWithParameterApplied() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspects.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		tb.setAge(10);
		assertEquals(20, tb.getAge());
	}

	public void testAspectsAreAppliedInDefinedOrder() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsWithOrdering.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertEquals(71, tb.getAge());
	}

	public void testAspectsAndAdvisorAreApplied() {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsPlusAdvisor.xml");
		ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian");
		testAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
	}

	public void testAspectsAndAdvisorAppliedToPrototypeIsFastEnough() {
		if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
			// Skip this test: Trace logging blows the time limit.
			return;
		}
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsPlusAdvisor.xml");
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 10000; i++) {
			ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian2");
			if (i < 10) {
				testAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
			}
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 4000);
	}

	public void testAspectsAndAdvisorNotAppliedToPrototypeIsFastEnough() {
		if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
			// Skip this test: Trace logging blows the time limit.
			return;
		}
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsPlusAdvisor.xml");
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			INestedTestBean shouldNotBeWeaved = (INestedTestBean) ac.getBean("i21");
			if (i < 10) {
				assertFalse(AopUtils.isAopProxy(shouldNotBeWeaved));
			}
		}
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 3000);
	}

	public void testAspectsAndAdvisorNotAppliedToManySingletonsIsFastEnough() {
		if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
			// Skip this test: Trace logging blows the time limit.
			return;
		}
		GenericApplicationContext ac = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ac).loadBeanDefinitions(
				"/org/springframework/aop/aspectj/autoproxy/aspectsPlusAdvisor.xml");
		for (int i = 0; i < 10000; i++) {
			ac.registerBeanDefinition("singleton" + i, new RootBeanDefinition(NestedTestBean.class));
		}
		StopWatch sw = new StopWatch();
		sw.start("singleton");
		ac.refresh();
		sw.stop();
		System.out.println(sw.getTotalTimeMillis());
		assertTrue("Singleton creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 4000);
	}

	public void testAspectsAndAdvisorAreAppliedEvenIfComingFromParentFactory() {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsPlusAdvisor.xml");
		GenericApplicationContext childAc = new GenericApplicationContext(ac);
		// Create a child factory with a bean that should be weaved                                              
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().addPropertyValue(new PropertyValue("name", "Adrian")).
				addPropertyValue(new PropertyValue("age", new Integer(34)));
		childAc.registerBeanDefinition("adrian2", bd);
		// Register the advisor auto proxy creator with subclass
		childAc.registerBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class.getName(),
				new RootBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class));
		childAc.refresh();

		ITestBean beanFromChildContextThatShouldBeWeaved = (ITestBean) childAc.getBean("adrian2");
		//testAspectsAndAdvisorAreApplied(childAc, (ITestBean) ac.getBean("adrian"));
		testAspectsAndAdvisorAreApplied(childAc, beanFromChildContextThatShouldBeWeaved);
	}

	protected void testAspectsAndAdvisorAreApplied(ApplicationContext ac, ITestBean shouldBeWeaved) {
		TestBeanAdvisor tba = (TestBeanAdvisor) ac.getBean("advisor");

		MultiplyReturnValue mrv = (MultiplyReturnValue) ac.getBean("aspect");
		assertEquals(3, mrv.getMultiple());

		tba.count = 0;
		mrv.invocations = 0;

		assertTrue("Autoproxying must apply from @AspectJ aspect", AopUtils.isAopProxy(shouldBeWeaved));
		assertEquals("Adrian", shouldBeWeaved.getName());
		assertEquals(0, mrv.invocations);
		assertEquals(34 * mrv.getMultiple(), shouldBeWeaved.getAge());
		assertEquals("Spring advisor must be invoked", 2, tba.count);
		assertEquals("Must be able to hold state in aspect", 1, mrv.invocations);
	}

	public void testPerThisAspect() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/perthis.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian1));

		assertEquals(0, adrian1.getAge());
		assertEquals(1, adrian1.getAge());

		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertNotSame(adrian1, adrian2);
		assertTrue(AopUtils.isAopProxy(adrian1));
		assertEquals(0, adrian2.getAge());
		assertEquals(1, adrian2.getAge());
		assertEquals(2, adrian2.getAge());
		assertEquals(3, adrian2.getAge());
		assertEquals(2, adrian1.getAge());
	}

	public void testPerTargetAspect() throws SecurityException, NoSuchMethodException {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/pertarget.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian1));

		// Does not trigger advice or count
		int explicitlySetAge = 25;
		adrian1.setAge(explicitlySetAge);

		assertEquals("Setter does not initiate advice", explicitlySetAge, adrian1.getAge());
		// Fire aspect

		AspectMetadata am = new AspectMetadata(AbstractAspectJAdvisorFactoryTests.PerTargetAspect.class, "someBean");
		assertTrue(am.getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));

		adrian1.getSpouse();

		assertEquals("Advice has now been instantiated", 0, adrian1.getAge());
		adrian1.setAge(11);
		assertEquals("Any int setter increments", 2, adrian1.getAge());
		adrian1.setName("Adrian");
		//assertEquals("Any other setter does not increment", 2, adrian1.getAge());

		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertNotSame(adrian1, adrian2);
		assertTrue(AopUtils.isAopProxy(adrian1));
		assertEquals(34, adrian2.getAge());
		adrian2.getSpouse();
		assertEquals("Aspect now fired", 0, adrian2.getAge());
		assertEquals(1, adrian2.getAge());
		assertEquals(2, adrian2.getAge());
		assertEquals(3, adrian1.getAge());
	}

	public void testTwoAdviceAspectSingleton() {
		doTestTwoAdviceAspectWith("twoAdviceAspect.xml");
	}

	public void testTwoAdviceAspectPrototype() {
		doTestTwoAdviceAspectWith("twoAdviceAspectPrototype.xml");
	}

	private void doTestTwoAdviceAspectWith(String location) {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/" + location);

		boolean aspectSingleton = bf.isSingleton("aspect");
		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testPrototype(adrian1, 0);
		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertNotSame(adrian1, adrian2);
		testPrototype(adrian2, aspectSingleton ? 2 : 0);
	}

	public void testAdviceUsingJoinPoint() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/usesJoinPointAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		adrian1.getAge();
		AdviceUsingThisJoinPoint aspectInstance = (AdviceUsingThisJoinPoint) bf.getBean("aspect");
		//(AdviceUsingThisJoinPoint) Aspects.aspectOf(AdviceUsingThisJoinPoint.class);
		//assertEquals("method-execution(int TestBean.getAge())",aspectInstance.getLastMethodEntered());		
		assertTrue(aspectInstance.getLastMethodEntered().indexOf("TestBean.getAge())") != 0);
	}

	public void testIncludeMechanism() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/usesInclude.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian));
		assertEquals(68, adrian.getAge());
	}

	private void testPrototype(ITestBean adrian1, int start) {
		assertTrue(AopUtils.isAopProxy(adrian1));
		//TwoAdviceAspect twoAdviceAspect = (TwoAdviceAspect) bf.getBean(TwoAdviceAspect.class.getName());
		adrian1.setName("");
		assertEquals(start++, adrian1.getAge());
		int newAge = 32;
		adrian1.setAge(newAge);
		assertEquals(start++, adrian1.getAge());
		adrian1.setAge(0);
		assertEquals(start++, adrian1.getAge());
	}

	public void testForceProxyTargetClass() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsWithCGLIB.xml");

		ProxyConfig pc = (ProxyConfig) bf.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertTrue("should be proxying classes", pc.isProxyTargetClass());
	}

	public void testWithAbstractFactoryBeanAreApplied() {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/aspectsWithAbstractBean.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian));
		assertEquals(68, adrian.getAge());
	}

	public void testRetryAspect() throws Exception {
		ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(
				"/org/springframework/aop/aspectj/autoproxy/retryAspect.xml");
		UnreliableBean bean = (UnreliableBean) bf.getBean("unreliableBean");
		RetryAspect aspect = (RetryAspect) bf.getBean("retryAspect");
		int attempts = bean.unreliable();
		assertEquals(2, attempts);
		assertEquals(2, aspect.getBeginCalls());
		assertEquals(1, aspect.getRollbackCalls());
		assertEquals(1, aspect.getCommitCalls());
	}

}
