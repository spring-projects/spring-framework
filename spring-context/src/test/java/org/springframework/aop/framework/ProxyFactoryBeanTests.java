/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.TestListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.tests.TimeStamped;
import org.springframework.tests.aop.advice.CountingBeforeAdvice;
import org.springframework.tests.aop.advice.MyThrowsHandler;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.aop.interceptor.TimestampIntroductionInterceptor;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SideEffectBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @since 13.03.2003
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
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


	@Before
	public void setUp() throws Exception {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		parent.registerBeanDefinition("target2", new RootBeanDefinition(TestListener.class));
		this.factory = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader((BeanDefinitionRegistry) this.factory).loadBeanDefinitions(
				new ClassPathResource(CONTEXT, getClass()));
	}


	@Test
	public void testIsDynamicProxyWhenInterfaceSpecified() {
		ITestBean test1 = (ITestBean) factory.getBean("test1");
		assertTrue("test1 is a dynamic proxy", Proxy.isProxyClass(test1.getClass()));
	}

	@Test
	public void testIsDynamicProxyWhenInterfaceSpecifiedForPrototype() {
		ITestBean test1 = (ITestBean) factory.getBean("test2");
		assertTrue("test2 is a dynamic proxy", Proxy.isProxyClass(test1.getClass()));
	}

	@Test
	public void testIsDynamicProxyWhenAutodetectingInterfaces() {
		ITestBean test1 = (ITestBean) factory.getBean("test3");
		assertTrue("test3 is a dynamic proxy", Proxy.isProxyClass(test1.getClass()));
	}

	@Test
	public void testIsDynamicProxyWhenAutodetectingInterfacesForPrototype() {
		ITestBean test1 = (ITestBean) factory.getBean("test4");
		assertTrue("test4 is a dynamic proxy", Proxy.isProxyClass(test1.getClass()));
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
		try {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(DBL_TARGETSOURCE_CONTEXT, CLASS));
			bf.getBean(name);
			fail("Should not allow TargetSource to be specified in interceptorNames as well as targetSource property");
		}
		catch (BeanCreationException ex) {
			// Root cause of the problem must be an AOP exception
			AopConfigException aex = (AopConfigException) ex.getCause();
			assertTrue(aex.getMessage().indexOf("TargetSource") != -1);
		}
	}

	@Test
	public void testTargetSourceNotAtEndOfInterceptorNamesIsRejected() {
		try {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(NOTLAST_TARGETSOURCE_CONTEXT, CLASS));
			bf.getBean("targetSourceNotLast");
			fail("TargetSource or non-advised object must be last in interceptorNames");
		}
		catch (BeanCreationException ex) {
			// Root cause of the problem must be an AOP exception
			AopConfigException aex = (AopConfigException) ex.getCause();
			assertTrue(aex.getMessage().contains("interceptorNames"));
		}
	}

	@Test
	public void testGetObjectTypeWithDirectTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));

		// We have a counting before advice here
		CountingBeforeAdvice cba = (CountingBeforeAdvice) bf.getBean("countingBeforeAdvice");
		assertEquals(0, cba.getCalls());

		ITestBean tb = (ITestBean) bf.getBean("directTarget");
		assertTrue(tb.getName().equals("Adam"));
		assertEquals(1, cba.getCalls());

		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&directTarget");
		assertTrue("Has correct object type", TestBean.class.isAssignableFrom(pfb.getObjectType()));
	}

	@Test
	public void testGetObjectTypeWithTargetViaTargetSource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));
		ITestBean tb = (ITestBean) bf.getBean("viaTargetSource");
		assertTrue(tb.getName().equals("Adam"));
		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&viaTargetSource");
		assertTrue("Has correct object type", TestBean.class.isAssignableFrom(pfb.getObjectType()));
	}

	@Test
	public void testGetObjectTypeWithNoTargetOrTargetSource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(TARGETSOURCE_CONTEXT, CLASS));

		ITestBean tb = (ITestBean) bf.getBean("noTarget");
		try {
			tb.getName();
			fail();
		}
		catch (UnsupportedOperationException ex) {
			assertEquals("getName", ex.getMessage());
		}
		FactoryBean<?> pfb = (ProxyFactoryBean) bf.getBean("&noTarget");
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(pfb.getObjectType()));
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
		assertEquals("Singleton instances ==", test1, test1_1);
		test1.setAge(25);
		assertEquals(test1.getAge(), test1_1.getAge());
		test1.setAge(250);
		assertEquals(test1.getAge(), test1_1.getAge());
		Advised pc1 = (Advised) test1;
		Advised pc2 = (Advised) test1_1;
		assertArrayEquals(pc1.getAdvisors(), pc2.getAdvisors());
		int oldLength = pc1.getAdvisors().length;
		NopInterceptor di = new NopInterceptor();
		pc1.addAdvice(1, di);
		assertArrayEquals(pc1.getAdvisors(), pc2.getAdvisors());
		assertEquals("Now have one more advisor", oldLength + 1, pc2.getAdvisors().length);
		assertEquals(di.getCount(), 0);
		test1.setAge(5);
		assertEquals(test1_1.getAge(), test1.getAge());
		assertEquals(di.getCount(), 3);
	}

	@Test
	public void testPrototypeInstancesAreNotEqual() {
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(factory.getType("prototype")));
		ITestBean test2 = (ITestBean) factory.getBean("prototype");
		ITestBean test2_1 = (ITestBean) factory.getBean("prototype");
		assertTrue("Prototype instances !=", test2 != test2_1);
		assertTrue("Prototype instances equal", test2.equals(test2_1));
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(factory.getType("prototype")));
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
		assertEquals(INITIAL_COUNT, raw.getCount());
		raw.doWork();
		assertEquals(INITIAL_COUNT+1, raw.getCount());
		raw = (SideEffectBean) bf.getBean("prototypeTarget");
		assertEquals(INITIAL_COUNT, raw.getCount());

		// Now try with advised instances
		SideEffectBean prototype2FirstInstance = (SideEffectBean) bf.getBean(beanName);
		assertEquals(INITIAL_COUNT, prototype2FirstInstance.getCount());
		prototype2FirstInstance.doWork();
		assertEquals(INITIAL_COUNT + 1, prototype2FirstInstance.getCount());

		SideEffectBean prototype2SecondInstance = (SideEffectBean) bf.getBean(beanName);
		assertFalse("Prototypes are not ==", prototype2FirstInstance == prototype2SecondInstance);
		assertEquals(INITIAL_COUNT, prototype2SecondInstance.getCount());
		assertEquals(INITIAL_COUNT + 1, prototype2FirstInstance.getCount());

		return prototype2FirstInstance;
	}

	@Test
	public void testCglibPrototypeInstance() {
		Object prototype = testPrototypeInstancesAreIndependent("cglibPrototype");
		assertTrue("It's a cglib proxy", AopUtils.isCglibProxy(prototype));
		assertFalse("It's not a dynamic proxy", AopUtils.isJdkDynamicProxy(prototype));
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
		assertTrue(autoInvoker.getName().equals(name));
	}

	@Test
	public void testCanGetFactoryReferenceAndManipulate() {
		ProxyFactoryBean config = (ProxyFactoryBean) factory.getBean("&test1");
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(config.getObjectType()));
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(factory.getType("test1")));
		// Trigger lazy initialization.
		config.getObject();
		assertEquals("Have one advisors", 1, config.getAdvisors().length);
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(config.getObjectType()));
		assertTrue("Has correct object type", ITestBean.class.isAssignableFrom(factory.getType("test1")));

		ITestBean tb = (ITestBean) factory.getBean("test1");
		// no exception
		tb.hashCode();

		final Exception ex = new UnsupportedOperationException("invoke");
		// Add evil interceptor to head of list
		config.addAdvice(0, new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw ex;
			}
		});
		assertEquals("Have correct advisor count", 2, config.getAdvisors().length);

		tb = (ITestBean) factory.getBean("test1");
		try {
			// Will fail now
			tb.toString();
			fail("Evil interceptor added programmatically should fail all method calls");
		}
		catch (Exception thrown) {
			assertTrue(thrown == ex);
		}
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
		assertEquals("innerBeanTarget", itb.getName());
		assertEquals("Only have proxy and interceptor: no target", 3, bf.getBeanDefinitionCount());
		DependsOnITestBean doit = (DependsOnITestBean) bf.getBean("autowireCheck");
		assertSame(itb, doit.tb);
	}

	/**
	 * Try adding and removing interfaces and interceptors on prototype.
	 * Changes will only affect future references obtained from the factory.
	 * Each instance will be independent.
	 */
	@Test
	public void testCanAddAndRemoveAspectInterfacesOnPrototype() {
		assertThat("Shouldn't implement TimeStamped before manipulation",
				factory.getBean("test2"), not(instanceOf(TimeStamped.class)));

		ProxyFactoryBean config = (ProxyFactoryBean) factory.getBean("&test2");
		long time = 666L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor();
		ti.setTime(time);
		// Add to head of interceptor chain
		int oldCount = config.getAdvisors().length;
		config.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));
		assertTrue(config.getAdvisors().length == oldCount + 1);

		TimeStamped ts = (TimeStamped) factory.getBean("test2");
		assertEquals(time, ts.getTimeStamp());

		// Can remove
		config.removeAdvice(ti);
		assertTrue(config.getAdvisors().length == oldCount);

		// Check no change on existing object reference
		assertTrue(ts.getTimeStamp() == time);

		assertThat("Should no longer implement TimeStamped",
				factory.getBean("test2"), not(instanceOf(TimeStamped.class)));

		// Now check non-effect of removing interceptor that isn't there
		config.removeAdvice(new DebugInterceptor());
		assertTrue(config.getAdvisors().length == oldCount);

		ITestBean it = (ITestBean) ts;
		DebugInterceptor debugInterceptor = new DebugInterceptor();
		config.addAdvice(0, debugInterceptor);
		it.getSpouse();
		// Won't affect existing reference
		assertTrue(debugInterceptor.getCount() == 0);
		it = (ITestBean) factory.getBean("test2");
		it.getSpouse();
		assertEquals(1, debugInterceptor.getCount());
		config.removeAdvice(debugInterceptor);
		it.getSpouse();

		// Still invoked wiht old reference
		assertEquals(2, debugInterceptor.getCount());

		// not invoked with new object
		it = (ITestBean) factory.getBean("test2");
		it.getSpouse();
		assertEquals(2, debugInterceptor.getCount());

		// Our own timestamped reference should still work
		assertEquals(time, ts.getTimeStamp());
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
		assertEquals(0, di.getCount());
		it.setAge(25);
		assertEquals(25, it.getAge());
		assertEquals(2, di.getCount());
	}

	@Test
	public void testMethodPointcuts() {
		ITestBean tb = (ITestBean) factory.getBean("pointcuts");
		PointcutForVoid.reset();
		assertTrue("No methods intercepted", PointcutForVoid.methodNames.isEmpty());
		tb.getAge();
		assertTrue("Not void: shouldn't have intercepted", PointcutForVoid.methodNames.isEmpty());
		tb.setAge(1);
		tb.getAge();
		tb.setName("Tristan");
		tb.toString();
		assertEquals("Recorded wrong number of invocations", 2, PointcutForVoid.methodNames.size());
		assertTrue(PointcutForVoid.methodNames.get(0).equals("setAge"));
		assertTrue(PointcutForVoid.methodNames.get(1).equals("setName"));
	}

	@Test
	public void testCanAddThrowsAdviceWithoutAdvisor() throws Throwable {
		DefaultListableBeanFactory f = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(f).loadBeanDefinitions(new ClassPathResource(THROWS_ADVICE_CONTEXT, CLASS));
		MyThrowsHandler th = (MyThrowsHandler) f.getBean("throwsAdvice");
		CountingBeforeAdvice cba = (CountingBeforeAdvice) f.getBean("countingBeforeAdvice");
		assertEquals(0, cba.getCalls());
		assertEquals(0, th.getCalls());
		IEcho echo = (IEcho) f.getBean("throwsAdvised");
		int i = 12;
		echo.setA(i);
		assertEquals(i, echo.getA());
		assertEquals(2, cba.getCalls());
		assertEquals(0, th.getCalls());
		Exception expected = new Exception();
		try {
			echo.echoException(1, expected);
			fail();
		}
		catch (Exception ex) {
			assertEquals(expected, ex);
		}
		// No throws handler method: count should still be 0
		assertEquals(0, th.getCalls());

		// Handler knows how to handle this exception
		expected = new FileNotFoundException();
		try {
			echo.echoException(1, expected);
			fail();
		}
		catch (IOException ex) {
			assertEquals(expected, ex);
		}
		// One match
		assertEquals(1, th.getCalls("ioException"));
	}

	// These two fail the whole bean factory
	// TODO put in sep file to check quality of error message
	/*
	@Test
	public void testNoInterceptorNamesWithoutTarget() {
		try {
			ITestBean tb = (ITestBean) factory.getBean("noInterceptorNamesWithoutTarget");
			fail("Should require interceptor names");
		}
		catch (AopConfigException ex) {
			// Ok
		}
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
		try {
			bf.getBean("emptyInterceptorNames");
			fail("Interceptor names cannot be empty");
		}
		catch (BeanCreationException ex) {
			// Ok
		}
	}

	/**
	 * Globals must be followed by a target.
	 */
	@Test
	public void testGlobalsWithoutTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(INVALID_CONTEXT, CLASS));
		try {
			bf.getBean("globalsWithoutTarget");
			fail("Should require target name");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof AopConfigException);
		}
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
		assertTrue(agi.globalsAdded() == -1);

		ProxyFactoryBean pfb = (ProxyFactoryBean) factory.getBean("&validGlobals");
		// Trigger lazy initialization.
		pfb.getObject();
		// 2 globals + 2 explicit
		assertEquals("Have 2 globals and 2 explicit advisors", 3, pfb.getAdvisors().length);

		ApplicationListener<?> l = (ApplicationListener<?>) factory.getBean("validGlobals");
		agi = (AddedGlobalInterface) l;
		assertTrue(agi.globalsAdded() == -1);

		try {
			agi = (AddedGlobalInterface) factory.getBean("test1");
			fail("Aspect interface should't be implemeneted without globals");
		}
		catch (ClassCastException ex) {
		}
	}

	@Test
	public void testSerializableSingletonProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializableSingleton");
		assertSame("Should be a Singleton", p, bf.getBean("serializableSingleton"));
		Person p2 = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertEquals(p, p2);
		assertNotSame(p, p2);
		assertEquals("serializableSingleton", p2.getName());

		// Add unserializable advice
		Advice nop = new NopInterceptor();
		((Advised) p).addAdvice(nop);
		// Check it still works
		assertEquals(p2.getName(), p2.getName());
		assertFalse("Not serializable because an interceptor isn't serializable", SerializationTestUtils.isSerializable(p));

		// Remove offending interceptor...
		assertTrue(((Advised) p).removeAdvice(nop));
		assertTrue("Serializable again because offending interceptor was removed", SerializationTestUtils.isSerializable(p));
	}

	@Test
	public void testSerializablePrototypeProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializablePrototype");
		assertNotSame("Should not be a Singleton", p, bf.getBean("serializablePrototype"));
		Person p2 = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertEquals(p, p2);
		assertNotSame(p, p2);
		assertEquals("serializablePrototype", p2.getName());
	}

	@Test
	public void testSerializableSingletonProxyFactoryBean() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("serializableSingleton");
		ProxyFactoryBean pfb = (ProxyFactoryBean) bf.getBean("&serializableSingleton");
		ProxyFactoryBean pfb2 = (ProxyFactoryBean) SerializationTestUtils.serializeAndDeserialize(pfb);
		Person p2 = (Person) pfb2.getObject();
		assertEquals(p, p2);
		assertNotSame(p, p2);
		assertEquals("serializableSingleton", p2.getName());
	}

	@Test
	public void testProxyNotSerializableBecauseOfAdvice() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(SERIALIZATION_CONTEXT, CLASS));
		Person p = (Person) bf.getBean("interceptorNotSerializableSingleton");
		assertFalse("Not serializable because an interceptor isn't serializable", SerializationTestUtils.isSerializable(p));
	}

	@Test
	public void testPrototypeAdvisor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(CONTEXT, CLASS));

		ITestBean bean1 = (ITestBean) bf.getBean("prototypeTestBeanProxy");
		ITestBean bean2 = (ITestBean) bf.getBean("prototypeTestBeanProxy");

		bean1.setAge(3);
		bean2.setAge(4);

		assertEquals(3, bean1.getAge());
		assertEquals(4, bean2.getAge());

		((Lockable) bean1).lock();

		try {
			bean1.setAge(5);
			fail("expected LockedException");
		}
		catch (LockedException ex) {
			// expected
		}

		try {
			bean2.setAge(6);
		}
		catch (LockedException ex) {
			fail("did not expect LockedException");
		}
	}

	@Test
	public void testPrototypeInterceptorSingletonTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(CONTEXT, CLASS));

		ITestBean bean1 = (ITestBean) bf.getBean("prototypeTestBeanProxySingletonTarget");
		ITestBean bean2 = (ITestBean) bf.getBean("prototypeTestBeanProxySingletonTarget");

		bean1.setAge(1);
		bean2.setAge(2);

		assertEquals(2, bean1.getAge());

		((Lockable) bean1).lock();

		try {
			bean1.setAge(5);
			fail("expected LockedException");
		}
		catch (LockedException ex) {
			// expected
		}

		try {
			bean2.setAge(6);
		}
		catch (LockedException ex) {
			fail("did not expect LockedException");
		}
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

		Advised advised = (Advised)bf.getBean("frozen");
		assertTrue("The proxy should be frozen", advised.isFrozen());
	}

	@Test
	public void testDetectsInterfaces() throws Exception {
		ProxyFactoryBean fb = new ProxyFactoryBean();
		fb.setTarget(new TestBean());
		fb.addAdvice(new DebugInterceptor());
		fb.setBeanFactory(new DefaultListableBeanFactory());
		ITestBean proxy = (ITestBean) fb.getObject();
		assertTrue(AopUtils.isJdkDynamicProxy(proxy));
	}


	/**
	 * Fires only on void methods. Saves list of methods intercepted.
	 */
	@SuppressWarnings("serial")
	public static class PointcutForVoid extends DefaultPointcutAdvisor {

		public static List<String> methodNames = new LinkedList<>();

		public static void reset() {
			methodNames.clear();
		}

		public PointcutForVoid() {
			setAdvice(new MethodInterceptor() {
				@Override
				public Object invoke(MethodInvocation invocation) throws Throwable {
					methodNames.add(invocation.getMethod().getName());
					return invocation.proceed();
				}
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
				return new Integer(-1);
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
