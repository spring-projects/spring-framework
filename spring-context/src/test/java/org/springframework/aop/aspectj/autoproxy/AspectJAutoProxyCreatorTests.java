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

package org.springframework.aop.aspectj.autoproxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.annotation.AspectMetadata;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.aop.testfixture.aspectj.PerTargetAspect;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.cglib.proxy.Factory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;

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
class AspectJAutoProxyCreatorTests {

	@Test
	void aspectsAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(68);
		MethodInvokingFactoryBean factoryBean = (MethodInvokingFactoryBean) bf.getBean("&factoryBean");
		assertThat(AopUtils.isAopProxy(factoryBean.getTargetObject())).isTrue();
		assertThat(((ITestBean) factoryBean.getTargetObject()).getAge()).isEqualTo(68);
	}

	@Test
	void multipleAspectsWithParameterApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspects.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		tb.setAge(10);
		assertThat(tb.getAge()).isEqualTo(20);
	}

	@Test
	void aspectsAreAppliedInDefinedOrder() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithOrdering.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(71);
	}

	@Test
	void aspectsAndAdvisorAreApplied() {
		ClassPathXmlApplicationContext ac = newContext("aspectsPlusAdvisor.xml");

		ITestBean shouldBeWeaved = (ITestBean) ac.getBean("adrian");
		doTestAspectsAndAdvisorAreApplied(ac, shouldBeWeaved);
	}

	@Test
	void aspectsAndAdvisorAreAppliedEvenIfComingFromParentFactory() {
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
	void perThisAspect() {
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
	void perTargetAspect() throws SecurityException, NoSuchMethodException {
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

	@Test  // gh-31238
	void cglibProxyClassIsCachedAcrossApplicationContextsForPerTargetAspect() {
		Class<?> configClass = PerTargetProxyTargetClassTrueConfig.class;
		TestBean testBean1;
		TestBean testBean2;

		// Round #1
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
			testBean1 = context.getBean(TestBean.class);
			assertThat(AopUtils.isCglibProxy(testBean1)).as("CGLIB proxy").isTrue();
			assertThat(testBean1.getClass().getInterfaces()).containsExactlyInAnyOrder(
					Factory.class, SpringProxy.class, Advised.class);
		}

		// Round #2
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
			testBean2 = context.getBean(TestBean.class);
			assertThat(AopUtils.isCglibProxy(testBean2)).as("CGLIB proxy").isTrue();
			assertThat(testBean2.getClass().getInterfaces()).containsExactlyInAnyOrder(
					Factory.class, SpringProxy.class, Advised.class);
		}

		assertThat(testBean1.getClass()).isSameAs(testBean2.getClass());
	}

	@Test
	void twoAdviceAspect() {
		ClassPathXmlApplicationContext bf = newContext("twoAdviceAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testAgeAspect(adrian1, 0, 2);
	}

	@Test
	void twoAdviceAspectSingleton() {
		ClassPathXmlApplicationContext bf = newContext("twoAdviceAspectSingleton.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		testAgeAspect(adrian1, 0, 1);
		ITestBean adrian2 = (ITestBean) bf.getBean("adrian");
		assertThat(adrian2).isNotSameAs(adrian1);
		testAgeAspect(adrian2, 2, 1);
	}

	@Test
	void twoAdviceAspectPrototype() {
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
		assertThat(adrian.age()).isEqualTo(start + increment);
		adrian.setAge(0);
		assertThat(adrian.age()).isEqualTo(start + increment * 2);
	}

	@Test
	void adviceUsingJoinPoint() {
		ClassPathXmlApplicationContext bf = newContext("usesJoinPointAspect.xml");

		ITestBean adrian1 = (ITestBean) bf.getBean("adrian");
		adrian1.getAge();
		AdviceUsingThisJoinPoint aspectInstance = (AdviceUsingThisJoinPoint) bf.getBean("aspect");
		//(AdviceUsingThisJoinPoint) Aspects.aspectOf(AdviceUsingThisJoinPoint.class);
		//assertEquals("method-execution(int TestBean.getAge())",aspectInstance.getLastMethodEntered());
		assertThat(aspectInstance.getLastMethodEntered()).doesNotStartWith("TestBean.getAge())");
	}

	@Test
	void includeMechanism() {
		ClassPathXmlApplicationContext bf = newContext("usesInclude.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		assertThat(adrian.getAge()).isEqualTo(68);
	}

	@Test
	void forceProxyTargetClass() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithCGLIB.xml");

		ProxyConfig pc = (ProxyConfig) bf.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		assertThat(pc.isProxyTargetClass()).as("should be proxying classes").isTrue();
		assertThat(pc.isExposeProxy()).as("should expose proxy").isTrue();
	}

	@Test
	void withAbstractFactoryBeanAreApplied() {
		ClassPathXmlApplicationContext bf = newContext("aspectsWithAbstractBean.xml");

		ITestBean adrian = (ITestBean) bf.getBean("adrian");
		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		assertThat(adrian.getAge()).isEqualTo(68);
	}

	@Test
	void retryAspect() {
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
	void withBeanNameAutoProxyCreator() {
		ClassPathXmlApplicationContext bf = newContext("withBeanNameAutoProxyCreator.xml");

		ITestBean tb = (ITestBean) bf.getBean("adrian");
		assertThat(tb.getAge()).isEqualTo(68);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(classes = {ProxyTargetClassFalseConfig.class, ProxyTargetClassTrueConfig.class})
	void lambdaIsAlwaysProxiedWithJdkProxy(Class<?> configClass) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
			@SuppressWarnings("unchecked")
			Supplier<String> supplier = context.getBean(Supplier.class);
			assertThat(AopUtils.isAopProxy(supplier)).as("AOP proxy").isTrue();
			assertThat(AopUtils.isJdkDynamicProxy(supplier)).as("JDK Dynamic proxy").isTrue();
			assertThat(supplier.getClass().getInterfaces()).containsExactlyInAnyOrder(
					Supplier.class, SpringProxy.class, Advised.class, DecoratingProxy.class);
			assertThat(supplier.get()).isEqualTo("advised: lambda");
		}
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(classes = {MixinProxyTargetClassFalseConfig.class, MixinProxyTargetClassTrueConfig.class})
	void lambdaIsAlwaysProxiedWithJdkProxyWithIntroductions(Class<?> configClass) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configClass)) {
			MessageGenerator messageGenerator = context.getBean(MessageGenerator.class);
			assertThat(AopUtils.isAopProxy(messageGenerator)).as("AOP proxy").isTrue();
			assertThat(AopUtils.isJdkDynamicProxy(messageGenerator)).as("JDK Dynamic proxy").isTrue();
			assertThat(messageGenerator.getClass().getInterfaces()).containsExactlyInAnyOrder(
					MessageGenerator.class, Mixin.class, SpringProxy.class, Advised.class, DecoratingProxy.class);
			assertThat(messageGenerator.generateMessage()).isEqualTo("mixin: lambda");
		}
	}

	private ClassPathXmlApplicationContext newContext(String fileSuffix) {
		return new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-" + fileSuffix, getClass());
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

	RetryableException(String msg) {
		super(msg);
	}

	RetryableException(String msg, Throwable cause) {
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
		setAdvice((MethodBeforeAdvice) (method, args, target) -> ++count);
	}

	@Override
	public boolean matches(Method method, @Nullable Class<?> targetClass) {
		return ITestBean.class.isAssignableFrom(targetClass);
	}
}

abstract class AbstractProxyTargetClassConfig {

	@Bean
	Supplier<String> stringSupplier() {
		return () -> "lambda";
	}

	@Bean
	SupplierAdvice supplierAdvice() {
		return new SupplierAdvice();
	}

	@Aspect
	static class SupplierAdvice {

		@Around("execution(public * org.springframework.aop.aspectj.autoproxy..*.*(..))")
		Object aroundSupplier(ProceedingJoinPoint joinPoint) throws Throwable {
			return "advised: " + joinPoint.proceed();
		}
	}
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
class ProxyTargetClassFalseConfig extends AbstractProxyTargetClassConfig {
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ProxyTargetClassTrueConfig extends AbstractProxyTargetClassConfig {
}

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class PerTargetProxyTargetClassTrueConfig {

	@Bean
	@Scope("prototype")
	TestBean testBean() {
		return new TestBean("Jane", 34);
	}

	@Bean
	@Scope("prototype")
	PerTargetAspect perTargetAspect() {
		return new PerTargetAspect();
	}
}

@FunctionalInterface
interface MessageGenerator {

	String generateMessage();
}

interface Mixin {
}

class MixinIntroductionInterceptor implements IntroductionInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		return "mixin: " + invocation.proceed();
	}

	@Override
	public boolean implementsInterface(Class<?> intf) {
		return Mixin.class.isAssignableFrom(intf);
	}
}

@SuppressWarnings("serial")
class MixinAdvisor extends AbstractPointcutAdvisor implements IntroductionAdvisor {

	@Override
	public org.springframework.aop.Pointcut getPointcut() {
		return org.springframework.aop.Pointcut.TRUE;
	}

	@Override
	public Advice getAdvice() {
		return new MixinIntroductionInterceptor();
	}

	@Override
	public Class<?>[] getInterfaces() {
		return new Class[] { Mixin.class };
	}

	@Override
	public ClassFilter getClassFilter() {
		return MessageGenerator.class::isAssignableFrom;
	}

	@Override
	public void validateInterfaces() {
		/* no-op */
	}
}

abstract class AbstractMixinConfig {

	@Bean
	MessageGenerator messageGenerator() {
		return () -> "lambda";
	}

	@Bean
	MixinAdvisor mixinAdvisor() {
		return new MixinAdvisor();
	}
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
class MixinProxyTargetClassFalseConfig extends AbstractMixinConfig {
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class MixinProxyTargetClassTrueConfig extends AbstractMixinConfig {
}
