/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.MarshalException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.aop.testfixture.advice.CountingAfterReturningAdvice;
import org.springframework.aop.testfixture.advice.CountingBeforeAdvice;
import org.springframework.aop.testfixture.advice.MethodCounter;
import org.springframework.aop.testfixture.advice.MyThrowsHandler;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.interceptor.SerializableNopInterceptor;
import org.springframework.aop.testfixture.interceptor.TimestampIntroductionInterceptor;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.TimeStamped;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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
	@BeforeEach
	public void setUp() {
		mockTargetSource.reset();
	}

	@AfterEach
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


	@Test
	public void testNoInterceptorsAndNoTarget() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() -> {
				AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
				//Add no interceptors
				AopProxy aop = createAopProxy(pc);
				aop.getProxy();
		});
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

		assertThat(tb.getAge()).isEqualTo(age1);
		tb.setAge(age2);
		assertThat(tb.getAge()).isEqualTo(age2);
		assertThat(tb.getName()).isNull();
		tb.setName(name);
		assertThat(tb.getName()).isEqualTo(name);
	}

	@Test
	public void testSerializationAdviceAndTargetNotSerializable() throws Exception {
		TestBean tb = new TestBean();
		assertThat(SerializationTestUtils.isSerializable(tb)).isFalse();

		ProxyFactory pf = new ProxyFactory(tb);

		pf.addAdvice(new NopInterceptor());
		ITestBean proxy = (ITestBean) createAopProxy(pf).getProxy();

		assertThat(SerializationTestUtils.isSerializable(proxy)).isFalse();
	}

	@Test
	public void testSerializationAdviceNotSerializable() throws Exception {
		SerializablePerson sp = new SerializablePerson();
		assertThat(SerializationTestUtils.isSerializable(sp)).isTrue();

		ProxyFactory pf = new ProxyFactory(sp);

		// This isn't serializable
		Advice i = new NopInterceptor();
		pf.addAdvice(i);
		assertThat(SerializationTestUtils.isSerializable(i)).isFalse();
		Object proxy = createAopProxy(pf).getProxy();

		assertThat(SerializationTestUtils.isSerializable(proxy)).isFalse();
	}

	@Test
	public void testSerializableTargetAndAdvice() throws Throwable {
		SerializablePerson personTarget = new SerializablePerson();
		personTarget.setName("jim");
		personTarget.setAge(26);

		assertThat(SerializationTestUtils.isSerializable(personTarget)).isTrue();

		ProxyFactory pf = new ProxyFactory(personTarget);

		CountingThrowsAdvice cta = new CountingThrowsAdvice();

		pf.addAdvice(new SerializableNopInterceptor());
		// Try various advice types
		pf.addAdvice(new CountingBeforeAdvice());
		pf.addAdvice(new CountingAfterReturningAdvice());
		pf.addAdvice(cta);
		Person p = (Person) createAopProxy(pf).getProxy();

		p.echo(null);
		assertThat(cta.getCalls()).isEqualTo(0);
		try {
			p.echo(new IOException());
		}
		catch (IOException ex) {
			/* expected */
		}
		assertThat(cta.getCalls()).isEqualTo(1);

		// Will throw exception if it fails
		Person p2 = SerializationTestUtils.serializeAndDeserialize(p);
		assertThat(p2).isNotSameAs(p);
		assertThat(p2.getName()).isEqualTo(p.getName());
		assertThat(p2.getAge()).isEqualTo(p.getAge());
		assertThat(AopUtils.isAopProxy(p2)).as("Deserialized object is an AOP proxy").isTrue();

		Advised a1 = (Advised) p;
		Advised a2 = (Advised) p2;
		// Check we can manipulate state of p2
		assertThat(a2.getAdvisors().length).isEqualTo(a1.getAdvisors().length);

		// This should work as SerializablePerson is equal
		assertThat(p2).as("Proxies should be equal, even after one was serialized").isEqualTo(p);
		assertThat(p).as("Proxies should be equal, even after one was serialized").isEqualTo(p2);

		// Check we can add a new advisor to the target
		NopInterceptor ni = new NopInterceptor();
		p2.getAge();
		assertThat(ni.getCount()).isEqualTo(0);
		a2.addAdvice(ni);
		p2.getAge();
		assertThat(ni.getCount()).isEqualTo(1);

		cta = (CountingThrowsAdvice) a2.getAdvisors()[3].getAdvice();
		p2.echo(null);
		assertThat(cta.getCalls()).isEqualTo(1);
		try {
			p2.echo(new IOException());
		}
		catch (IOException ex) {

		}
		assertThat(cta.getCalls()).isEqualTo(2);
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

		// = 3 invocations
		assertThat(advised1.getAge()).as("Advised one has correct age").isEqualTo(age1);
		assertThat(advised2.getAge()).as("Advised two has correct age").isEqualTo(age2);
		// Means extra call on advised 2
		// = 4 invocations on 1 and another one on 2
		assertThat(advised1.getSpouse().getAge()).as("Advised one spouse has correct age").isEqualTo(age2);

		assertThat(di1.getCount()).as("one was invoked correct number of times").isEqualTo(4);
		// Got hit by call to advised1.getSpouse().getAge()
		assertThat(di2.getCount()).as("one was invoked correct number of times").isEqualTo(3);
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

		assertThat(di1.getCount()).as("one was invoked correct number of times").isEqualTo(2);

		// = 3 invocations
		assertThat(advised1.getAge()).as("Advised one has correct age").isEqualTo(age1);
		assertThat(di1.getCount()).as("one was invoked correct number of times").isEqualTo(3);

		// = 5 invocations, as reentrant call to spouse is advised also
		assertThat(advised1.getSpouse().getAge()).as("Advised spouse has correct age").isEqualTo(age1);

		assertThat(di1.getCount()).as("one was invoked correct number of times").isEqualTo(5);
	}

	@Test
	public void testTargetCanGetProxy() {
		NopInterceptor di = new NopInterceptor();
		INeedsToSeeProxy target = new TargetChecker();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setExposeProxy(true);
		assertThat(proxyFactory.isExposeProxy()).isTrue();

		proxyFactory.addAdvice(0, di);
		INeedsToSeeProxy proxied = (INeedsToSeeProxy) createProxy(proxyFactory);
		assertThat(di.getCount()).isEqualTo(0);
		assertThat(target.getCount()).isEqualTo(0);
		proxied.incrementViaThis();
		assertThat(target.getCount()).as("Increment happened").isEqualTo(1);

		assertThat(di.getCount()).as("Only one invocation via AOP as use of this wasn't proxied").isEqualTo(1);
		// 1 invocation
		assertThat(proxied.getCount()).as("Increment happened").isEqualTo(1);
		proxied.incrementViaProxy(); // 2 invocations
		assertThat(target.getCount()).as("Increment happened").isEqualTo(2);
		assertThat(di.getCount()).as("3 more invocations via AOP as the first call was reentrant through the proxy").isEqualTo(4);
	}

	@Test
	// Should fail to get proxy as exposeProxy wasn't set to true
	public void testTargetCantGetProxyByDefault() {
		NeedsToSeeProxy et = new NeedsToSeeProxy();
		ProxyFactory pf1 = new ProxyFactory(et);
		assertThat(pf1.isExposeProxy()).isFalse();
		INeedsToSeeProxy proxied = (INeedsToSeeProxy) createProxy(pf1);
		assertThatIllegalStateException().isThrownBy(() ->
				proxied.incrementViaProxy());
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
					assertThat(ExposeInvocationInterceptor.currentInvocation()).as("have context").isNotNull();
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
		assertThat(tb.getName()).as("correct return value").isSameAs(s);
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
		assertThat(tb.getSpouse()).as("this return is wrapped in proxy").isSameAs(tb);
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

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
				ITestBean tb = (ITestBean) aop.getProxy();
				// Note: exception param below isn't used
				tb.exceptional(expectedException);
			}).matches(expectedException::equals);
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

		assertThatExceptionOfType(UndeclaredThrowableException.class).isThrownBy(
				tb::getAge)
			.satisfies(ex -> assertThat(ex.getUndeclaredThrowable()).isEqualTo(unexpectedException));
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

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				tb::getAge)
			.matches(unexpectedException::equals);
	}

	/**
	 * Check that although a method is eligible for advice chain optimization and
	 * direct reflective invocation, it doesn't happen if we've asked to see the proxy,
	 * so as to guarantee a consistent programming model.
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
				assertThat(invocation.getThis()).as("Target is correct").isEqualTo(expectedTarget);
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
		assertThatIllegalStateException().isThrownBy(
				ExposeInvocationInterceptor::currentInvocation);
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
		assertThat(itb.getAge()).isEqualTo(newAge);

		Lockable lockable = (Lockable) itb;
		assertThat(lockable.locked()).isFalse();
		lockable.lock();

		assertThat(itb.getAge()).isEqualTo(newAge);
		assertThatExceptionOfType(LockedException.class).isThrownBy(() ->
				itb.setAge(1));
		assertThat(itb.getAge()).isEqualTo(newAge);

		// Unlock
		assertThat(lockable.locked()).isTrue();
		lockable.unlock();
		itb.setAge(1);
		assertThat(itb.getAge()).isEqualTo(1);
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
		assertThat(t.getAge()).isEqualTo(newAge);
		String newName = "greg";
		t.setName(newName);
		assertThat(t.getName()).isEqualTo(newName);

		t.setName(null);
		// Null replacement magic should work
		assertThat(t.getName()).isEqualTo("");
	}

	@Test
	public void testCanCastProxyToProxyConfig() throws Throwable {
		TestBean tb = new TestBean();
		ProxyFactory pc = new ProxyFactory(tb);
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(0, di);

		ITestBean t = (ITestBean) createProxy(pc);
		assertThat(di.getCount()).isEqualTo(0);
		t.setAge(23);
		assertThat(t.getAge()).isEqualTo(23);
		assertThat(di.getCount()).isEqualTo(2);

		Advised advised = (Advised) t;
		assertThat(advised.getAdvisors().length).as("Have 1 advisor").isEqualTo(1);
		assertThat(advised.getAdvisors()[0].getAdvice()).isEqualTo(di);
		NopInterceptor di2 = new NopInterceptor();
		advised.addAdvice(1, di2);
		t.getName();
		assertThat(di.getCount()).isEqualTo(3);
		assertThat(di2.getCount()).isEqualTo(1);
		// will remove di
		advised.removeAdvisor(0);
		t.getAge();
		// Unchanged
		assertThat(di.getCount()).isEqualTo(3);
		assertThat(di2.getCount()).isEqualTo(2);

		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		assertThat(cba.getCalls()).isEqualTo(0);
		advised.addAdvice(cba);
		t.setAge(16);
		assertThat(t.getAge()).isEqualTo(16);
		assertThat(cba.getCalls()).isEqualTo(2);
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
		assertThat(proxied.getName()).isEqualTo(name);
		TimeStamped intro = (TimeStamped) proxied;
		assertThat(intro.getTimeStamp()).isEqualTo(ts);
	}

	@Test
	public void testCannotAddDynamicIntroductionAdviceExceptInIntroductionAdvice() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				pc.addAdvice(new DummyIntroductionAdviceImpl()))
			.withMessageContaining("ntroduction");
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
	}

	@Test
	public void testRejectsBogusDynamicIntroductionAdviceWithNoAdapter() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		pc.addAdvisor(new DefaultIntroductionAdvisor(new DummyIntroductionAdviceImpl(), Comparable.class));
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
			// TODO May fail on either call: may want to tighten up definition
			ITestBean proxied = (ITestBean) createProxy(pc);
			proxied.getName();
		});
		// TODO used to catch UnknownAdviceTypeException, but
		// with CGLIB some errors are in proxy creation and are wrapped
		// in aspect exception. Error message is still fine.
		//assertTrue(ex.getMessage().indexOf("ntroduction") > -1);
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
		assertThatIllegalArgumentException().isThrownBy(() ->
				pc.addAdvisor(0, new DefaultIntroductionAdvisor(new TimestampIntroductionInterceptor(), ITestBean.class)));
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
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
			 * @see org.springframework.core.testfixture.TimeStamped#getTimeStamp()
			 */
			@Override
			public long getTimeStamp() {
				throw new UnsupportedOperationException();
			}
		}
		pc.addAdvisor(new DefaultIntroductionAdvisor(new MyDi()));

		TimeStamped ts = (TimeStamped) createProxy(pc);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				ts::getTimeStamp);
	}

	/**
	 * Should only be able to introduce interfaces, not classes.
	 */
	@Test
	public void testCannotAddIntroductionAdviceToIntroduceClass() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertThatIllegalArgumentException().as("Shouldn't be able to add introduction advice that introduces a class, rather than an interface").isThrownBy(() ->
				pc.addAdvisor(0, new DefaultIntroductionAdvisor(new TimestampIntroductionInterceptor(), TestBean.class)))
			.withMessageContaining("interface");
		// Check it still works: proxy factory state shouldn't have been corrupted
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
	}

	@Test
	public void testCannotAddInterceptorWhenFrozen() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertThat(pc.isFrozen()).isFalse();
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		assertThatExceptionOfType(AopConfigException.class).as("Shouldn't be able to add interceptor when frozen").isThrownBy(() ->
				pc.addAdvice(0, new NopInterceptor()))
			.withMessageContaining("frozen");
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(((Advised) proxied).getAdvisors().length).isEqualTo(1);
	}

	/**
	 * Check that casting to Advised can't get around advice freeze.
	 */
	@Test
	public void testCannotAddAdvisorWhenFrozenUsingCast() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertThat(pc.isFrozen()).isFalse();
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		Advised advised = (Advised) proxied;

		assertThat(pc.isFrozen()).isTrue();
		assertThatExceptionOfType(AopConfigException.class).as("Shouldn't be able to add Advisor when frozen").isThrownBy(() ->
				advised.addAdvisor(new DefaultPointcutAdvisor(new NopInterceptor())))
			.withMessageContaining("frozen");
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(advised.getAdvisors().length).isEqualTo(1);
	}

	@Test
	public void testCannotRemoveAdvisorWhenFrozen() throws Throwable {
		TestBean target = new TestBean();
		target.setAge(21);
		ProxyFactory pc = new ProxyFactory(target);
		assertThat(pc.isFrozen()).isFalse();
		pc.addAdvice(new NopInterceptor());
		ITestBean proxied = (ITestBean) createProxy(pc);
		pc.setFrozen(true);
		Advised advised = (Advised) proxied;

		assertThat(pc.isFrozen()).isTrue();
		assertThatExceptionOfType(AopConfigException.class).as("Shouldn't be able to remove Advisor when frozen").isThrownBy(() ->
				advised.removeAdvisor(0))
			.withMessageContaining("frozen");
		// Didn't get removed
		assertThat(advised.getAdvisors().length).isEqualTo(1);
		pc.setFrozen(false);
		// Can now remove it
		advised.removeAdvisor(0);
		// Check it still works: proxy factory state shouldn't have been corrupted
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(advised.getAdvisors().length).isEqualTo(0);
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
		assertThat(h.get(proxy1)).isNull();
		h.put(proxy1, value1);
		h.put(proxy2, value2);
		assertThat(value1).isEqualTo(h.get(proxy1));
		assertThat(value2).isEqualTo(h.get(proxy2));
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
		assertThat(proxyConfigString.contains(advisor.toString())).isTrue();
		assertThat(proxyConfigString.contains("1 interface")).isTrue();
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
		assertThat(pc.isOpaque()).as("Opaque defaults to false").isFalse();
		pc.setOpaque(true);
		assertThat(pc.isOpaque()).as("Opaque now true for this config").isTrue();
		ITestBean proxied = (ITestBean) createProxy(pc);
		proxied.setAge(10);
		assertThat(proxied.getAge()).isEqualTo(10);
		assertThat(mba.getCalls()).isEqualTo(1);

		boolean condition = proxied instanceof Advised;
		assertThat(condition).as("Cannot be cast to Advised").isFalse();
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
		assertThat(pc.isActive()).isFalse();
		assertThat(l.activates).isEqualTo(0);
		assertThat(acf.refreshes).isEqualTo(0);
		ITestBean proxied = (ITestBean) createProxy(pc);
		assertThat(acf.refreshes).isEqualTo(1);
		assertThat(l.activates).isEqualTo(1);
		assertThat(pc.isActive()).isTrue();
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(l.adviceChanges).isEqualTo(0);
		NopInterceptor di = new NopInterceptor();
		pc.addAdvice(0, di);
		assertThat(l.adviceChanges).isEqualTo(1);
		assertThat(acf.refreshes).isEqualTo(2);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		pc.removeAdvice(di);
		assertThat(l.adviceChanges).isEqualTo(2);
		assertThat(acf.refreshes).isEqualTo(3);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		pc.getProxy();
		assertThat(l.activates).isEqualTo(1);

		pc.removeListener(l);
		assertThat(l.adviceChanges).isEqualTo(2);
		pc.addAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()));
		// No longer counting
		assertThat(l.adviceChanges).isEqualTo(2);
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
		assertThat(0).isEqualTo(nop.getCount());
		assertThat(proxy.getAge()).isEqualTo(tb1.getAge());
		assertThat(1).isEqualTo(nop.getCount());
		// Change to a new static target
		pc.setTarget(tb2);
		assertThat(proxy.getAge()).isEqualTo(tb2.getAge());
		assertThat(2).isEqualTo(nop.getCount());

		// Change to a new dynamic target
		HotSwappableTargetSource hts = new HotSwappableTargetSource(tb3);
		pc.setTargetSource(hts);
		assertThat(proxy.getAge()).isEqualTo(tb3.getAge());
		assertThat(3).isEqualTo(nop.getCount());
		hts.swap(tb1);
		assertThat(proxy.getAge()).isEqualTo(tb1.getAge());
		tb1.setName("Colin");
		assertThat(proxy.getName()).isEqualTo(tb1.getName());
		assertThat(5).isEqualTo(nop.getCount());

		// Change back, relying on casting to Advised
		Advised advised = (Advised) proxy;
		assertThat(advised.getTargetSource()).isSameAs(hts);
		SingletonTargetSource sts = new SingletonTargetSource(tb2);
		advised.setTargetSource(sts);
		assertThat(proxy.getName()).isEqualTo(tb2.getName());
		assertThat(advised.getTargetSource()).isSameAs(sts);
		assertThat(proxy.getAge()).isEqualTo(tb2.getAge());
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
		assertThat(dp.count).isEqualTo(0);
		it.getAge();
		assertThat(dp.count).isEqualTo(1);
		it.setAge(11);
		assertThat(it.getAge()).isEqualTo(11);
		assertThat(dp.count).isEqualTo(2);
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
		assertThat(dp.count).isEqualTo(0);
		it.getAge();
		// Statically vetoed
		assertThat(dp.count).isEqualTo(0);
		it.setAge(11);
		assertThat(it.getAge()).isEqualTo(11);
		assertThat(dp.count).isEqualTo(1);
		// Applies statically but not dynamically
		it.setName("joe");
		assertThat(dp.count).isEqualTo(1);
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
		assertThat(0).isEqualTo(di.getCount());
		it.getAge();
		assertThat(1).isEqualTo(di.getCount());
		it.setAge(11);
		assertThat(11).isEqualTo(it.getAge());
		assertThat(2).isEqualTo(di.getCount());
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
		assertThat(it.getAge()).isEqualTo(age);
		// Should return the age before the third, AOP-induced birthday
		assertThat(it.haveBirthday()).isEqualTo((age + 2));
		// Return the final age produced by 3 birthdays
		assertThat(it.getAge()).isEqualTo((age + 3));
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
			private List<Object> names = new ArrayList<>();

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
		assertThat(tb.getName()).isEqualTo(name1);

		it.setName(name2);
		// NameReverter saved it back
		assertThat(it.getName()).isEqualTo(name1);
		assertThat(saver.names.size()).isEqualTo(2);
		assertThat(saver.names.get(0)).isEqualTo(name2);
		assertThat(saver.names.get(1)).isEqualTo(name1);
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
		assertThat(overLoadInts.getCount()).isEqualTo(0);
		assertThat(overLoadVoids.getCount()).isEqualTo(0);
		proxy.overload();
		assertThat(overLoadInts.getCount()).isEqualTo(0);
		assertThat(overLoadVoids.getCount()).isEqualTo(1);
		assertThat(proxy.overload(25)).isEqualTo(25);
		assertThat(overLoadInts.getCount()).isEqualTo(1);
		assertThat(overLoadVoids.getCount()).isEqualTo(1);
		proxy.noAdvice();
		assertThat(overLoadInts.getCount()).isEqualTo(1);
		assertThat(overLoadVoids.getCount()).isEqualTo(1);
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
				assertThat(AopContext.currentProxy()).isEqualTo(proxy);
				return target;
			}
			@Override
			public void releaseTarget(Object target) throws Exception {
			}
		});

		// Just test anything: it will fail if context wasn't found
		assertThat(proxy.getAge()).isEqualTo(0);
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

		assertThat(pfb.getAdvisors().length).isEqualTo(pfa.getAdvisors().length);
		assertThat(b).isEqualTo(a);
		assertThat(i2).isEqualTo(i1);
		assertThat(proxyB).isEqualTo(proxyA);
		assertThat(proxyB.hashCode()).isEqualTo(proxyA.hashCode());
		assertThat(proxyA.equals(a)).isFalse();

		// Equality checks were handled by the proxy
		assertThat(i1.getCount()).isEqualTo(0);

		// When we invoke A, it's NopInterceptor will have count == 1
		// and won't think it's equal to B's NopInterceptor
		proxyA.absquatulate();
		assertThat(i1.getCount()).isEqualTo(1);
		assertThat(proxyA.equals(proxyB)).isFalse();
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
		assertThat(pf.getAdvisors()[1]).as("Advisor was added").isEqualTo(matchesNoArgs);
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertThat(cba.getCalls()).isEqualTo(0);
		assertThat(cba.getCalls("getAge")).isEqualTo(0);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(cba.getCalls("getAge")).isEqualTo(1);
		assertThat(cba.getCalls("setAge")).isEqualTo(0);
		// Won't be advised
		proxied.setAge(26);
		assertThat(cba.getCalls()).isEqualTo(1);
		assertThat(proxied.getAge()).isEqualTo(26);
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
					assertThat(rmi.getUserAttributes().get(key)).isEqualTo(expectedValues.get(key));
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
		assertThat(tb.getName()).isEqualTo(newName);
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
		assertThat(pf.getAdvisors()[1]).as("Advisor was added").isEqualTo(matchesNoArgs);
		ITestBean proxied = (ITestBean) createProxy(pf);

		assertThat(cca.getCalls()).isEqualTo(0);
		assertThat(cca.getCalls("getAge")).isEqualTo(0);
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(cca.getCalls()).isEqualTo(2);
		assertThat(cca.getCalls("getAge")).isEqualTo(2);
		assertThat(cca.getCalls("setAge")).isEqualTo(0);
		// Won't be advised
		proxied.setAge(26);
		assertThat(cca.getCalls()).isEqualTo(2);
		assertThat(proxied.getAge()).isEqualTo(26);
		assertThat(cca.getCalls()).isEqualTo(4);
		assertThatExceptionOfType(SpecializedUncheckedException.class).as("Should have thrown CannotGetJdbcConnectionException").isThrownBy(() ->
				proxied.exceptional(new SpecializedUncheckedException("foo", (SQLException)null)));
		assertThat(cca.getCalls()).isEqualTo(6);
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
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(ba.getCalls()).isEqualTo(1);
		assertThat(ba.getCalls("getAge")).isEqualTo(1);
		assertThat(nop1.getCount()).isEqualTo(1);
		assertThat(nop2.getCount()).isEqualTo(1);
		// Will fail, after invoking Nop1
		assertThatExceptionOfType(RuntimeException.class).as("before advice should have ended chain").isThrownBy(() ->
				proxied.setAge(26))
			.matches(rex::equals);
		assertThat(ba.getCalls()).isEqualTo(2);
		assertThat(nop1.getCount()).isEqualTo(2);
		// Nop2 didn't get invoked when the exception was thrown
		assertThat(nop2.getCount()).isEqualTo(1);
		// Shouldn't have changed value in joinpoint
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
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
		assertThat(pf.getAdvisors()[1]).as("Advisor was added").isEqualTo(matchesInt);
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertThat(aa.sum).isEqualTo(0);
		int i1 = 12;
		int i2 = 13;

		// Won't be advised
		proxied.setAge(i1);
		assertThat(proxied.getAge()).isEqualTo(i1);
		assertThat(aa.sum).isEqualTo(i1);
		proxied.setAge(i2);
		assertThat(proxied.getAge()).isEqualTo(i2);
		assertThat(aa.sum).isEqualTo((i1 + i2));
		assertThat(proxied.getAge()).isEqualTo(i2);
	}

	@Test
	public void testAfterReturningAdvisorIsNotInvokedOnException() {
		CountingAfterReturningAdvice car = new CountingAfterReturningAdvice();
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(new NopInterceptor());
		pf.addAdvice(car);
		assertThat(pf.getAdvisors()[1].getAdvice()).as("Advice was wrapped in Advisor and added").isEqualTo(car);
		ITestBean proxied = (ITestBean) createProxy(pf);
		assertThat(car.getCalls()).isEqualTo(0);
		int age = 10;
		proxied.setAge(age);
		assertThat(proxied.getAge()).isEqualTo(age);
		assertThat(car.getCalls()).isEqualTo(2);
		Exception exc = new Exception();
		// On exception it won't be invoked
		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				proxied.exceptional(exc))
			.satisfies(ex -> assertThat(ex).isSameAs(exc));
		assertThat(car.getCalls()).isEqualTo(2);
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
		assertThat(pf.getAdvisors()[1]).as("Advisor was added").isEqualTo(matchesEchoInvocations);
		IEcho proxied = (IEcho) createProxy(pf);
		assertThat(th.getCalls()).isEqualTo(0);
		assertThat(proxied.getA()).isEqualTo(target.getA());
		assertThat(th.getCalls()).isEqualTo(0);
		Exception ex = new Exception();
		// Will be advised but doesn't match
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				proxied.echoException(1, ex))
			.matches(ex::equals);
		FileNotFoundException fex = new FileNotFoundException();
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				proxied.echoException(1, fex))
			.matches(fex::equals);
		assertThat(th.getCalls("ioException")).isEqualTo(1);
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
		assertThat(th.getCalls()).isEqualTo(0);
		assertThat(proxied.getA()).isEqualTo(target.getA());
		assertThat(th.getCalls()).isEqualTo(0);
		Exception ex = new Exception();
		// Will be advised but doesn't match
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				proxied.echoException(1, ex))
			.matches(ex::equals);

		// Subclass of RemoteException
		MarshalException mex = new MarshalException("");
		assertThatExceptionOfType(MarshalException.class).isThrownBy(() ->
				proxied.echoException(1, mex))
			.matches(mex::equals);

		assertThat(th.getCalls("remoteException")).isEqualTo(1);
	}

	private static class CheckMethodInvocationIsSameInAndOutInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			Method m = mi.getMethod();
			Object retval = mi.proceed();
			assertThat(mi.getMethod()).as("Method invocation has same method on way back").isEqualTo(m);
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
				assertThat(current.getMethod()).isEqualTo(mi.getMethod());
				Object retval = mi.proceed();
				task = "get invocation on way OUT";
				assertThat(ExposeInvocationInterceptor.currentInvocation()).isEqualTo(current);
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
			assertThat(AopContext.currentProxy()).isEqualTo(proxy);
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
			assertThat(advised).isEqualTo(expectedSource);
			++activates;
		}

		@Override
		public void adviceChanged(AdvisedSupport advised) {
			assertThat(advised).isEqualTo(expectedSource);
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
			assertThat(invocation.getThis()).isSameAs(this);
			assertThat(ITestBean.class.isAssignableFrom(invocation.getMethod().getDeclaringClass())).as("Invocation should be on ITestBean: " + invocation.getMethod()).isTrue();
		}
	}

}
