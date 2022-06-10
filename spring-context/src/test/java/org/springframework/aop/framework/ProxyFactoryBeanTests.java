/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.mixin.Lockable;
import test.mixin.LockedException;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.aop.testfixture.advice.MyThrowsHandler;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.interceptor.TimestampIntroductionInterceptor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SideEffectBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.testfixture.beans.TestApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.TimeStamped;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13.03.2003
 */
public class ProxyFactoryBeanTests {

	private static final Class<?> CLASS = ProxyFactoryBeanTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String CONTEXT = CLASSNAME + "-context.xml";
	private static final String SERIALIZATION_CONTEXT = CLASSNAME + "-serialization.xml";
	private static final String AUTOWIRING_CONTEXT = CLASSNAME + "-autowiring.xml";
	private static final String DBL_TARGETSOURCE_CONTEXT = CLASSNAME + "-double-targetsource.xml";
	private static final String NOTLAST_TARGETSOURCE_CONTEXT = CLASSNAME + "-notlast-targetsource.xml";
	private static final String TARGETSOURCE_CONTEXT = CLASSNAME + "-targetsource.xml";
	private static final String INVALID_CONTEXT = CLASSNAME + "-invalid.xml";
	private static final String FROZEN_CONTEXT = CLASSNAME + "-frozen.xml";
	private static final String PROTOTYPE_CONTEXT = CLASSNAME + "-prototype.xml";
	private static final String THROWS_ADVICE_CONTEXT = CLASSNAME + "-throws-advice.xml";
	private static final String INNER_BEAN_TARGET_CONTEXT = CLASSNAME + "-inner-bean-target.xml";

	private BeanFactory factory;


	@BeforeEach
	public void setUp() throws Exception {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		parent.registerBeanDefinition("target2", new RootBeanDefinition(TestApplicationListener.class));
		this.factory = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader((BeanDefinitionRegistry) this.factory).loadBeanDefinitions(
				new ClassPathResource(CONTEXT, getClass()));
	}


	@Test
	public void testIsDynamicProxyWhenInterfaceSpecified() {
		ITestBean test1 = (ITestBean) factory.getBean("test1");
		assertThat(Proxy.isProxyClass(test1.getClass())).as("test1 is a dynamic proxy").isTrue();
	}

	@Test
	public void testIsDynamicProxyWhenInterfaceSpecifiedForPrototype() {
		ITestBean test1 = (ITestBean) factory.getBean("test2");
		assertThat(Proxy.isProxyClass(test1.getClass())).as("test2 is a dynamic proxy").isTrue();
	}

	@Test
	public void testIsDynamicProxyWhenAutodetectingInterfaces() {
		ITestBean test1 = (ITestBean) factory.getBean("test3");
		assertThat(Proxy.isProxyClass(test1.getClass())).as("test3 is a dynamic proxy").isTrue();
	}

	@Test
	public void testIsDynamicProxyWhenAutodetectingInterfacesForPrototype() {
		ITestBean test1 = (ITestBean) factory.getBean("test4");
		assertThat(Proxy.isProxyClass(test1.getClass())).as("test4 is a dynamic proxy").isTrue();
	}

	/**
	 * Test that it's forbidden to specify TargetSource in both
	 * interceptor chain and targetSource property.
	 */
	@Test
	public void testDoubleTargetSourcesAreRejected() {
		testDoubleTargetSourceIsRejected("doubleTarget");
		// Now with conversion from arbitrary bean to a TargetSource
		testDoubleTargetSourceIsRejected("arbitraryTarget");
	}

	private void testDoubleTargetSourceIsRejected(String name) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(DBL_TARGETSOURCE_CONTEXT, CLASS));
		assertThatExceptionOfType(BeanCreationException.class).as("Should not allow TargetSource to be specified in interceptorNames as well as targetSource property")
			.isThrownBy(() -> bf.getBean(name))
			.havingCause()
			.isInstanceOf(AopConfigException.class)
			.withMessageContaining("TargetSource");
	}

	@Test
	public void testTargetSourceNotAtEndOfInterceptorNamesIsRejected() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(NOTLAST_TARGETSOURCE_CONTEXT, CLASS));
		assertThatExceptionOfType(BeanCreationException.class).as("TargetSource or non-advised object must be last in interceptorNames")
			.isThrownBy(() -> bf.getBean("targetSourceNotLast"))
			.havingCause()
			.isInstanceOf(AopConfigException.class)
			.withMessageContaining("interceptorNames");
	}

	@Test
	public void testGetObjectTypeWithDirectTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));

		// We have a counting before advice here
		CountingBeforeAdvice cba = (CountingBeforeAdvice) bf.getBean("countingBeforeAdvice");
		assertThat(cba.getCalls()).isEqualTo(0);

		ITestBean tb = (ITestBean) bf.getBean("directTarget");
		assertThat(tb.getName().equals("Adam")).isTrue();
		assertThat(cba.getCalls()).isEqualTo(1);

		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&directTarget");
		assertThat(TestBean.class.isAssignableFrom(pfb.getObjectType())).as("Has correct object type").isTrue();
	}

	@Test
	public void testGetObjectTypeWithTargetViaTargetSource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));
		ITestBean tb = (ITestBean) bf.getBean("viaTargetSource");
		assertThat(tb.getName().equals("Adam")).isTrue();
		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&viaTargetSource");
		assertThat(TestBean.class.isAssignableFrom(pfb.getObjectType())).as("Has correct object type").isTrue();
	}

	@Test
	public void testGetObjectTypeWithNoTargetOrTargetSource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));

		ITestBean tb = (ITestBean) bf.getBean("noTarget");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				tb.getName())
			.withMessage("getName");
		FactoryBean<?> pfb = (ProxyFactoryBean) bf.getBean("&noTarget");
		assertThat(ITestBean.class.isAssignableFrom(pfb.getObjectType())).as("Has correct object type").isTrue();
	}

	/**
	 * The instances are equal, but do not have object identity.
	 * Interceptors and interfaces and the target are the same.
	 */
	@Test
	public void testSingletonInstancesAreEqual() {
		ITestBean test1 = (ITestBean) factory.getBean("test1");
		ITestBean test1_1 = (ITestBean) factory.getBean("test1");
		//assertTrue("Singleton instances ==", test1 == test1_1);
		assertThat(test1_1).as("Singleton instances ==").isEqualTo(test1);
		test1.setAge(25);
		assertThat(test1_1.getAge()).isEqualTo(test1.getAge());
		test1.setAge(250);
		assertThat(test1_1.getAge()).isEqualTo(test1.getAge());
		Advised pc1 = (Advised) test1;
		Advised pc2 = (Advised) test1_1;
		assertThat(pc2.getAdvisors()).isEqualTo(pc1.getAdvisors());
		int oldLength = pc1.getAdvisors().length;
		NopInterceptor di = new NopInterceptor();
		pc1.addAdvice(1, di);
		assertThat(pc2.getAdvisors()).isEqualTo(pc1.getAdvisors());
		assertThat(pc2.getAdvisors().length).as("Now have one more advisor").isEqualTo((oldLength + 1));
		assertThat(0).isEqualTo(di.getCount());
		test1.setAge(5);
		assertThat(test1.getAge()).isEqualTo(test1_1.getAge());
		assertThat(3).isEqualTo(di.getCount());
	}

	@Test
	public void testPrototypeInstancesAreNotEqual() {
		assertThat(ITestBean.class.isAssignableFrom(factory.getType("prototype"))).as("Has correct object type").isTrue();
		ITestBean test2 = (ITestBean) factory.getBean("prototype");
		ITestBean test2_1 = (ITestBean) factory.getBean("prototype");
		assertThat(test2 != test2_1).as("Prototype instances !=").isTrue();
		assertThat(test2.equals(test2_1)).as("Prototype instances equal").isTrue();
		assertThat(ITestBean.class.isAssignableFrom(factory.getType("prototype"))).as("Has correct object type").isTrue();
	}

	/**
	 * Uses its own bean factory XML for clarity
	 * @param beanName name of the ProxyFactoryBean definition that should
	 * be a prototype
	 */
	private Object testPrototypeInstancesAreIndependent(String beanName) {
		// Initial count value set in bean factory XML
		int INITIAL_COUNT = 10;

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(PROTOTYPE_CONTEXT, CLASS));

		// Check it works without AOP
		SideEffectBean raw = (SideEffectBean) bf.getBean("prototypeTarget");
		assertThat(raw.getCount()).isEqualTo(INITIAL_COUNT);
		raw.doWork();
		assertThat(raw.getCount()).isEqualTo(INITIAL_COUNT+1);
		raw = (SideEffectBean) bf.getBean("prototypeTarget");
		assertThat(raw.getCount()).isEqualTo(INITIAL_COUNT);

		// Now try with advised instances
		SideEffectBean prototype2FirstInstance = (SideEffectBean) bf.getBean(beanName);
		assertThat(prototype2FirstInstance.getCount()).isEqualTo(INITIAL_COUNT);
		prototype2FirstInstance.doWork();
		assertThat(prototype2FirstInstance.getCount()).isEqualTo(INITIAL_COUNT + 1);

		SideEffectBean prototype2SecondInstance = (SideEffectBean) bf.getBean(beanName);
		assertThat(prototype2FirstInstance == prototype2SecondInstance).as("Prototypes are not ==").isFalse();
		assertThat(prototype2SecondInstance.getCount()).isEqualTo(INITIAL_COUNT);
		assertThat(prototype2FirstInstance.getCount()).isEqualTo(INITIAL_COUNT + 1);

		return prototype2FirstInstance;
	}

	@Test
	public void testCglibPrototypeInstance() {
		Object prototype = testPrototypeInstancesAreIndependent("cglibPrototype");
		assertThat(AopUtils.isCglibProxy(prototype)).as("It's a cglib proxy").isTrue();
		assertThat(AopUtils.isJdkDynamicProxy(prototype)).as("It's not a dynamic proxy").isFalse();
	}

	/**
	 * Test invoker is automatically added to manipulate target.
	 */
	@Test
	public void testAutoInvoker() {
		String name = "Hieronymous";
		TestBean target = (TestBean) factory.getBean("test");
		target.setName(name);
		ITestBean autoInvoker = (ITestBean) factory.getBean("autoInvoker");
		assertThat(autoInvoker.getName().equals(name)).isTrue();
	}

	@Test
	public void testCanGetFactoryReferenceAndManipulate() {
		ProxyFactoryBean config = (ProxyFactoryBean) factory.getBean("&test1");
		assertThat(ITestBean.class.isAssignableFrom(config.getObjectType())).as("Has correct object type").isTrue();
		assertThat(ITestBean.class.isAssignableFrom(factory.getType("test1"))).as("Has correct object type").isTrue();
		// Trigger lazy initialization.
		config.getObject();
		assertThat(config.getAdvisors().length).as("Have one advisors").isEqualTo(1);
		assertThat(ITestBean.class.isAssignableFrom(config.getObjectType())).as("Has correct object type").isTrue();
		assertThat(ITestBean.class.isAssignableFrom(factory.getType("test1"))).as("Has correct object type").isTrue();

		ITestBean tb = (ITestBean) factory.getBean("test1");
		// no exception
		tb.hashCode();

		final Exception ex = new UnsupportedOperationException("invoke");
		// Add evil interceptor to head of list
		config.addAdvice(0, (MethodInterceptor) invocation -> {
			throw ex;
		});
		assertThat(config.getAdvisors().length).as("Have correct advisor count").isEqualTo(2);

		ITestBean tb1 = (ITestBean) factory.getBean("test1");
		assertThatException()
			.isThrownBy(tb1::toString)
			.isSameAs(ex);
	}

	/**
	 * Test that inner bean for target means that we can use
	 * autowire without ambiguity from target and proxy
	 */
	@Test
	public void testTargetAsInnerBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(INNER_BEAN_TARGET_CONTEXT, CLASS));
		ITestBean itb = (ITestBean) bf.getBean("testBean");
		assertThat(itb.getName()).isEqualTo("innerBeanTarget");
		assertThat(bf.getBeanDefinitionCount()).as("Only have proxy and interceptor: no target").isEqualTo(3);
		DependsOnITestBean doit = (DependsOnITestBean) bf.getBean("autowireCheck");
		assertThat(doit.tb).isSameAs(itb);
	}

	/**
	 * Try adding and removing interfaces and interceptors on prototype.
	 * Changes will only affect future references obtained from the factory.
	 * Each instance will be independent.
	 */
	@Test
	public void testCanAddAndRemoveAspectInterfacesOnPrototype() {
		assertThat(factory.getBean("test2")).as("Shouldn't implement TimeStamped before manipulation")
				.isNotInstanceOf(TimeStamped.class);

		ProxyFactoryBean config = (ProxyFactoryBean) factory.getBean("&test2");
		long time = 666L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor();
		ti.setTime(time);
		// Add to head of interceptor chain
		int oldCount = config.getAdvisors().length;
		config.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));
		assertThat(config.getAdvisors().length == oldCount + 1).isTrue();

		TimeStamped ts = (TimeStamped) factory.getBean("test2");
		assertThat(ts.getTimeStamp()).isEqualTo(time);

		// Can remove
		config.removeAdvice(ti);
		assertThat(config.getAdvisors().length == oldCount).isTrue();

		// Check no change on existing object reference
		assertThat(ts.getTimeStamp() == time).isTrue();

		assertThat(factory.getBean("test2")).as("Should no longer implement TimeStamped")
				.isNotInstanceOf(TimeStamped.class);

		// Now check non-effect of removing interceptor that isn't there
		config.removeAdvice(new DebugInterceptor());
		assertThat(config.getAdvisors().length == oldCount).isTrue();

		ITestBean it = (ITestBean) ts;
		DebugInterceptor debugInterceptor = new DebugInterceptor();
		config.addAdvice(0, debugInterceptor);
		it.getSpouse();
		// Won't affect existing reference
		assertThat(debugInterceptor.getCount() == 0).isTrue();
		it = (ITestBean) factory.getBean("test2");
		it.getSpouse();
		assertThat(debugInterceptor.getCount()).isEqualTo(1);
		config.removeAdvice(debugInterceptor);
		it.getSpouse();

		// Still invoked with old reference
		assertThat(debugInterceptor.getCount()).isEqualTo(2);

		// not invoked with new object
		it = (ITestBean) factory.getBean("test2");
		it.getSpouse();
		assertThat(debugInterceptor.getCount()).isEqualTo(2);

		// Our own timestamped reference should still work
		assertThat(ts.getTimeStamp()).isEqualTo(time);
	}

	/**
	 * Note that we can't add or remove interfaces without reconfiguring the
	 * singleton.
	 */
	@Test
	public void testCanAddAndRemoveAdvicesOnSingleton() {
		ITestBean it = (ITestBean) factory.getBean("test1");
		Advised pc = (Advised) it;
		it.getAge();
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(0, di);
		assertThat(di.getCount()).isEqualTo(0);
		it.setAge(25);
		assertThat(it.getAge()).isEqualTo(25);
		assertThat(di.getCount()).isEqualTo(2);
	}

	@Test
	public void testMethodPointcuts() {
		ITestBean tb = (ITestBean) factory.getBean("pointcuts");
		PointcutForVoid.reset();
		assertThat(PointcutForVoid.methodNames.isEmpty()).as("No methods intercepted").isTrue();
		tb.getAge();
		assertThat(PointcutForVoid.methodNames.isEmpty()).as("Not void: shouldn't have intercepted").isTrue();
		tb.setAge(1);
		tb.getAge();
		tb.setName("Tristan");
		tb.toString();
		assertThat(PointcutForVoid.methodNames.size()).as("Recorded wrong number of invocations").isEqualTo(2);
		assertThat(PointcutForVoid.methodNames.get(0).equals("setAge")).isTrue();
		assertThat(PointcutForVoid.methodNames.get(1).equals("setName")).isTrue();
	}

	@Test
	public void testCanAddThrowsAdviceWithoutAdvisor() throws Throwable {
		DefaultListableBeanFactory f = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(f).loadBeanDefinitions(new ClassPathResource(THROWS_ADVICE_CONTEXT, CLASS));
		MyThrowsHandler th = (MyThrowsHandler) f.getBean("throwsAdvice");
		CountingBeforeAdvice cba = (CountingBeforeAdvice) f.getBean("countingBeforeAdvice");
		assertThat(cba.getCalls()).isEqualTo(0);
		assertThat(th.getCalls()).isEqualTo(0);
		IEcho echo = (IEcho) f.getBean("throwsAdvised");
		int i = 12;
		echo.setA(i);
		assertThat(echo.getA()).isEqualTo(i);
		assertThat(cba.getCalls()).isEqualTo(2);
		assertThat(th.getCalls()).isEqualTo(0);
		Exception expected = new Exception();
		assertThatException().isThrownBy(() -> echo.echoException(1, expected))
			.matches(expected::equals);
		// No throws handler method: count should still be 0
		assertThat(th.getCalls()).isEqualTo(0);

		// Handler knows how to handle this exception
		FileNotFoundException expectedFileNotFound = new FileNotFoundException();
		assertThatIOException().isThrownBy(() ->
				echo.echoException(1, expectedFileNotFound))
			.matches(expectedFileNotFound::equals);

		// One match
		assertThat(th.getCalls("ioException")).isEqualTo(1);
	}

	// These two fail the whole bean factory
	// TODO put in sep file to check quality of error message
	/*
	@Test
	public void testNoInterceptorNamesWithoutTarget() {
		assertThatExceptionOfType(AopConfigurationException.class).as("Should require interceptor names").isThrownBy(() ->
				ITestBean tb = (ITestBean) factory.getBean("noInterceptorNamesWithoutTarget"));
	}

	@Test
	public void testNoInterceptorNamesWithTarget() {
		ITestBean tb = (ITestBean) factory.getBean("noInterceptorNamesWithoutTarget");
	}
	*/

	@Test
	public void testEmptyInterceptorNames() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(INVALID_CONTEXT, CLASS));
		assertThatExceptionOfType(BeanCreationException.class).as("Interceptor names cannot be empty").isThrownBy(() ->
				bf.getBean("emptyInterceptorNames"));
	}

	/**
	 * Globals must be followed by a target.
	 */
	@Test
	public void testGlobalsWithoutTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(INVALID_CONTEXT, CLASS));
		assertThatExceptionOfType(BeanCreationException.class).as("Should require target name").isThrownBy(() ->
				bf.getBean("globalsWithoutTarget"))
			.withCauseInstanceOf(AopConfigException.class);
	}

	/**
	 * Checks that globals get invoked,
	 * and that they can add aspect interfaces unavailable
	 * to other beans. These interfaces don't need
	 * to be included in proxiedInterface [].
	 */
	@Test
	public void testGlobalsCanAddAspectInterfaces() {
		AddedGlobalInterface agi = (AddedGlobalInterface) factory.getBean("autoInvoker");
		assertThat(agi.globalsAdded() == -1).isTrue();

		ProxyFactoryBean pfb = (ProxyFactoryBean) factory.getBean("&validGlobals");
		// Trigger lazy initialization.
		pfb.getObject();
		// 2 globals + 2 explicit
		assertThat(pfb.getAdvisors().length).as("Have 2 globals and 2 explicit advisors").isEqualTo(3);

		ApplicationListener<?> l = (ApplicationListener<?>) factory.getBean("validGlobals");
		agi = (AddedGlobalInterface) l;
		assertThat(agi.globalsAdded() == -1).isTrue();

		assertThat(factory.getBean("test1")).as("Aspect interface should't be implemeneted without globals")
				.isNotInstanceOf(AddedGlobalInterface.class);
	}

	@Test
	public void testSerializableSingletonProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializableSingleton");
		assertThat(bf.getBean("serializableSingleton")).as("Should be a Singleton").isSameAs(p);
		Person p2 = SerializationTestUtils.serializeAndDeserialize(p);
		assertThat(p2).isEqualTo(p);
		assertThat(p2).isNotSameAs(p);
		assertThat(p2.getName()).isEqualTo("serializableSingleton");

		// Add unserializable advice
		Advice nop = new NopInterceptor();
		((Advised) p).addAdvice(nop);
		// Check it still works
		assertThat(p2.getName()).isEqualTo(p2.getName());
		assertThat(SerializationTestUtils.isSerializable(p)).as("Not serializable because an interceptor isn't serializable").isFalse();

		// Remove offending interceptor...
		assertThat(((Advised) p).removeAdvice(nop)).isTrue();
		assertThat(SerializationTestUtils.isSerializable(p)).as("Serializable again because offending interceptor was removed").isTrue();
	}

	@Test
	public void testSerializablePrototypeProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializablePrototype");
		assertThat(bf.getBean("serializablePrototype")).as("Should not be a Singleton").isNotSameAs(p);
		Person p2 = SerializationTestUtils.serializeAndDeserialize(p);
		assertThat(p2).isEqualTo(p);
		assertThat(p2).isNotSameAs(p);
		assertThat(p2.getName()).isEqualTo("serializablePrototype");
	}

	@Test
	public void testSerializableSingletonProxyFactoryBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializableSingleton");
		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&serializableSingleton");
		ProxyFactoryBean pfb2 = SerializationTestUtils.serializeAndDeserialize(pfb);
		Person p2 = (Person) pfb2.getObject();
		assertThat(p2).isEqualTo(p);
		assertThat(p2).isNotSameAs(p);
		assertThat(p2.getName()).isEqualTo("serializableSingleton");
	}

	@Test
	public void testProxyNotSerializableBecauseOfAdvice() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("interceptorNotSerializableSingleton");
		assertThat(SerializationTestUtils.isSerializable(p)).as("Not serializable because an interceptor isn't serializable").isFalse();
	}

	@Test
	public void testPrototypeAdvisor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(CONTEXT, CLASS));

		ITestBean bean1 = (ITestBean) bf.getBean("prototypeTestBeanProxy");
		ITestBean bean2 = (ITestBean) bf.getBean("prototypeTestBeanProxy");

		bean1.setAge(3);
		bean2.setAge(4);

		assertThat(bean1.getAge()).isEqualTo(3);
		assertThat(bean2.getAge()).isEqualTo(4);

		((Lockable) bean1).lock();

		assertThatExceptionOfType(LockedException.class).isThrownBy(() -> bean1.setAge(5));

		bean2.setAge(6); //do not expect LockedException"
	}

	@Test
	public void testPrototypeInterceptorSingletonTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(CONTEXT, CLASS));

		ITestBean bean1 = (ITestBean) bf.getBean("prototypeTestBeanProxySingletonTarget");
		ITestBean bean2 = (ITestBean) bf.getBean("prototypeTestBeanProxySingletonTarget");

		bean1.setAge(1);
		bean2.setAge(2);

		assertThat(bean1.getAge()).isEqualTo(2);

		((Lockable) bean1).lock();

		assertThatExceptionOfType(LockedException.class).isThrownBy(() -> bean1.setAge(5));

		// do not expect LockedException
		bean2.setAge(6);
	}

	/**
	 * Simple test of a ProxyFactoryBean that has an inner bean as target that specifies autowiring.
	 * Checks for correct use of getType() by bean factory.
	 */
	@Test
	public void testInnerBeanTargetUsingAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(AUTOWIRING_CONTEXT, CLASS));
		bf.getBean("testBean");
	}

	@Test
	public void testFrozenFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(FROZEN_CONTEXT, CLASS));

		Advised advised = (Advised) bf.getBean("frozen");
		assertThat(advised.isFrozen()).as("The proxy should be frozen").isTrue();
	}

	@Test
	public void testDetectsInterfaces() {
		ProxyFactoryBean fb = new ProxyFactoryBean();
		fb.setTarget(new TestBean());
		fb.addAdvice(new DebugInterceptor());
		fb.setBeanFactory(new DefaultListableBeanFactory());

		ITestBean proxy = (ITestBean) fb.getObject();
		assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
	}

	@Test
	public void testWithInterceptorNames() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("debug", new DebugInterceptor());

		ProxyFactoryBean fb = new ProxyFactoryBean();
		fb.setTarget(new TestBean());
		fb.setInterceptorNames("debug");
		fb.setBeanFactory(bf);

		Advised proxy = (Advised) fb.getObject();
		assertThat(proxy.getAdvisorCount()).isEqualTo(1);
	}

	@Test
	public void testWithLateInterceptorNames() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("debug", new DebugInterceptor());

		ProxyFactoryBean fb = new ProxyFactoryBean();
		fb.setTarget(new TestBean());
		fb.setBeanFactory(bf);
		fb.getObject();

		fb.setInterceptorNames("debug");
		Advised proxy = (Advised) fb.getObject();
		assertThat(proxy.getAdvisorCount()).isEqualTo(1);
	}


	/**
	 * Fires only on void methods. Saves list of methods intercepted.
	 */
	@SuppressWarnings("serial")
	public static class PointcutForVoid extends DefaultPointcutAdvisor {

		public static List<String> methodNames = new ArrayList<>();

		public static void reset() {
			methodNames.clear();
		}

		public PointcutForVoid() {
			setAdvice((MethodInterceptor) invocation -> {
				methodNames.add(invocation.getMethod().getName());
				return invocation.proceed();
			});
			setPointcut(new DynamicMethodMatcherPointcut() {
				@Override
				public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
					return m.getReturnType() == Void.TYPE;
				}
			});
		}
	}


	public static class DependsOnITestBean {

		public final ITestBean tb;

		public DependsOnITestBean(ITestBean tb) {
			this.tb = tb;
		}
	}

	/**
	 * Aspect interface
	 */
	public interface AddedGlobalInterface {

		int globalsAdded();
	}


	/**
	 * Use as a global interceptor. Checks that
	 * global interceptors can add aspect interfaces.
	 * NB: Add only via global interceptors in XML file.
	 */
	public static class GlobalAspectInterfaceInterceptor implements IntroductionInterceptor {

		@Override
		public boolean implementsInterface(Class<?> intf) {
			return intf.equals(AddedGlobalInterface.class);
		}

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			if (mi.getMethod().getDeclaringClass().equals(AddedGlobalInterface.class)) {
				return -1;
			}
			return mi.proceed();
		}
	}


	public static class GlobalIntroductionAdvice implements IntroductionAdvisor {

		private IntroductionInterceptor gi = new GlobalAspectInterfaceInterceptor();

		@Override
		public ClassFilter getClassFilter() {
			return ClassFilter.TRUE;
		}

		@Override
		public Advice getAdvice() {
			return this.gi;
		}

		@Override
		public Class<?>[] getInterfaces() {
			return new Class<?>[] { AddedGlobalInterface.class };
		}

		@Override
		public boolean isPerInstance() {
			return false;
		}

		@Override
		public void validateInterfaces() {
		}
	}

}
