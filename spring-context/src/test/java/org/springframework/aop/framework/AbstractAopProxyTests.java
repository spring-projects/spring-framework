/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.MarshalException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.mixin.LockMixin;
import test.mixin.LockMixinAdvisor;
import test.mixin.Lockable;
import test.mixin.LockedException;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.TargetSource;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.lang.Nullable;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.TimeStamped;
import org.springframework.tests.aop.advice.CountingAfterReturningAdvice;
import org.springframework.tests.aop.advice.CountingBeforeAdvice;
import org.springframework.tests.aop.advice.MethodCounter;
import org.springframework.tests.aop.advice.MyThrowsHandler;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.aop.interceptor.SerializableNopInterceptor;
import org.springframework.tests.aop.interceptor.TimestampIntroductionInterceptor;
import org.springframework.tests.sample.beans.IOther;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StopWatch;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13.03.2003
 */
public abstract class AbstractAopProxyTests {

	protected final MockTargetSource mockTargetSource = new MockTargetSource();


	/**
	 * Make a clean target source available if code wants to use it.
	 * The target must be set. Verification will be automatic in tearDown
	 * to ensure that it was used appropriately by code.
	 */
	@Before
	public void setUp() {
		mockTargetSource.reset();
	}

	@After
	public void tearDown() {
		mockTargetSource.verify();
	}


	/**
	 * Set in CGLIB or JDK mode.
	 */
	protected abstract Object createProxy(ProxyCreatorSupport as);

	protected abstract AopProxy createAopProxy(AdvisedSupport as);

	/**
	 * Is a target always required?
	 */
	protected boolean requiresTarget() {
		return false;
	}


	@Test(expected = AopConfigException.class)
	public void testNoInterceptorsAndNoTarget() {
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		// Add no interceptors
		AopProxy aop = createAopProxy(pc);
		aop.getProxy();
	}

	/**
	 * Simple test that if we set values we can get them out again.
	 */
	@Test
	public void testValuesStick() {
		int age1 = 33;
		int age2 = 37;
		String name = "tony";

		TestBean target1 = new TestBean();
		target1.setAge(age1);
		ProxyFactory pf1 = new ProxyFactory(target1);
		pf1.addAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()));
		pf1.addAdvisor(new DefaultPointcutAdvisor(new TimestampIntroductionInterceptor()));
		ITestBean tb = (ITestBean) pf1.getProxy();

		assertEquals(age1, tb.getAge());
		tb.setAge(age2);
		assertEquals(age2, tb.getAge());
		assertNull(tb.getName());
		tb.setName(name);
		assertEquals(name, tb.getName());
	}

	/**
	 * This is primarily a test for the efficiency of our
	 * usage of CGLIB. If we create too many classes with
	 * CGLIB this will be slow or will run out of memory.
	 */
	@Test
	public void testManyProxies() {
		Assume.group(TestGroup.PERFORMANCE);
		int howMany = 10000;
		StopWatch sw = new StopWatch();
		sw.start("Create " + howMany + " proxies");
		testManyProxies(howMany);
		sw.stop();
		assertTrue("Proxy creation was too slow",  sw.getTotalTimeMillis() < 5000);
	}

	private void testManyProxies(int howMany) {
		int age1 = 33;
		TestBean target1 = new TestBean();
		target1.setAge(age1);
		ProxyFactory pf1 = new ProxyFactory(target1);
		pf1.addAdvice(new NopInterceptor());
		pf1.addAdvice(new NopInterceptor());
		ITestBean[] proxies = new ITestBean[howMany];
		for (int i = 0; i < howMany; i++) {
			proxies[i] = (ITestBean) createAopProxy(pf1).getProxy();
			assertEquals(age1, proxies[i].getAge());
		}
	}

	@Test
	public void testSerializationAdviceAndTargetNotSerializable() throws Exception {
		TestBean tb = new TestBean();
		assertFalse(SerializationTestUtils.isSerializable(tb));

		ProxyFactory pf = new ProxyFactory(tb);

		pf.addAdvice(new NopInterceptor());
		ITestBean proxy = (ITestBean) createAopProxy(pf).getProxy();

		assertFalse(SerializationTestUtils.isSerializable(proxy));
	}

	@Test
	public void testSerializationAdviceNotSerializable() throws Exception {
		SerializablePerson sp = new SerializablePerson();
		assertTrue(SerializationTestUtils.isSerializable(sp));

		ProxyFactory pf = new ProxyFactory(sp);

		// This isn't serializable
		Advice i = new NopInterceptor();
		pf.addAdvice(i);
		assertFalse(SerializationTestUtils.isSerializable(i));
		Object proxy = createAopProxy(pf).getProxy();

		assertFalse(SerializationTestUtils.isSerializable(proxy));
	}

	@Test
	public void testSerializableTargetAndAdvice() throws Throwable {
		SerializablePerson personTarget = new SerializablePerson();
		personTarget.setName("jim");
		personTarget.setAge(26);

		assertTrue(SerializationTestUtils.isSerializable(personTarget));

		ProxyFactory pf = new ProxyFactory(personTarget);

		CountingThrowsAdvice cta = new CountingThrowsAdvice();

		pf.addAdvice(new SerializableNopInterceptor());
		// Try various advice types
		pf.addAdvice(new CountingBeforeAdvice());
		pf.addAdvice(new CountingAfterReturningAdvice());
		pf.addAdvice(cta);
		Person p = (Person) createAopProxy(pf).getProxy();

		p.echo(null);
		assertEquals(0, cta.getCalls());
		try {
			p.echo(new IOException());
		}
		catch (IOException ex) {
			/* expected */
		}
		assertEquals(1, cta.getCalls());

		// Will throw exception if it fails
		Person p2 = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertNotSame(p, p2);
		assertEquals(p.getName(), p2.getName());
		assertEquals(p.getAge(), p2.getAge());
		assertTrue("Deserialized object is an AOP proxy", AopUtils.isAopProxy(p2));

		Advised a1 = (Advised) p;
		Advised a2 = (Advised) p2;
		// Check we can manipulate state of p2
		assertEquals(a1.getAdvisors().length, a2.getAdvisors().length);

		// This should work as SerializablePerson is equal
		assertEquals("Proxies should be equal, even after one was serialized", p, p2);
		assertEquals("Proxies should be equal, even after one was serialized", p2, p);

		// Check we can add a new advisor to the target
		NopInterceptor ni = new NopInterceptor();
		p2.getAge();
		assertEquals(0, ni.getCount());
		a2.addAdvice(ni);
		p2.getAge();
		assertEquals(1, ni.getCount());

		cta = (CountingThrowsAdvice) a2.getAdvisors()[3].getAdvice();
		p2.echo(null);
		assertEquals(1, cta.getCalls());
		try {
			p2.echo(new IOException());
		}
		catch (IOException ex) {

		}
		assertEquals(2, cta.getCalls());
	}

	/**
	 * Check that the two MethodInvocations necessary are independent and
	 * don't conflict.
	 * Check also proxy exposure.
	 */
	@Test
	public void testOneAdvisedObjectCallsAnother() {
		int age1 = 33;
		int age2 = 37;

		TestBean target1 = new TestBean();
		ProxyFactory pf1 = new ProxyFactory(target1);
		// Permit proxy and invocation checkers to get context from AopContext
		pf1.setExposeProxy(true);
		NopInterceptor di1 = new NopInterceptor();
		pf1.addAdvice(0, di1);
		pf1.addAdvice(1, new ProxyMatcherInterceptor());
		pf1.addAdvice(2, new CheckMethodInvocationIsSameInAndOutInterceptor());
		pf1.addAdvice(1, new CheckMethodInvocationViaThreadLocalIsSameInAndOutInterceptor());
		// Must be first
		pf1.addAdvice(0, ExposeInvocationInterceptor.INSTANCE);
		ITestBean advised1 = (ITestBean) pf1.getProxy();
		advised1.setAge(age1); // = 1 invocation

		TestBean target2 = new TestBean();
		ProxyFactory pf2 = new ProxyFactory(target2);
		pf2.setExposeProxy(true);
		NopInterceptor di2 = new NopInterceptor();
		pf2.addAdvice(0, di2);
		pf2.addAdvice(1, new ProxyMatcherInterceptor());
		pf2.addAdvice(2, new CheckMethodInvocationIsSameInAndOutInterceptor());
		pf2.addAdvice(1, new CheckMethodInvocationViaThreadLocalIsSameInAndOutInterceptor());
		pf2.addAdvice(0, ExposeInvocationInterceptor.INSTANCE);
		ITestBean advised2 = (ITestBean) createProxy(pf2);
		advised2.setAge(age2);
		advised1.setSpouse(advised2); // = 2 invocations

		assertEquals("Advised one has correct age", age1, advised1.getAge()); // = 3 invocations
		assertEquals("Advised two has correct age", age2, advised2.getAge());
		// Means extra call on advised 2
		assertEquals("Advised one spouse has correct age", age2, advised1.getSpouse().getAge()); // = 4 invocations on 1 and another one on 2

		assertEquals("one was invoked correct number of times", 4, di1.getCount());
		// Got hit by call to advised1.getSpouse().getAge()
		assertEquals("one was invoked correct number of times", 3, di2.getCount());
	}


	@Test
	public void testReentrance() {
		int age1 = 33;

		TestBean target1 = new TestBean();
		ProxyFactory pf1 = new ProxyFactory(target1);
		NopInterceptor di1 = new NopInterceptor();
		pf1.addAdvice(0, di1);
		ITestBean advised1 = (ITestBean) createProxy(pf1);
		advised1.setAge(age1); // = 1 invocation
		advised1.setSpouse(advised1); // = 2 invocations

		assertEquals("one was invoked correct number of times", 2, di1.getCount());

		assertEquals("Advised one has correct age", age1, advised1.getAge()); // = 3 invocations
		assertEquals("one was invoked correct number of times", 3, di1.getCount());

		// = 5 invocations, as reentrant call to spouse is advised also
		assertEquals("Advised spouse has correct age", age1, advised1.getSpouse().getAge());

		assertEquals("one was invoked correct number of times", 5, di1.getCount());
	}

	@Test
	public void testTargetCanGetProxy() {
		NopInterceptor di = new NopInterceptor();
		INeedsToSeeProxy target = new TargetChecker();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setExposeProxy(true);
		assertTrue(proxyFactory.isExposeProxy());

		proxyFactory.addAdvice(0, di);
		INeedsToSeeProxy proxied = (INeedsToSeeProxy) createProxy(proxyFactory);
		assertEquals(0, di.getCount());
		assertEquals(0, target.getCount());
		proxied.incrementViaThis();
		assertEquals("Increment happened", 1, target.getCount());

		assertEquals("Only one invocation via AOP as use of this wasn't proxied", 1, di.getCount());
		// 1 invocation
		assertEquals("Increment happened", 1, proxied.getCount());
		proxied.incrementViaProxy(); // 2 invocations
		assertEquals("Increment happened", 2, target.getCount());
		assertEquals("3 more invocations via AOP as the first call was reentrant through the proxy", 4, di.getCount());
	}

	@Test(expected = IllegalStateException.class)
	// Should fail to get proxy as exposeProxy wasn't set to true
	public void testTargetCantGetProxyByDefault() {
		NeedsToSeeProxy et = new NeedsToSeeProxy();
		ProxyFactory pf1 = new ProxyFactory(et);
		assertFalse(pf1.isExposeProxy());
		INeedsToSeeProxy proxied = (INeedsToSeeProxy) createProxy(pf1);
		proxied.incrementViaProxy();
	}

	@Test
	public void testContext() throws Throwable {
		testContext(true);
	}

	@Test
	public void testNoContext() throws Throwable {
		testContext(false);
	}

	/**
	 * @param context if true, want context
	 */
	private void testContext(final boolean context) throws Throwable {
		final String s = "foo";
		// Test return value
		MethodInterceptor mi = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				if (!context) {
					assertNoInvocationContext();
				}
				else {
					assertNotNull("have context", ExposeInvocationInterceptor.currentInvocation());
				}
				return s;
			}
		};
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		if (context) {
			pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		}
		pc.addAdvice(mi);
		// Keep CGLIB happy
		if (requiresTarget()) {
			pc.setTarget(new TestBean());
		}
		AopProxy aop = createAopProxy(pc);

		assertNoInvocationContext();
		ITestBean tb = (ITestBean) aop.getProxy();
		assertNoInvocationContext();
		assertSame("correct return value", s, tb.getName());
	}

	/**
	 * Test that the proxy returns itself when the
	 * target returns {@code this}
	 */
	@Test
	public void testTargetReturnsThis() throws Throwable {
		// Test return value
		TestBean raw = new OwnSpouse();

		ProxyCreatorSupport pc = new ProxyCreatorSupport();
		pc.setInterfaces(ITestBean.class);
		pc.setTarget(raw);

		ITestBean tb = (ITestBean) createProxy(pc);
		assertSame("this return is wrapped in proxy", tb, tb.getSpouse());
	}

	@Test
	public void testDeclaredException() throws Throwable {
		final Exception expectedException = new Exception();
		// Test return value
		MethodInterceptor mi = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw expectedException;
			}
		};
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		pc.addAdvice(mi);

		// We don't care about the object
		mockTargetSource.setTarget(new TestBean());
		pc.setTargetSource(mockTargetSource);
		AopProxy aop = createAopProxy(pc);

		try {
			ITestBean tb = (ITestBean) aop.getProxy();
			// Note: exception param below isn't used
			tb.exceptional(expectedException);
			fail("Should have thrown exception raised by interceptor");
		}
		catch (Exception thrown) {
			assertEquals("exception matches", expectedException, thrown);
		}
	}

	/**
	 * An interceptor throws a checked exception not on the method signature.
	 * For efficiency, we don't bother unifying java.lang.reflect and
	 * org.springframework.cglib UndeclaredThrowableException
	 */
	@Test
	public void testUndeclaredCheckedException() throws Throwable {
		final Exception unexpectedException = new Exception();
		// Test return value
		MethodInterceptor mi = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw unexpectedException;
			}
		};
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		pc.addAdvice(mi);

		// We don't care about the object
		pc.setTarget(new TestBean());
		AopProxy aop = createAopProxy(pc);
		ITestBean tb = (ITestBean) aop.getProxy();

		try {
			// Note: exception param below isn't used
			tb.getAge();
			fail("Should have wrapped exception raised by interceptor");
		}
		catch (UndeclaredThrowableException thrown) {
			assertEquals("exception matches", unexpectedException, thrown.getUndeclaredThrowable());
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Didn't expect exception: " + ex);
		}
	}

	@Test
	public void testUndeclaredUncheckedException() throws Throwable {
		final RuntimeException unexpectedException = new RuntimeException();
		// Test return value
		MethodInterceptor mi = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw unexpectedException;
			}
		};
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		pc.addAdvice(mi);

		// We don't care about the object
		pc.setTarget(new TestBean());
		AopProxy aop = createAopProxy(pc);
		ITestBean tb = (ITestBean) aop.getProxy();

		try {
			// Note: exception param below isn't used
			tb.getAge();
			fail("Should have wrapped exception raised by interceptor");
		}
		catch (RuntimeException thrown) {
			assertEquals("exception matches", unexpectedException, thrown);
		}
	}

	/**
	 * Check that although a method is eligible for advice chain optimization and
	 * direct reflective invocation, it doesn't happen if we've asked to see the proxy,
	 * so as to guarantee a consistent programming model.
	 * @throws Throwable
	 */
	@Test
	public void testTargetCanGetInvocationEvenIfNoAdviceChain() throws Throwable {
		NeedsToSeeProxy target = new NeedsToSeeProxy();
		AdvisedSupport pc = new AdvisedSupport(INeedsToSeeProxy.class);
		pc.setTarget(target);
		pc.setExposeProxy(true);

		// Now let's try it with the special target
		AopProxy aop = createAopProxy(pc);
		INeedsToSeeProxy proxied = (INeedsToSeeProxy) aop.getProxy();
		// It will complain if it can't get the proxy
		proxied.incrementViaProxy();
	}

	@Test
	public void testTargetCanGetInvocation() throws Throwable {
		final InvocationCheckExposedInvocationTestBean expectedTarget = new InvocationCheckExposedInvocationTestBean();

		AdvisedSupport pc = new AdvisedSupport(ITestBean.class, IOther.class);
		pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		TrapTargetInterceptor tii = new TrapTargetInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				// Assert that target matches BEFORE invocation returns
				assertEquals("Target is correct", expectedTarget, invocation.getThis());
				return super.invoke(invocation);
			}
		};
		pc.addAdvice(tii);
		pc.setTarget(expectedTarget);
		AopProxy aop = createAopProxy(pc);

		ITestBean tb = (ITestBean) aop.getProxy();
		tb.getName();
	}

	/**
	 * Throw an exception if there is an Invocation.
	 */
	private void assertNoInvocationContext() {
		try {
			ExposeInvocationInterceptor.currentInvocation();
			fail("Expected no invocation context");
		}
		catch (IllegalStateException ex) {
			// ok
		}
	}

	/**
	 * Test stateful interceptor
	 */
	@Test
	public void testMixinWithIntroductionAdvisor() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		pc.addAdvisor(new LockMixinAdvisor());
		pc.setTarget(tb);

		testTestBeanIntroduction(pc);
	}

	@Test
	public void testMixinWithIntroductionInfo() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		// We don't use an IntroductionAdvisor, we can just add an advice that implements IntroductionInfo
		pc.addAdvice(new LockMixin());
		pc.setTarget(tb);

		testTestBeanIntroduction(pc);
	}

	private void testTestBeanIntroduction(ProxyFactory pc) {
		int newAge = 65;
		ITestBean itb = (ITestBean) createProxy(pc);
		itb.setAge(newAge);
		assertEquals(newAge, itb.getAge());

		Lockable lockable = (Lockable) itb;
		assertFalse(lockable.locked());
		lockable.lock();

		assertEquals(newAge, itb.getAge());
		try {
			itb.setAge(1);
			fail("Setters should fail when locked");
		}
		catch (LockedException ex) {
			// ok
		}
		assertEquals(newAge, itb.getAge());

		// Unlock
		assertTrue(lockable.locked());
		lockable.unlock();
		itb.setAge(1);
		assertEquals(1, itb.getAge());
	}

	@Test
	public void testReplaceArgument() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		pc.setTarget(tb);
		pc.addAdvisor(new StringSetterNullReplacementAdvice());

		ITestBean t = (ITestBean) pc.getProxy();
		int newAge = 5;
		t.setAge(newAge);
		assertEquals(newAge, t.getAge());
		String newName = "greg";
		t.setName(newName);
		assertEquals(newName, t.getName());

		t.setName(null);
		// Null replacement magic should work
		assertEquals("", t.getName());
	}

	@Test
	public void testCanCastProxyToProxyConfig() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory(tb);
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(0, di);

		ITestBean t = (ITestBean) createProxy(pc);
		assertEquals(0, di.getCount());
		t.setAge(23);
		assertEquals(23, t.getAge());
		assertEquals(2, di.getCount());

		Advised advised = (Advised) t;
		assertEquals("Have 1 advisor", 1, advised.getAdvisors().length);
		assertEquals(di, advised.getAdvisors()[0].getAdvice());
		NopInterceptor di2 = new NopInterceptor();
		advised.addAdvice(1, di2);
		t.getName();
		assertEquals(3, di.getCount());
		assertEquals(1, di2.getCount());
		// will remove di
		advised.removeAdvisor(0);
		t.getAge();
		// Unchanged
		assertEquals(3, di.getCount());
		assertEquals(2, di2.getCount());

		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		assertEquals(0, cba.getCalls());
		advised.addAdvice(cba);
		t.setAge(16);
		assertEquals(16, t.getAge());
		assertEquals(2, cba.getCalls());
	}

	@Test
	public void testAdviceImplementsIntroductionInfo() throws Throwable {
		TestBean tb = new TestBean();
		String name = "tony";
		tb.setName(name);
		ProxyFactory pc = new ProxyFactory(tb);
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(di);
		final long ts = 37;
		pc.addAdvice(new DelegatingIntroductionInterceptor(new TimeStamped() {
			@Override
			public long getTimeStamp() {
				return ts;
			}
		}));

		ITestBean proxied = (ITestBean) createProxy(pc);
		assertEquals(name, proxied.getName());
		TimeStamped intro = (TimeStamped) proxied;
		assertEquals(ts, intro.getTimeStamp());
	}

	@Test
	public void testCannotAddDynamicIntroductionAdviceExceptInIntroductionAdvice() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		try {
			pc.addAdvice(new DummyIntroductionAdviceImpl());
			fail("Shouldn't be able to add introduction interceptor except via introduction advice");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().contains("ntroduction"));
		}
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertEquals(target.getAge(), proxied.getAge());
	}

	@Test
	public void testRejectsBogusDynamicIntroductionAdviceWithNoAdapter() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		pc.addAdvisor(new DefaultIntroductionAdvisor(new DummyIntroductionAdviceImpl(), Comparable.class));
		try {
			// TODO May fail on either call: may want to tighten up definition
			ITestBean proxied = (ITestBean) createProxy(pc);
			proxied.getName();
			fail("Bogus introduction");
		}
		catch (Exception ex) {
			// TODO used to catch UnknownAdviceTypeException, but
			// with CGLIB some errors are in proxy creation and are wrapped
			// in aspect exception. Error message is still fine.
			//assertTrue(ex.getMessage().indexOf("ntroduction") > -1);
		}
	}

	/**
	 * Check that the introduction advice isn't allowed to introduce interfaces
	 * that are unsupported by the IntroductionInterceptor.
	 */
	@Test
	public void testCannotAddIntroductionAdviceWithUnimplementedInterface() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		try {
			pc.addAdvisor(0, new DefaultIntroductionAdvisor(new TimestampIntroductionInterceptor(), ITestBean.class));
			fail("Shouldn't be able to add introduction advice introducing an unimplemented interface");
		}
		catch (IllegalArgumentException ex) {
			//assertTrue(ex.getMessage().indexOf("ntroduction") > -1);
		}
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertEquals(target.getAge(), proxied.getAge());
	}

	/**
	 * Note that an introduction can't throw an unexpected checked exception,
	 * as it's constrained by the interface.
	 */
	@Test
	public void testIntroductionThrowsUncheckedException() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);

		@SuppressWarnings("serial")
		class MyDi extends DelegatingIntroductionInterceptor implements TimeStamped {
			/**
			 * @see test.util.TimeStamped#getTimeStamp()
			 */
			@Override
			public long getTimeStamp() {
				throw new UnsupportedOperationException();
			}
		}
		pc.addAdvisor(new DefaultIntroductionAdvisor(new MyDi()));

		TimeStamped ts = (TimeStamped) createProxy(pc);
		try {
			ts.getTimeStamp();
			fail("Should throw UnsupportedOperationException");
		}
		catch (UnsupportedOperationException ex) {
		}
	}

	/**
	 * Should only be able to introduce interfaces, not classes.
	 */
	@Test
	public void testCannotAddIntroductionAdviceToIntroduceClass() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		try {
			pc.addAdvisor(0, new DefaultIntroductionAdvisor(new TimestampIntroductionInterceptor(), TestBean.class));
			fail("Shouldn't be able to add introduction advice that introduces a class, rather than an interface");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("interface"));
		}
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertEquals(target.getAge(), proxied.getAge());
	}

	@Test
	public void testCannotAddInterceptorWhenFrozen() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertFalse(pc.isFrozen());
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		try {
			pc.addAdvice(0, new NopInterceptor());
			fail("Shouldn't be able to add interceptor when frozen");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().contains("frozen"));
		}
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(1, ((Advised) proxied).getAdvisors().length);
	}

	/**
	 * Check that casting to Advised can't get around advice freeze.
	 */
	@Test
	public void testCannotAddAdvisorWhenFrozenUsingCast() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertFalse(pc.isFrozen());
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		Advised advised = (Advised) proxied;

		assertTrue(pc.isFrozen());
		try {
			advised.addAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()));
			fail("Shouldn't be able to add Advisor when frozen");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().contains("frozen"));
		}
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(1, advised.getAdvisors().length);
	}

	@Test
	public void testCannotRemoveAdvisorWhenFrozen() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertFalse(pc.isFrozen());
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		Advised advised = (Advised) proxied;

		assertTrue(pc.isFrozen());
		try {
			advised.removeAdvisor(0);
			fail("Shouldn't be able to remove Advisor when frozen");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().contains("frozen"));
		}
		// Didn't get removed
		assertEquals(1, advised.getAdvisors().length);
		pc.setFrozen(false);
		// Can now remove it
		advised.removeAdvisor(0);
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(0, advised.getAdvisors().length);
	}

	@Test
	public void testUseAsHashKey() {
		TestBean target1 = new TestBean();
		ProxyFactory pf1 = new ProxyFactory(target1);
		pf1.addAdvice(new NopInterceptor());
		ITestBean proxy1 = (ITestBean) createProxy(pf1);

		TestBean target2 = new TestBean();
		ProxyFactory pf2 = new ProxyFactory(target2);
		pf2.addAdvisor(new DefaultIntroductionAdvisor(new TimestampIntroductionInterceptor()));
		ITestBean proxy2 = (ITestBean) createProxy(pf2);

		HashMap<ITestBean, Object> h = new HashMap<>();
		Object value1 = "foo";
		Object value2 = "bar";
		assertNull(h.get(proxy1));
		h.put(proxy1, value1);
		h.put(proxy2, value2);
		assertEquals(h.get(proxy1), value1);
		assertEquals(h.get(proxy2), value2);
	}

	/**
	 * Check that the string is informative.
	 */
	@Test
	public void testProxyConfigString() {
		TestBean target = new TestBean();
		ProxyFactory pc = new ProxyFactory(target);
		pc.setInterfaces(ITestBean.class);
		pc.addAdvice(new NopInterceptor());
		MethodBeforeAdvice mba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(new NameMatchMethodPointcut(), mba);
		pc.addAdvisor(advisor);
		ITestBean proxied = (ITestBean) createProxy(pc);

		String proxyConfigString = ((Advised) proxied).toProxyConfigString();
		assertTrue(proxyConfigString.contains(advisor.toString()));
		assertTrue(proxyConfigString.contains("1 interface"));
	}

	@Test
	public void testCanPreventCastToAdvisedUsingOpaque() {
		TestBean target = new TestBean();
		ProxyFactory pc = new ProxyFactory(target);
		pc.setInterfaces(ITestBean.class);
		pc.addAdvice(new NopInterceptor());
		CountingBeforeAdvice mba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(new NameMatchMethodPointcut().addMethodName("setAge"), mba);
		pc.addAdvisor(advisor);
		assertFalse("Opaque defaults to false", pc.isOpaque());
		pc.setOpaque(true);
		assertTrue("Opaque now true for this config", pc.isOpaque());
		ITestBean proxied = (ITestBean) createProxy(pc);
		proxied.setAge(10);
		assertEquals(10, proxied.getAge());
		assertEquals(1, mba.getCalls());

		assertFalse("Cannot be cast to Advised", proxied instanceof Advised);
	}

	@Test
	public void testAdviceSupportListeners() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);

		ProxyFactory pc = new ProxyFactory(target);
		CountingAdvisorListener l = new CountingAdvisorListener(pc);
		pc.addListener(l);
		RefreshCountingAdvisorChainFactory acf = new RefreshCountingAdvisorChainFactory();
		// Should be automatically added as a listener
		pc.addListener(acf);
		assertFalse(pc.isActive());
		assertEquals(0, l.activates);
		assertEquals(0, acf.refreshes);
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertEquals(1, acf.refreshes);
		assertEquals(1, l.activates);
		assertTrue(pc.isActive());
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(0, l.adviceChanges);
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(0, di);
		assertEquals(1, l.adviceChanges);
		assertEquals(2, acf.refreshes);
		assertEquals(target.getAge(), proxied.getAge());
		pc.removeAdvice(di);
		assertEquals(2, l.adviceChanges);
		assertEquals(3, acf.refreshes);
		assertEquals(target.getAge(), proxied.getAge());
		pc.getProxy();
		assertEquals(1, l.activates);

		pc.removeListener(l);
		assertEquals(2, l.adviceChanges);
		pc.addAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()));
		// No longer counting
		assertEquals(2, l.adviceChanges);
	}

	@Test
	public void testExistingProxyChangesTarget() throws Throwable {
		TestBean tb1 = new TestBean();
		tb1.setAge(33);

		TestBean tb2 = new TestBean();
		tb2.setAge(26);
		tb2.setName("Juergen");
		TestBean tb3 = new TestBean();
		tb3.setAge(37);
		ProxyFactory pc = new ProxyFactory(tb1);
		NopInterceptor nop = new NopInterceptor();
		pc.addAdvice(nop);
		ITestBean proxy = (ITestBean) createProxy(pc);
		assertEquals(nop.getCount(), 0);
		assertEquals(tb1.getAge(), proxy.getAge());
		assertEquals(nop.getCount(), 1);
		// Change to a new static target
		pc.setTarget(tb2);
		assertEquals(tb2.getAge(), proxy.getAge());
		assertEquals(nop.getCount(), 2);

		// Change to a new dynamic target
		HotSwappableTargetSource hts = new HotSwappableTargetSource(tb3);
		pc.setTargetSource(hts);
		assertEquals(tb3.getAge(), proxy.getAge());
		assertEquals(nop.getCount(), 3);
		hts.swap(tb1);
		assertEquals(tb1.getAge(), proxy.getAge());
		tb1.setName("Colin");
		assertEquals(tb1.getName(), proxy.getName());
		assertEquals(nop.getCount(), 5);

		// Change back, relying on casting to Advised
		Advised advised = (Advised) proxy;
		assertSame(hts, advised.getTargetSource());
		SingletonTargetSource sts = new SingletonTargetSource(tb2);
		advised.setTargetSource(sts);
		assertEquals(tb2.getName(), proxy.getName());
		assertSame(sts, advised.getTargetSource());
		assertEquals(tb2.getAge(), proxy.getAge());
	}

	@Test
	public void testDynamicMethodPointcutThatAlwaysAppliesStatically() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		TestDynamicPointcutAdvice dp = new TestDynamicPointcutAdvice(new NopInterceptor(), "getAge");
		pc.addAdvisor(dp);
		pc.setTarget(tb);
		ITestBean it = (ITestBean) createProxy(pc);
		assertEquals(0, dp.count);
		it.getAge();
		assertEquals(1, dp.count);
		it.setAge(11);
		assertEquals(11, it.getAge());
		assertEquals(2, dp.count);
	}

	@Test
	public void testDynamicMethodPointcutThatAppliesStaticallyOnlyToSetters() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		// Could apply dynamically to getAge/setAge but not to getName
		TestDynamicPointcutForSettersOnly dp = new TestDynamicPointcutForSettersOnly(new NopInterceptor(), "Age");
		pc.addAdvisor(dp);
		this.mockTargetSource.setTarget(tb);
		pc.setTargetSource(mockTargetSource);
		ITestBean it = (ITestBean) createProxy(pc);
		assertEquals(0, dp.count);
		it.getAge();
		// Statically vetoed
		assertEquals(0, dp.count);
		it.setAge(11);
		assertEquals(11, it.getAge());
		assertEquals(1, dp.count);
		// Applies statically but not dynamically
		it.setName("joe");
		assertEquals(1, dp.count);
	}

	@Test
	public void testStaticMethodPointcut() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory();
		pc.addInterface(ITestBean.class);
		NopInterceptor di = new NopInterceptor();
		TestStaticPointcutAdvice sp = new TestStaticPointcutAdvice(di, "getAge");
		pc.addAdvisor(sp);
		pc.setTarget(tb);
		ITestBean it = (ITestBean) createProxy(pc);
		assertEquals(di.getCount(), 0);
		it.getAge();
		assertEquals(di.getCount(), 1);
		it.setAge(11);
		assertEquals(it.getAge(), 11);
		assertEquals(di.getCount(), 2);
	}

	/**
	 * There are times when we want to call proceed() twice.
	 * We can do this if we clone the invocation.
	 */
	@Test
	public void testCloneInvocationToProceedThreeTimes() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory(tb);
		pc.addInterface(ITestBean.class);

		MethodInterceptor twoBirthdayInterceptor = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation mi) throws Throwable {
				// Clone the invocation to proceed three times
				// "The Moor's Last Sigh": this technology can cause premature aging
				MethodInvocation clone1 = ((ReflectiveMethodInvocation) mi).invocableClone();
				MethodInvocation clone2 = ((ReflectiveMethodInvocation) mi).invocableClone();
				clone1.proceed();
				clone2.proceed();
				return mi.proceed();
			}
		};
		@SuppressWarnings("serial")
		StaticMethodMatcherPointcutAdvisor advisor = new StaticMethodMatcherPointcutAdvisor(twoBirthdayInterceptor) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return "haveBirthday".equals(m.getName());
			}
		};
		pc.addAdvisor(advisor);
		ITestBean it = (ITestBean) createProxy(pc);

		final int age = 20;
		it.setAge(age);
		assertEquals(age, it.getAge());
		// Should return the age before the third, AOP-induced birthday
		assertEquals(age + 2, it.haveBirthday());
		// Return the final age produced by 3 birthdays
		assertEquals(age + 3, it.getAge());
	}

	/**
	 * We want to change the arguments on a clone: it shouldn't affect the original.
	 */
	@Test
	public void testCanChangeArgumentsIndependentlyOnClonedInvocation() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory(tb);
		pc.addInterface(ITestBean.class);

		/**
		 * Changes the name, then changes it back.
		 */
		MethodInterceptor nameReverter = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation mi) throws Throwable {
				MethodInvocation clone = ((ReflectiveMethodInvocation) mi).invocableClone();
				String oldName = ((ITestBean) mi.getThis()).getName();
				clone.getArguments()[0] = oldName;
				// Original method invocation should be unaffected by changes to argument list of clone
				mi.proceed();
				return clone.proceed();
			}
		};

		class NameSaver implements MethodInterceptor {
			private List<Object> names = new LinkedList<>();

			@Override
			public Object invoke(MethodInvocation mi) throws Throwable {
				names.add(mi.getArguments()[0]);
				return mi.proceed();
			}
		}

		NameSaver saver = new NameSaver();

		pc.addAdvisor(new DefaultPointcutAdvisor(Pointcuts.SETTERS, nameReverter));
		pc.addAdvisor(new DefaultPointcutAdvisor(Pointcuts.SETTERS, saver));
		ITestBean it = (ITestBean) createProxy(pc);

		String name1 = "tony";
		String name2 = "gordon";

		tb.setName(name1);
		assertEquals(name1, tb.getName());

		it.setName(name2);
		// NameReverter saved it back
		assertEquals(name1, it.getName());
		assertEquals(2, saver.names.size());
		assertEquals(name2, saver.names.get(0));
		assertEquals(name1, saver.names.get(1));
	}

	@SuppressWarnings("serial")
	@Test
	public void testOverloadedMethodsWithDifferentAdvice() throws Throwable {
		Overloads target = new Overloads();
		ProxyFactory pc = new ProxyFactory(target);

		NopInterceptor overLoadVoids = new NopInterceptor();
		pc.addAdvisor(new StaticMethodMatcherPointcutAdvisor(overLoadVoids) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getName().equals("overload") && m.getParameterCount() == 0;
			}
		});

		NopInterceptor overLoadInts = new NopInterceptor();
		pc.addAdvisor(new StaticMethodMatcherPointcutAdvisor(overLoadInts) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getName().equals("overload") && m.getParameterCount() == 1 &&
						m.getParameterTypes()[0].equals(int.class);
			}
		});

		IOverloads proxy = (IOverloads) createProxy(pc);
		assertEquals(0, overLoadInts.getCount());
		assertEquals(0, overLoadVoids.getCount());
		proxy.overload();
		assertEquals(0, overLoadInts.getCount());
		assertEquals(1, overLoadVoids.getCount());
		assertEquals(25, proxy.overload(25));
		assertEquals(1, overLoadInts.getCount());
		assertEquals(1, overLoadVoids.getCount());
		proxy.noAdvice();
		assertEquals(1, overLoadInts.getCount());
		assertEquals(1, overLoadVoids.getCount());
	}

	@Test
	public void testProxyIsBoundBeforeTargetSourceInvoked() {
		final TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new DebugInterceptor());
		pf.setExposeProxy(true);
		final ITestBean proxy = (ITestBean) createProxy(pf);
		Advised config = (Advised) proxy;

		// This class just checks proxy is bound before getTarget() call
		config.setTargetSource(new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return TestBean.class;
			}
			@Override
			public boolean isStatic() {
				return false;
			}
			@Override
			public Object getTarget() throws Exception {
				assertEquals(proxy, AopContext.currentProxy());
				return target;
			}
			@Override
			public void releaseTarget(Object target) throws Exception {
			}
		});

		// Just test anything: it will fail if context wasn't found
		assertEquals(0, proxy.getAge());
	}

	@Test
	public void testEquals() {
		IOther a = new AllInstancesAreEqual();
		IOther b = new AllInstancesAreEqual();
		NopInterceptor i1 = new NopInterceptor();
		NopInterceptor i2 = new NopInterceptor();
		ProxyFactory pfa = new ProxyFactory(a);
		pfa.addAdvice(i1);
		ProxyFactory pfb = new ProxyFactory(b);
		pfb.addAdvice(i2);
		IOther proxyA = (IOther) createProxy(pfa);
		IOther proxyB = (IOther) createProxy(pfb);

		assertEquals(pfa.getAdvisors().length, pfb.getAdvisors().length);
		assertEquals(a, b);
		assertEquals(i1, i2);
		assertEquals(proxyA, proxyB);
		assertEquals(proxyA.hashCode(), proxyB.hashCode());
		assertFalse(proxyA.equals(a));

		// Equality checks were handled by the proxy
		assertEquals(0, i1.getCount());

		// When we invoke A, it's NopInterceptor will have count == 1
		// and won't think it's equal to B's NopInterceptor
		proxyA.absquatulate();
		assertEquals(1, i1.getCount());
		assertFalse(proxyA.equals(proxyB));
	}

	@Test
	public void testBeforeAdvisorIsInvoked() {
		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		@SuppressWarnings("serial")
		Advisor matchesNoArgs = new StaticMethodMatcherPointcutAdvisor(cba) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getParameterCount() == 0;
			}
		};
		TestBean target = new TestBean();
		target.setAge(80);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvisor(matchesNoArgs);
		assertEquals("Advisor was added", matchesNoArgs, pf.getAdvisors()[1]);
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertEquals(0, cba.getCalls());
		assertEquals(0, cba.getCalls("getAge"));
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(1, cba.getCalls());
		assertEquals(1, cba.getCalls("getAge"));
		assertEquals(0, cba.getCalls("setAge"));
		// Won't be advised
		proxied.setAge(26);
		assertEquals(1, cba.getCalls());
		assertEquals(26, proxied.getAge());
	}

	@Test
	public void testUserAttributes() throws Throwable {
		class MapAwareMethodInterceptor implements MethodInterceptor {
			private final Map<String, String> expectedValues;
			private final Map<String, String> valuesToAdd;
			public MapAwareMethodInterceptor(Map<String, String> expectedValues, Map<String, String> valuesToAdd) {
				this.expectedValues = expectedValues;
				this.valuesToAdd = valuesToAdd;
			}
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				ReflectiveMethodInvocation rmi = (ReflectiveMethodInvocation) invocation;
				for (Iterator<String> it = rmi.getUserAttributes().keySet().iterator(); it.hasNext(); ){
					Object key = it.next();
					assertEquals(expectedValues.get(key), rmi.getUserAttributes().get(key));
				}
				rmi.getUserAttributes().putAll(valuesToAdd);
				return invocation.proceed();
			}
		}
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		MapAwareMethodInterceptor mami1 = new MapAwareMethodInterceptor(new HashMap<>(), new HashMap<String, String>());
		Map<String, String> firstValuesToAdd = new HashMap<>();
		firstValuesToAdd.put("test", "");
		MapAwareMethodInterceptor mami2 = new MapAwareMethodInterceptor(new HashMap<>(), firstValuesToAdd);
		MapAwareMethodInterceptor mami3 = new MapAwareMethodInterceptor(firstValuesToAdd, new HashMap<>());
		MapAwareMethodInterceptor mami4 = new MapAwareMethodInterceptor(firstValuesToAdd, new HashMap<>());
		Map<String, String> secondValuesToAdd = new HashMap<>();
		secondValuesToAdd.put("foo", "bar");
		secondValuesToAdd.put("cat", "dog");
		MapAwareMethodInterceptor mami5 = new MapAwareMethodInterceptor(firstValuesToAdd, secondValuesToAdd);
		Map<String, String> finalExpected = new HashMap<>(firstValuesToAdd);
		finalExpected.putAll(secondValuesToAdd);
		MapAwareMethodInterceptor mami6 = new MapAwareMethodInterceptor(finalExpected, secondValuesToAdd);

		pc.addAdvice(mami1);
		pc.addAdvice(mami2);
		pc.addAdvice(mami3);
		pc.addAdvice(mami4);
		pc.addAdvice(mami5);
		pc.addAdvice(mami6);

		// We don't care about the object
		pc.setTarget(new TestBean());
		AopProxy aop = createAopProxy(pc);
		ITestBean tb = (ITestBean) aop.getProxy();

		String newName = "foo";
		tb.setName(newName);
		assertEquals(newName, tb.getName());
	}

	@Test
	public void testMultiAdvice() throws Throwable {
		CountingMultiAdvice cca = new CountingMultiAdvice();
		@SuppressWarnings("serial")
		Advisor matchesNoArgs = new StaticMethodMatcherPointcutAdvisor(cca) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getParameterCount() == 0 || "exceptional".equals(m.getName());
			}
		};
		TestBean target = new TestBean();
		target.setAge(80);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvisor(matchesNoArgs);
		assertEquals("Advisor was added", matchesNoArgs, pf.getAdvisors()[1]);
		ITestBean proxied = (ITestBean) createProxy(pf);

		assertEquals(0, cca.getCalls());
		assertEquals(0, cca.getCalls("getAge"));
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(2, cca.getCalls());
		assertEquals(2, cca.getCalls("getAge"));
		assertEquals(0, cca.getCalls("setAge"));
		// Won't be advised
		proxied.setAge(26);
		assertEquals(2, cca.getCalls());
		assertEquals(26, proxied.getAge());
		assertEquals(4, cca.getCalls());
		try {
			proxied.exceptional(new SpecializedUncheckedException("foo", (SQLException)null));
			fail("Should have thrown CannotGetJdbcConnectionException");
		}
		catch (SpecializedUncheckedException ex) {
			// expected
		}
		assertEquals(6, cca.getCalls());
	}

	@Test
	public void testBeforeAdviceThrowsException() {
		final RuntimeException rex = new RuntimeException();
		@SuppressWarnings("serial")
		CountingBeforeAdvice ba = new CountingBeforeAdvice() {
			@Override
			public void before(Method m, Object[] args, Object target) throws Throwable {
				super.before(m, args, target);
				if (m.getName().startsWith("set"))
					throw rex;
			}
		};

		TestBean target = new TestBean();
		target.setAge(80);
		NopInterceptor nop1 = new NopInterceptor();
		NopInterceptor nop2 = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(nop1);
		pf.addAdvice(ba);
		pf.addAdvice(nop2);
		ITestBean proxied = (ITestBean) createProxy(pf);
		// Won't throw an exception
		assertEquals(target.getAge(), proxied.getAge());
		assertEquals(1, ba.getCalls());
		assertEquals(1, ba.getCalls("getAge"));
		assertEquals(1, nop1.getCount());
		assertEquals(1, nop2.getCount());
		// Will fail, after invoking Nop1
		try {
			proxied.setAge(26);
			fail("before advice should have ended chain");
		}
		catch (RuntimeException ex) {
			assertEquals(rex, ex);
		}
		assertEquals(2, ba.getCalls());
		assertEquals(2, nop1.getCount());
		// Nop2 didn't get invoked when the exception was thrown
		assertEquals(1, nop2.getCount());
		// Shouldn't have changed value in joinpoint
		assertEquals(target.getAge(), proxied.getAge());
	}


	@Test
	public void testAfterReturningAdvisorIsInvoked() {
		class SummingAfterAdvice implements AfterReturningAdvice {
			public int sum;
			@Override
			public void afterReturning(@Nullable Object returnValue, Method m, Object[] args, @Nullable Object target) throws Throwable {
				sum += ((Integer) returnValue).intValue();
			}
		}
		SummingAfterAdvice aa = new SummingAfterAdvice();
		@SuppressWarnings("serial")
		Advisor matchesInt = new StaticMethodMatcherPointcutAdvisor(aa) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getReturnType() == int.class;
			}
		};
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvisor(matchesInt);
		assertEquals("Advisor was added", matchesInt, pf.getAdvisors()[1]);
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertEquals(0, aa.sum);
		int i1 = 12;
		int i2 = 13;

		// Won't be advised
		proxied.setAge(i1);
		assertEquals(i1, proxied.getAge());
		assertEquals(i1, aa.sum);
		proxied.setAge(i2);
		assertEquals(i2, proxied.getAge());
		assertEquals(i1 + i2, aa.sum);
		assertEquals(i2, proxied.getAge());
	}

	@Test
	public void testAfterReturningAdvisorIsNotInvokedOnException() {
		CountingAfterReturningAdvice car = new CountingAfterReturningAdvice();
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvice(car);
		assertEquals("Advice was wrapped in Advisor and added", car, pf.getAdvisors()[1].getAdvice());
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertEquals(0, car.getCalls());
		int age = 10;
		proxied.setAge(age);
		assertEquals(age, proxied.getAge());
		assertEquals(2, car.getCalls());
		Exception exc = new Exception();
		// On exception it won't be invoked
		try {
			proxied.exceptional(exc);
			fail();
		}
		catch (Throwable t) {
			assertSame(exc, t);
		}
		assertEquals(2, car.getCalls());
	}


	@Test
	public void testThrowsAdvisorIsInvoked() throws Throwable {
		// Reacts to ServletException and RemoteException
		MyThrowsHandler th = new MyThrowsHandler();
		@SuppressWarnings("serial")
		Advisor matchesEchoInvocations = new StaticMethodMatcherPointcutAdvisor(th) {
			@Override
			public boolean matches(Method m, @Nullable Class<?> targetClass) {
				return m.getName().startsWith("echo");
			}
		};

		Echo target = new Echo();
		target.setA(16);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvisor(matchesEchoInvocations);
		assertEquals("Advisor was added", matchesEchoInvocations, pf.getAdvisors()[1]);
		IEcho proxied = (IEcho) createProxy(pf);
		assertEquals(0, th.getCalls());
		assertEquals(target.getA(), proxied.getA());
		assertEquals(0, th.getCalls());
		Exception ex = new Exception();
		// Will be advised but doesn't match
		try {
			proxied.echoException(1, ex);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}

		ex = new FileNotFoundException();
		try {
			proxied.echoException(1, ex);
			fail();
		}
		catch (FileNotFoundException caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls("ioException"));
	}

	@Test
	public void testAddThrowsAdviceWithoutAdvisor() throws Throwable {
		// Reacts to ServletException and RemoteException
		MyThrowsHandler th = new MyThrowsHandler();

		Echo target = new Echo();
		target.setA(16);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvice(th);
		IEcho proxied = (IEcho) createProxy(pf);
		assertEquals(0, th.getCalls());
		assertEquals(target.getA(), proxied.getA());
		assertEquals(0, th.getCalls());
		Exception ex = new Exception();
		// Will be advised but doesn't match
		try {
			proxied.echoException(1, ex);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}

		// Subclass of RemoteException
		ex = new MarshalException("");
		try {
			proxied.echoException(1, ex);
			fail();
		}
		catch (MarshalException caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls("remoteException"));
	}

	private static class CheckMethodInvocationIsSameInAndOutInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			Method m = mi.getMethod();
			Object retval = mi.proceed();
			assertEquals("Method invocation has same method on way back", m, mi.getMethod());
			return retval;
		}
	}


	/**
	 * ExposeInvocation must be set to true.
	 */
	private static class CheckMethodInvocationViaThreadLocalIsSameInAndOutInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			String task = "get invocation on way IN";
			try {
				MethodInvocation current = ExposeInvocationInterceptor.currentInvocation();
				assertEquals(mi.getMethod(), current.getMethod());
				Object retval = mi.proceed();
				task = "get invocation on way OUT";
				assertEquals(current, ExposeInvocationInterceptor.currentInvocation());
				return retval;
			}
			catch (IllegalStateException ex) {
				System.err.println(task + " for " + mi.getMethod());
				ex.printStackTrace();
				throw ex;
			}
		}
	}


	/**
	 * Same thing for a proxy.
	 * Only works when exposeProxy is set to true.
	 * Checks that the proxy is the same on the way in and out.
	 */
	private static class ProxyMatcherInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			Object proxy = AopContext.currentProxy();
			Object ret = mi.proceed();
			assertEquals(proxy, AopContext.currentProxy());
			return ret;
		}
	}


	/**
	 * Fires on setter methods that take a string. Replaces null arg with "".
	 */
	@SuppressWarnings("serial")
	protected static class StringSetterNullReplacementAdvice extends DefaultPointcutAdvisor {

		private static MethodInterceptor cleaner = new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation mi) throws Throwable {
				// We know it can only be invoked if there's a single parameter of type string
				mi.getArguments()[0] = "";
				return mi.proceed();
			}
		};

		public StringSetterNullReplacementAdvice() {
			super(cleaner);
			setPointcut(new DynamicMethodMatcherPointcut() {
				@Override
				public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
					return args[0] == null;
				}
				@Override
				public boolean matches(Method m, @Nullable Class<?> targetClass) {
					return m.getName().startsWith("set") &&
						m.getParameterCount() == 1 &&
						m.getParameterTypes()[0].equals(String.class);
				}
			});
		}
	}


	@SuppressWarnings("serial")
	protected static class TestDynamicPointcutAdvice extends DefaultPointcutAdvisor {

		public int count;

		public TestDynamicPointcutAdvice(MethodInterceptor mi, final String pattern) {
			super(mi);
			setPointcut(new DynamicMethodMatcherPointcut() {
				@Override
				public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
					boolean run = m.getName().contains(pattern);
					if (run) ++count;
					return run;
				}
			});
		}
	}


	@SuppressWarnings("serial")
	protected static class TestDynamicPointcutForSettersOnly extends DefaultPointcutAdvisor {

		public int count;

		public TestDynamicPointcutForSettersOnly(MethodInterceptor mi, final String pattern) {
			super(mi);
			setPointcut(new DynamicMethodMatcherPointcut() {
				@Override
				public boolean matches(Method m, @Nullable Class<?> targetClass, Object... args) {
					boolean run = m.getName().contains(pattern);
					if (run) ++count;
					return run;
				}
				@Override
				public boolean matches(Method m, @Nullable Class<?> clazz) {
					return m.getName().startsWith("set");
				}
			});
		}
	}


	@SuppressWarnings("serial")
	protected static class TestStaticPointcutAdvice extends StaticMethodMatcherPointcutAdvisor {

		private String pattern;

		public TestStaticPointcutAdvice(MethodInterceptor mi, String pattern) {
			super(mi);
			this.pattern = pattern;
		}
		@Override
		public boolean matches(Method m, @Nullable Class<?> targetClass) {
			return m.getName().contains(pattern);
		}
	}


	/**
	 * Note that trapping the Invocation as in previous version of this test
	 * isn't safe, as invocations may be reused
	 * and hence cleared at the end of each invocation.
	 * So we trap only the target.
	 */
	protected static class TrapTargetInterceptor implements MethodInterceptor {

		public Object target;

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			this.target = invocation.getThis();
			return invocation.proceed();
		}
	}


	private static class DummyIntroductionAdviceImpl implements DynamicIntroductionAdvice {

		@Override
		public boolean implementsInterface(Class<?> intf) {
			return true;
		}
	}


	public static class OwnSpouse extends TestBean {

		@Override
		public ITestBean getSpouse() {
			return this;
		}
	}


	public static class AllInstancesAreEqual implements IOther {

		@Override
		public boolean equals(Object other) {
			return (other instanceof AllInstancesAreEqual);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public void absquatulate() {
		}
	}


	public interface INeedsToSeeProxy {

		int getCount();

		void incrementViaThis();

		void incrementViaProxy();

		void increment();
	}


	public static class NeedsToSeeProxy implements INeedsToSeeProxy {

		private int count;

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public void incrementViaThis() {
			this.increment();
		}

		@Override
		public void incrementViaProxy() {
			INeedsToSeeProxy thisViaProxy = (INeedsToSeeProxy) AopContext.currentProxy();
			thisViaProxy.increment();
			Advised advised = (Advised) thisViaProxy;
			checkAdvised(advised);
		}

		protected void checkAdvised(Advised advised) {
		}

		@Override
		public void increment() {
			++count;
		}
	}


	public static class TargetChecker extends NeedsToSeeProxy {

		@Override
		protected void checkAdvised(Advised advised) {
			// TODO replace this check: no longer possible
			//assertEquals(advised.getTarget(), this);
		}
	}


	public static class CountingAdvisorListener implements AdvisedSupportListener {

		public int adviceChanges;
		public int activates;
		private AdvisedSupport expectedSource;

		public CountingAdvisorListener(AdvisedSupport expectedSource) {
			this.expectedSource = expectedSource;
		}

		@Override
		public void activated(AdvisedSupport advised) {
			assertEquals(expectedSource, advised);
			++activates;
		}

		@Override
		public void adviceChanged(AdvisedSupport advised) {
			assertEquals(expectedSource, advised);
			++adviceChanges;
		}
	}


	public static class RefreshCountingAdvisorChainFactory implements AdvisedSupportListener {

		public int refreshes;

		@Override
		public void activated(AdvisedSupport advised) {
			++refreshes;
		}

		@Override
		public void adviceChanged(AdvisedSupport advised) {
			++refreshes;
		}
	}


	public static interface IOverloads {

		void overload();

		int overload(int i);

		String overload(String foo);

		void noAdvice();
	}


	public static class Overloads implements IOverloads {

		@Override
		public void overload() {
		}

		@Override
		public int overload(int i) {
			return i;
		}

		@Override
		public String overload(String s) {
			return s;
		}

		@Override
		public void noAdvice() {
		}
	}


	@SuppressWarnings("serial")
	public static class CountingMultiAdvice extends MethodCounter implements MethodBeforeAdvice,
			AfterReturningAdvice, ThrowsAdvice {

		@Override
		public void before(Method m, Object[] args, @Nullable Object target) throws Throwable {
			count(m);
		}

		@Override
		public void afterReturning(@Nullable Object o, Method m, Object[] args, @Nullable Object target)
				throws Throwable {
			count(m);
		}

		public void afterThrowing(IOException ex) throws Throwable {
			count(IOException.class.getName());
		}

		public void afterThrowing(UncheckedException ex) throws Throwable {
			count(UncheckedException.class.getName());
		}

	}


	@SuppressWarnings("serial")
	public static class CountingThrowsAdvice extends MethodCounter implements ThrowsAdvice {

		public void afterThrowing(IOException ex) throws Throwable {
			count(IOException.class.getName());
		}

		public void afterThrowing(UncheckedException ex) throws Throwable {
			count(UncheckedException.class.getName());
		}

	}


	@SuppressWarnings("serial")
	static class UncheckedException extends RuntimeException {

	}


	@SuppressWarnings("serial")
	static class SpecializedUncheckedException extends UncheckedException {

		public SpecializedUncheckedException(String string, SQLException exception) {
		}

	}


	static class MockTargetSource implements TargetSource {

		private Object target;

		public int gets;

		public int releases;

		public void reset() {
			this.target = null;
			gets = releases = 0;
		}

		public void setTarget(Object target) {
			this.target = target;
		}

		/**
		 * @see org.springframework.aop.TargetSource#getTargetClass()
		 */
		@Override
		public Class<?> getTargetClass() {
			return target.getClass();
		}

		/**
		 * @see org.springframework.aop.TargetSource#getTarget()
		 */
		@Override
		public Object getTarget() throws Exception {
			++gets;
			return target;
		}

		/**
		 * @see org.springframework.aop.TargetSource#releaseTarget(java.lang.Object)
		 */
		@Override
		public void releaseTarget(Object pTarget) throws Exception {
			if (pTarget != this.target)
				throw new RuntimeException("Released wrong target");
			++releases;
		}

		/**
		 * Check that gets and releases match
		 *
		 */
		public void verify() {
			if (gets != releases)
				throw new RuntimeException("Expectation failed: " + gets + " gets and " + releases + " releases");
		}

		/**
		 * @see org.springframework.aop.TargetSource#isStatic()
		 */
		@Override
		public boolean isStatic() {
			return false;
		}

	}


	static abstract class ExposedInvocationTestBean extends TestBean {

		@Override
		public String getName() {
			MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
			assertions(invocation);
			return super.getName();
		}

		@Override
		public void absquatulate() {
			MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
			assertions(invocation);
			super.absquatulate();
		}

		protected abstract void assertions(MethodInvocation invocation);
	}


	static class InvocationCheckExposedInvocationTestBean extends ExposedInvocationTestBean {
		@Override
		protected void assertions(MethodInvocation invocation) {
			assertSame(this, invocation.getThis());
			assertTrue("Invocation should be on ITestBean: " + invocation.getMethod(),
				ITestBean.class.isAssignableFrom(invocation.getMethod().getDeclaringClass()));
		}
	}

}
