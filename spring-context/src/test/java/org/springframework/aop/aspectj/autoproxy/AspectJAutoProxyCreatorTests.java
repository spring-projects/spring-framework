/*
 * Copyright 2002-2013 the original author or authors.
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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.annotation.AspectMetadata;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.tests.sample.beans.INestedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.NestedTestBean;
import org.springframework.beans.PropertyValue;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StopWatch;

/**
 * Integration tests for AspectJ auto-proxying. Includes mixing with Spring AOP Advisors
 * to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public final class AspectJAutoProxyCreatorTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);

	private static void assertStopWatchTimeLimit(final StopWatch sw, final long maxTimeMillis) {
		final long totalTimeMillis = sw.getTotalTimeMillis();
		assertTrue("'" + sw.getLastTaskName() + "' took too long: expected less than<" + maxTimeMillis
				+ "> ms, actual<" + totalTimeMillis + "> ms.", totalTimeMillis < maxTimeMillis);
	}

	@Test
	public void testAspectsAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertEquals(68, tb.getAge());
		MethodInvokingFactoryBean factoryBean = (MethodInvokingFactoryBean) bf.getBean("&factoryBean");
		assertTrue(AopUtils.isAopProxy(factoryBean.getTargetObject()));
		assertEquals(68, ((ITestBean) factoryBean.getTargetObject()).getAge());
	}

	@Test
	public void testMultipleAspectsWithParameterApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		tb.setAge(10);
		assertEquals(20, tb.getAge());
	}

	@Test
	public void testAspectsAreAppliedInDefinedOrder() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithOrdering.xml");
		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertEquals(71, tb.getAge());
	}

	@Test
	public void testAspectsAndAdvisorAreApplied() {
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");
		ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian");
		doTestAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
	}

	@Test
	public void testAspectsAndAdvisorAppliedToPrototypeIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");
		StopWatch sw = new StopWatch();
		sw.start("Prototype Creation");
		for (int i = 0; i < 10000; i++) {
			ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian2");
			if (i < 10) {
				doTestAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
			}
		}
		sw.stop();

		// What's a reasonable expectation for _any_ server or developer machine load?
		// 9 seconds?
		assertStopWatchTimeLimit(sw, 9000);
	}

	@Test
	public void testAspectsAndAdvisorNotAppliedToPrototypeIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");
		StopWatch sw = new StopWatch();
		sw.start("Prototype Creation");
		for (int i = 0; i < 100000; i++) {
			INestedTestBean shouldNotBeWeaved = (INestedTestBean) ac.getBean("i21");
			if (i < 10) {
				assertFalse(AopUtils.isAopProxy(shouldNotBeWeaved));
			}
		}
		sw.stop();

		// What's a reasonable expectation for _any_ server or developer machine load?
		// 3 seconds?
		assertStopWatchTimeLimit(sw, 6000);
	}

	@Test
	public void testAspectsAndAdvisorNotAppliedToManySingletonsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		GenericApplicationContext ac = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ac).loadBeanDefinitions(new ClassPathResource(qName("aspectsPlusAdvisor.xml"),
				getClass()));
		for (int i = 0; i < 10000; i++) {
			ac.registerBeanDefinition("singleton" + i, new RootBeanDefinition(NestedTestBean.class));
		}
		StopWatch sw = new StopWatch();
		sw.start("Singleton Creation");
		ac.refresh();
		sw.stop();

		// What's a reasonable expectation for _any_ server or developer machine load?
		// 8 seconds?
		assertStopWatchTimeLimit(sw, 8000);
	}

	@Test
	public void testAspectsAndAdvisorAreAppliedEvenIfComingFromParentFactory() {
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");
		GenericApplicationContext childAc = new GenericApplicationContext(ac);
		// Create a child factory with a bean that should be woven
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().addPropertyValue(new PropertyValue("name", "Adrian"))
				.addPropertyValue(new PropertyValue("age", new Integer(34)));
		childAc.registerBeanDefinition("adrian2", bd);
		// Register the advisor auto proxy creator with subclass
		childAc.registerBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class.getName(), new RootBeanDefinition(
				AnnotationAwareAspectJAutoProxyCreator.class));
		childAc.refresh();

		ITestBean beanFromChildContextThatShouldBeWeaved = (ITestBean) childAc.getBean("adrian2");
		//testAspectsAndAdvisorAreApplied(childAc, (ITestBean) ac.getBean("adrian"));
		doTestAspectsAndAdvisorAreApplied(childAc, beanFromChildContextThatShouldBeWeaved);
	}

	protected void doTestAspectsAndAdvisorAreApplied(ApplicationContext ac, ITestBean shouldBeWeaved) {
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

	@Test
	public void testPerThisAspect() {
		ClassPathXmlApplicationContext bf = newContext("perthis.xml");

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

	@Test
	public void testPerTargetAspect() throws SecurityException, NoSuchMethodException {
		ClassPathXmlApplicationContext bf = newContext("pertarget.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian1));

		// Does not trigger advice or count
		int explicitlySetAge = 25;
		adrian1.setAge(explicitlySetAge);

		assertEquals("Setter does not initiate advice", explicitlySetAge, adrian1.getAge());
		// Fire aspect

		AspectMetadata am = new AspectMetadata(PerTargetAspect.class, "someBean");
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

	@Test
	public void testTwoAdviceAspectSingleton() {
		doTestTwoAdviceAspectWith("twoAdviceAspect.xml");
	}

	@Test
	public void testTwoAdviceAspectPrototype() {
		doTestTwoAdviceAspectWith("twoAdviceAspectPrototype.xml");
	}

	private void doTestTwoAdviceAspectWith(String location) {
		ClassPathXmlApplicationContext bf = newContext(location);

		boolean aspectSingleton = bf.isSingleton("aspect");
		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testPrototype(adrian1, 0);
		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertNotSame(adrian1, adrian2);
		testPrototype(adrian2, aspectSingleton ? 2 : 0);
	}

	@Test
	public void testAdviceUsingJoinPoint() {
		ClassPathXmlApplicationContext bf = newContext("usesJoinPointAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		adrian1.getAge();
		AdviceUsingThisJoinPoint aspectInstance = (AdviceUsingThisJoinPoint) bf.getBean("aspect");
		//(AdviceUsingThisJoinPoint) Aspects.aspectOf(AdviceUsingThisJoinPoint.class);
		//assertEquals("method-execution(int TestBean.getAge())",aspectInstance.getLastMethodEntered());
		assertTrue(aspectInstance.getLastMethodEntered().indexOf("TestBean.getAge())") != 0);
	}

	@Test
	public void testIncludeMechanism() {
		ClassPathXmlApplicationContext bf = newContext("usesInclude.xml");

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

	@Test
	public void testForceProxyTargetClass() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithCGLIB.xml");

		ProxyConfig pc = (ProxyConfig) bf.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertTrue("should be proxying classes", pc.isProxyTargetClass());
		assertTrue("should expose proxy", pc.isExposeProxy());
	}

	@Test
	public void testWithAbstractFactoryBeanAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithAbstractBean.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertTrue(AopUtils.isAopProxy(adrian));
		assertEquals(68, adrian.getAge());
	}

	@Test
	public void testRetryAspect() throws Exception {
		ClassPathXmlApplicationContext bf = newContext("retryAspect.xml");
		UnreliableBean bean = (UnreliableBean) bf.getBean("unreliableBean");
		RetryAspect aspect = (RetryAspect) bf.getBean("retryAspect");
		int attempts = bean.unreliable();
		assertEquals(2, attempts);
		assertEquals(2, aspect.getBeginCalls());
		assertEquals(1, aspect.getRollbackCalls());
		assertEquals(1, aspect.getCommitCalls());
	}

	/**
	 * Returns a new {@link ClassPathXmlApplicationContext} for the file ending in <var>fileSuffix</var>.
	 */
	private ClassPathXmlApplicationContext newContext(String fileSuffix) {
		return new ClassPathXmlApplicationContext(qName(fileSuffix), getClass());
	}

	/**
	 * Returns the relatively qualified name for <var>fileSuffix</var>.
	 * e.g. for a fileSuffix='foo.xml', this method will return
	 * 'AspectJAutoProxyCreatorTests-foo.xml'
	 */
	private String qName(String fileSuffix) {
		return format("%s-%s", getClass().getSimpleName(), fileSuffix);
	}

}

@Aspect("pertarget(execution(* *.getSpouse()))")
class PerTargetAspect implements Ordered {

	public int count;

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Around("execution(int *.getAge())")
	public int returnCountAsAge() {
		return count++;
	}

	@Before("execution(void *.set*(int))")
	public void countSetter() {
		++count;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}

@Aspect
class AdviceUsingThisJoinPoint {

	private String lastEntry = "";

	public String getLastMethodEntered() {
		return this.lastEntry;
	}

	@Pointcut("execution(* *(..))")
	public void methodExecution() {
	}

	@Before("methodExecution()")
	public void entryTrace(JoinPoint jp) {
		this.lastEntry = jp.toString();
	}

}

@Aspect
class DummyAspect {

	@Around("execution(* setAge(int))")
	public Object test(ProceedingJoinPoint pjp) throws Throwable {
		return pjp.proceed();
	}

}

@Aspect
class DummyAspectWithParameter {

	@Around("execution(* setAge(int)) && args(age)")
	public Object test(ProceedingJoinPoint pjp, int age) throws Throwable {
		return pjp.proceed();
	}

}

class DummyFactoryBean implements FactoryBean<Object> {

	@Override
	public Object getObject() throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getObjectType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSingleton() {
		throw new UnsupportedOperationException();
	}

}

@Aspect
@Order(10)
class IncreaseReturnValue {

	@Around("execution(int *.getAge())")
	public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
		int result = (Integer) pjp.proceed();
		return result + 3;
	}

}

@Aspect
class MultiplyReturnValue {

	private int multiple = 2;

	public int invocations;

	public void setMultiple(int multiple) {
		this.multiple = multiple;
	}

	public int getMultiple() {
		return this.multiple;
	}

	@Around("execution(int *.getAge())")
	public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
		++this.invocations;
		int result = (Integer) pjp.proceed();
		return result * this.multiple;
	}

}

@Aspect
class RetryAspect {

	private int beginCalls;

	private int commitCalls;

	private int rollbackCalls;

	@Pointcut("execution(public * UnreliableBean.*(..))")
	public void execOfPublicMethod() {
	}

	/**
	 * Retry Advice
	 */
	@Around("execOfPublicMethod()")
	public Object retry(ProceedingJoinPoint jp) throws Throwable {
		boolean retry = true;
		Object o = null;
		while (retry) {
			try {
				retry = false;
				this.beginCalls++;
				try {
					o = jp.proceed();
					this.commitCalls++;
				} catch (RetryableException e) {
					this.rollbackCalls++;
					throw e;
				}
			} catch (RetryableException re) {
				retry = true;
			}
		}
		return o;
	}

	public int getBeginCalls() {
		return this.beginCalls;
	}

	public int getCommitCalls() {
		return this.commitCalls;
	}

	public int getRollbackCalls() {
		return this.rollbackCalls;
	}

}

@SuppressWarnings("serial")
class RetryableException extends NestedRuntimeException {

	public RetryableException(String msg) {
		super(msg);
	}

	public RetryableException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

class UnreliableBean {

	private int calls;

	public int unreliable() {
		this.calls++;
		if (this.calls % 2 != 0) {
			throw new RetryableException("foo");
		}
		return this.calls;
	}

}

@SuppressWarnings("serial")
class TestBeanAdvisor extends StaticMethodMatcherPointcutAdvisor {

	public int count;

	public TestBeanAdvisor() {
		setAdvice(new MethodBeforeAdvice() {
			@Override
			public void before(Method method, Object[] args, Object target) throws Throwable {
				++count;
			}
		});
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return ITestBean.class.isAssignableFrom(targetClass);
	}

}
