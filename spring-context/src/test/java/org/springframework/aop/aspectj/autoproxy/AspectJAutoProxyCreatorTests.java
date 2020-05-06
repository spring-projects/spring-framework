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

package org.springframework.aop.aspectj.autoproxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.annotation.AspectMetadata;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.INestedTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.Assume;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.core.testfixture.TestGroup;
import org.springframework.lang.Nullable;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AspectJ auto-proxying. Includes mixing with Spring AOP Advisors
 * to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public class AspectJAutoProxyCreatorTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);


	@Test
	public void testAspectsAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(68);
		MethodInvokingFactoryBean factoryBean = (MethodInvokingFactoryBean) bf.getBean("&factoryBean");
		assertThat(AopUtils.isAopProxy(factoryBean.getTargetObject())).isTrue();
		assertThat(((ITestBean) factoryBean.getTargetObject()).getAge()).isEqualTo(68);
	}

	@Test
	public void testMultipleAspectsWithParameterApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		tb.setAge(10);
		assertThat(tb.getAge()).isEqualTo(20);
	}

	@Test
	public void testAspectsAreAppliedInDefinedOrder() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithOrdering.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(71);
	}

	@Test
	public void testAspectsAndAdvisorAreApplied() {
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");

		ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian");
		doTestAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
	}

	@Test
	@EnabledForTestGroups(TestGroup.PERFORMANCE)
	public void testAspectsAndAdvisorAppliedToPrototypeIsFastEnough() {
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
	@EnabledForTestGroups(TestGroup.PERFORMANCE)
	public void testAspectsAndAdvisorNotAppliedToPrototypeIsFastEnough() {
		Assume.notLogging(factoryLog);

		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");

		StopWatch sw = new StopWatch();
		sw.start("Prototype Creation");
		for (int i = 0; i < 100000; i++) {
			INestedTestBean shouldNotBeWeaved = (INestedTestBean) ac.getBean("i21");
			if (i < 10) {
				assertThat(AopUtils.isAopProxy(shouldNotBeWeaved)).isFalse();
			}
		}
		sw.stop();

		// What's a reasonable expectation for _any_ server or developer machine load?
		// 3 seconds?
		assertStopWatchTimeLimit(sw, 6000);
	}

	@Test
	@EnabledForTestGroups(TestGroup.PERFORMANCE)
	public void testAspectsAndAdvisorNotAppliedToManySingletonsIsFastEnough() {
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
				.addPropertyValue(new PropertyValue("age", 34));
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
		assertThat(mrv.getMultiple()).isEqualTo(3);

		tba.count = 0;
		mrv.invocations = 0;

		assertThat(AopUtils.isAopProxy(shouldBeWeaved)).as("Autoproxying must apply from @AspectJ aspect").isTrue();
		assertThat(shouldBeWeaved.getName()).isEqualTo("Adrian");
		assertThat(mrv.invocations).isEqualTo(0);
		assertThat(shouldBeWeaved.getAge()).isEqualTo((34 * mrv.getMultiple()));
		assertThat(tba.count).as("Spring advisor must be invoked").isEqualTo(2);
		assertThat(mrv.invocations).as("Must be able to hold state in aspect").isEqualTo(1);
	}

	@Test
	public void testPerThisAspect() {
		ClassPathXmlApplicationContext bf = newContext("perthis.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian1)).isTrue();

		assertThat(adrian1.getAge()).isEqualTo(0);
		assertThat(adrian1.getAge()).isEqualTo(1);

		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertThat(adrian2).isNotSameAs(adrian1);
		assertThat(AopUtils.isAopProxy(adrian1)).isTrue();
		assertThat(adrian2.getAge()).isEqualTo(0);
		assertThat(adrian2.getAge()).isEqualTo(1);
		assertThat(adrian2.getAge()).isEqualTo(2);
		assertThat(adrian2.getAge()).isEqualTo(3);
		assertThat(adrian1.getAge()).isEqualTo(2);
	}

	@Test
	public void testPerTargetAspect() throws SecurityException, NoSuchMethodException {
		ClassPathXmlApplicationContext bf = newContext("pertarget.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian1)).isTrue();

		// Does not trigger advice or count
		int explicitlySetAge = 25;
		adrian1.setAge(explicitlySetAge);

		assertThat(adrian1.getAge()).as("Setter does not initiate advice").isEqualTo(explicitlySetAge);
		// Fire aspect

		AspectMetadata am = new AspectMetadata(PerTargetAspect.class, "someBean");
		assertThat(am.getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();

		adrian1.getSpouse();

		assertThat(adrian1.getAge()).as("Advice has now been instantiated").isEqualTo(0);
		adrian1.setAge(11);
		assertThat(adrian1.getAge()).as("Any int setter increments").isEqualTo(2);
		adrian1.setName("Adrian");
		//assertEquals("Any other setter does not increment", 2, adrian1.getAge());

		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertThat(adrian2).isNotSameAs(adrian1);
		assertThat(AopUtils.isAopProxy(adrian1)).isTrue();
		assertThat(adrian2.getAge()).isEqualTo(34);
		adrian2.getSpouse();
		assertThat(adrian2.getAge()).as("Aspect now fired").isEqualTo(0);
		assertThat(adrian2.getAge()).isEqualTo(1);
		assertThat(adrian2.getAge()).isEqualTo(2);
		assertThat(adrian1.getAge()).isEqualTo(3);
	}

	@Test
	public void testTwoAdviceAspect() {
		ClassPathXmlApplicationContext bf = newContext("twoAdviceAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testAgeAspect(adrian1, 0, 2);
	}

	@Test
	public void testTwoAdviceAspectSingleton() {
		ClassPathXmlApplicationContext bf = newContext("twoAdviceAspectSingleton.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testAgeAspect(adrian1, 0, 1);
		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertThat(adrian2).isNotSameAs(adrian1);
		testAgeAspect(adrian2, 2, 1);
	}

	@Test
	public void testTwoAdviceAspectPrototype() {
		ClassPathXmlApplicationContext bf = newContext("twoAdviceAspectPrototype.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testAgeAspect(adrian1, 0, 1);
		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertThat(adrian2).isNotSameAs(adrian1);
		testAgeAspect(adrian2, 0, 1);
	}

	private void testAgeAspect(ITestBean adrian, int start, int increment) {
		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		adrian.setName("");
		assertThat(adrian.age()).isEqualTo(start);
		int newAge = 32;
		adrian.setAge(newAge);
		assertThat(adrian.age()).isEqualTo((start + increment));
		adrian.setAge(0);
		assertThat(adrian.age()).isEqualTo((start + increment * 2));
	}

	@Test
	public void testAdviceUsingJoinPoint() {
		ClassPathXmlApplicationContext bf = newContext("usesJoinPointAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		adrian1.getAge();
		AdviceUsingThisJoinPoint aspectInstance = (AdviceUsingThisJoinPoint) bf.getBean("aspect");
		//(AdviceUsingThisJoinPoint) Aspects.aspectOf(AdviceUsingThisJoinPoint.class);
		//assertEquals("method-execution(int TestBean.getAge())",aspectInstance.getLastMethodEntered());
		assertThat(aspectInstance.getLastMethodEntered().indexOf("TestBean.getAge())") != 0).isTrue();
	}

	@Test
	public void testIncludeMechanism() {
		ClassPathXmlApplicationContext bf = newContext("usesInclude.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		assertThat(adrian.getAge()).isEqualTo(68);
	}

	@Test
	public void testForceProxyTargetClass() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithCGLIB.xml");

		ProxyConfig pc = (ProxyConfig) bf.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertThat(pc.isProxyTargetClass()).as("should be proxying classes").isTrue();
		assertThat(pc.isExposeProxy()).as("should expose proxy").isTrue();
	}

	@Test
	public void testWithAbstractFactoryBeanAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithAbstractBean.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		assertThat(adrian.getAge()).isEqualTo(68);
	}

	@Test
	public void testRetryAspect() {
		ClassPathXmlApplicationContext bf = newContext("retryAspect.xml");

		UnreliableBean bean = (UnreliableBean) bf.getBean("unreliableBean");
		RetryAspect aspect = (RetryAspect) bf.getBean("retryAspect");
		int attempts = bean.unreliable();
		assertThat(attempts).isEqualTo(2);
		assertThat(aspect.getBeginCalls()).isEqualTo(2);
		assertThat(aspect.getRollbackCalls()).isEqualTo(1);
		assertThat(aspect.getCommitCalls()).isEqualTo(1);
	}

	@Test
	public void testWithBeanNameAutoProxyCreator() {
		ClassPathXmlApplicationContext bf = newContext("withBeanNameAutoProxyCreator.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(68);
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
		return String.format("%s-%s", getClass().getSimpleName(), fileSuffix);
	}

	private void assertStopWatchTimeLimit(final StopWatch sw, final long maxTimeMillis) {
		long totalTimeMillis = sw.getTotalTimeMillis();
		assertThat(totalTimeMillis < maxTimeMillis).as("'" + sw.getLastTaskName() + "' took too long: expected less than<" + maxTimeMillis +
				"> ms, actual<" + totalTimeMillis + "> ms.").isTrue();
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
	public Object getObject() {
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

@Retention(RetentionPolicy.RUNTIME)
@interface Marker {
}

@Aspect
class MultiplyReturnValueForMarker {

	private int multiple = 2;

	public int invocations;

	public void setMultiple(int multiple) {
		this.multiple = multiple;
	}

	public int getMultiple() {
		return this.multiple;
	}

	@Around("@annotation(org.springframework.aop.aspectj.autoproxy.Marker)")
	public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
		++this.invocations;
		int result = (Integer) pjp.proceed();
		return result * this.multiple;
	}
}

interface IMarkerTestBean extends ITestBean {

	@Marker
	@Override
	int getAge();
}

class MarkerTestBean extends TestBean implements IMarkerTestBean {

	@Marker
	@Override
	public int getAge() {
		return super.getAge();
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
				}
				catch (RetryableException re) {
					this.rollbackCalls++;
					throw re;
				}
			}
			catch (RetryableException re) {
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
			public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
				++count;
			}
		});
	}

	@Override
	public boolean matches(Method method, @Nullable Class<?> targetClass) {
		return ITestBean.class.isAssignableFrom(targetClass);
	}

}
