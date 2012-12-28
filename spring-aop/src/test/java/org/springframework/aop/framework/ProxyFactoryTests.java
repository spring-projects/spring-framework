/*
 * Copyright 2002-2010 the original author or authors.
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

import javax.accessibility.Accessible;
import javax.swing.*;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import test.aop.CountingBeforeAdvice;
import test.aop.NopInterceptor;
import test.beans.IOther;
import test.beans.ITestBean;
import test.beans.TestBean;
import test.util.TimeStamped;

import org.springframework.aop.Advisor;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * Also tests AdvisedSupport and ProxyCreatorSupport superclasses.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 14.05.2003
 */
public final class ProxyFactoryTests {

	@Test
	public void testIndexOfMethods() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		Advisor advisor = new DefaultPointcutAdvisor(new CountingBeforeAdvice());
		Advised advised = (Advised) pf.getProxy();
		// Can use advised and ProxyFactory interchangeably
		advised.addAdvice(nop);
		pf.addAdvisor(advisor);
		assertEquals(-1, pf.indexOf(new NopInterceptor()));
		assertEquals(0, pf.indexOf(nop));
		assertEquals(1, pf.indexOf(advisor));
		assertEquals(-1, advised.indexOf(new DefaultPointcutAdvisor(null)));
	}

	@Test
	public void testRemoveAdvisorByReference() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(cba);
		pf.addAdvice(nop);
		pf.addAdvisor(advisor);
		ITestBean proxied = (ITestBean) pf.getProxy();
		proxied.setAge(5);
		assertEquals(1, cba.getCalls());
		assertEquals(1, nop.getCount());
		assertTrue(pf.removeAdvisor(advisor));
		assertEquals(5, proxied.getAge());
		assertEquals(1, cba.getCalls());
		assertEquals(2, nop.getCount());
		assertFalse(pf.removeAdvisor(new DefaultPointcutAdvisor(null)));
	}

	@Test
	public void testRemoveAdvisorByIndex() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba = new CountingBeforeAdvice();
		Advisor advisor = new DefaultPointcutAdvisor(cba);
		pf.addAdvice(nop);
		pf.addAdvisor(advisor);
		NopInterceptor nop2 = new NopInterceptor();
		pf.addAdvice(nop2);
		ITestBean proxied = (ITestBean) pf.getProxy();
		proxied.setAge(5);
		assertEquals(1, cba.getCalls());
		assertEquals(1, nop.getCount());
		assertEquals(1, nop2.getCount());
		// Removes counting before advisor
		pf.removeAdvisor(1);
		assertEquals(5, proxied.getAge());
		assertEquals(1, cba.getCalls());
		assertEquals(2, nop.getCount());
		assertEquals(2, nop2.getCount());
		// Removes Nop1
		pf.removeAdvisor(0);
		assertEquals(5, proxied.getAge());
		assertEquals(1, cba.getCalls());
		assertEquals(2, nop.getCount());
		assertEquals(3, nop2.getCount());

		// Check out of bounds
		try {
			pf.removeAdvisor(-1);
		}
		catch (AopConfigException ex) {
			// Ok
		}

		try {
			pf.removeAdvisor(2);
		}
		catch (AopConfigException ex) {
			// Ok
		}

		assertEquals(5, proxied.getAge());
		assertEquals(4, nop2.getCount());
	}

	@Test
	public void testReplaceAdvisor() {
		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		NopInterceptor nop = new NopInterceptor();
		CountingBeforeAdvice cba1 = new CountingBeforeAdvice();
		CountingBeforeAdvice cba2 = new CountingBeforeAdvice();
		Advisor advisor1 = new DefaultPointcutAdvisor(cba1);
		Advisor advisor2 = new DefaultPointcutAdvisor(cba2);
		pf.addAdvisor(advisor1);
		pf.addAdvice(nop);
		ITestBean proxied = (ITestBean) pf.getProxy();
		// Use the type cast feature
		// Replace etc methods on advised should be same as on ProxyFactory
		Advised advised = (Advised) proxied;
		proxied.setAge(5);
		assertEquals(1, cba1.getCalls());
		assertEquals(0, cba2.getCalls());
		assertEquals(1, nop.getCount());
		assertFalse(advised.replaceAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()), advisor2));
		assertTrue(advised.replaceAdvisor(advisor1, advisor2));
		assertEquals(advisor2, pf.getAdvisors()[0]);
		assertEquals(5, proxied.getAge());
		assertEquals(1, cba1.getCalls());
		assertEquals(2, nop.getCount());
		assertEquals(1, cba2.getCalls());
		assertFalse(pf.replaceAdvisor(new DefaultPointcutAdvisor(null), advisor1));
	}

	@Test
	public void testAddRepeatedInterface() {
		TimeStamped tst = new TimeStamped() {
			public long getTimeStamp() {
				throw new UnsupportedOperationException("getTimeStamp");
			}
		};
		ProxyFactory pf = new ProxyFactory(tst);
		// We've already implicitly added this interface.
		// This call should be ignored without error
		pf.addInterface(TimeStamped.class);
		// All cool
		assertThat(pf.getProxy(), instanceOf(TimeStamped.class));
	}

	@Test
	public void testGetsAllInterfaces() throws Exception {
		// Extend to get new interface
		class TestBeanSubclass extends TestBean implements Comparable<Object> {
			public int compareTo(Object arg0) {
				throw new UnsupportedOperationException("compareTo");
			}
		}
		TestBeanSubclass raw = new TestBeanSubclass();
		ProxyFactory factory = new ProxyFactory(raw);
		//System.out.println("Proxied interfaces are " + StringUtils.arrayToDelimitedString(factory.getProxiedInterfaces(), ","));
		assertEquals("Found correct number of interfaces", 3, factory.getProxiedInterfaces().length);
		ITestBean tb = (ITestBean) factory.getProxy();
		assertThat("Picked up secondary interface", tb, instanceOf(IOther.class));

		raw.setAge(25);
		assertTrue(tb.getAge() == raw.getAge());

		long t = 555555L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor(t);

		Class<?>[] oldProxiedInterfaces = factory.getProxiedInterfaces();

		factory.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

		Class<?>[] newProxiedInterfaces = factory.getProxiedInterfaces();
		assertEquals("Advisor proxies one more interface after introduction", oldProxiedInterfaces.length + 1, newProxiedInterfaces.length);

		TimeStamped ts = (TimeStamped) factory.getProxy();
		assertTrue(ts.getTimeStamp() == t);
		// Shouldn't fail;
		 ((IOther) ts).absquatulate();
	}

	@Test
	public void testInterceptorInclusionMethods() {
		class MyInterceptor implements MethodInterceptor {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw new UnsupportedOperationException();
			}
		}

		NopInterceptor di = new NopInterceptor();
		NopInterceptor diUnused = new NopInterceptor();
		ProxyFactory factory = new ProxyFactory(new TestBean());
		factory.addAdvice(0, di);
		assertThat(factory.getProxy(), instanceOf(ITestBean.class));
		assertTrue(factory.adviceIncluded(di));
		assertTrue(!factory.adviceIncluded(diUnused));
		assertTrue(factory.countAdvicesOfType(NopInterceptor.class) == 1);
		assertTrue(factory.countAdvicesOfType(MyInterceptor.class) == 0);

		factory.addAdvice(0, diUnused);
		assertTrue(factory.adviceIncluded(diUnused));
		assertTrue(factory.countAdvicesOfType(NopInterceptor.class) == 2);
	}

	/**
	 * Should see effect immediately on behavior.
	 */
	@Test
	public void testCanAddAndRemoveAspectInterfacesOnSingleton() {
		ProxyFactory config = new ProxyFactory(new TestBean());

		assertFalse("Shouldn't implement TimeStamped before manipulation",
				config.getProxy() instanceof TimeStamped);

		long time = 666L;
		TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor();
		ti.setTime(time);

		// Add to front of interceptor chain
		int oldCount = config.getAdvisors().length;
		config.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

		assertTrue(config.getAdvisors().length == oldCount + 1);

		TimeStamped ts = (TimeStamped) config.getProxy();
		assertTrue(ts.getTimeStamp() == time);

		// Can remove
		config.removeAdvice(ti);

		assertTrue(config.getAdvisors().length == oldCount);

		try {
			// Existing reference will fail
			ts.getTimeStamp();
			fail("Existing object won't implement this interface any more");
		}
		catch (RuntimeException ex) {
		}

		assertFalse("Should no longer implement TimeStamped",
				config.getProxy() instanceof TimeStamped);

		// Now check non-effect of removing interceptor that isn't there
		config.removeAdvice(new DebugInterceptor());

		assertTrue(config.getAdvisors().length == oldCount);

		ITestBean it = (ITestBean) ts;
		DebugInterceptor debugInterceptor = new DebugInterceptor();
		config.addAdvice(0, debugInterceptor);
		it.getSpouse();
		assertEquals(1, debugInterceptor.getCount());
		config.removeAdvice(debugInterceptor);
		it.getSpouse();
		// not invoked again
		assertTrue(debugInterceptor.getCount() == 1);
	}

	@Test
	public void testProxyTargetClassWithInterfaceAsTarget() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetClass(ITestBean.class);
		Object proxy = pf.getProxy();
		assertTrue("Proxy is a JDK proxy", AopUtils.isJdkDynamicProxy(proxy));
		assertTrue(proxy instanceof ITestBean);
		assertEquals(ITestBean.class, AopProxyUtils.ultimateTargetClass(proxy));

		ProxyFactory pf2 = new ProxyFactory(proxy);
		Object proxy2 = pf2.getProxy();
		assertTrue("Proxy is a JDK proxy", AopUtils.isJdkDynamicProxy(proxy2));
		assertTrue(proxy2 instanceof ITestBean);
		assertEquals(ITestBean.class, AopProxyUtils.ultimateTargetClass(proxy2));
	}

	@Test
	public void testProxyTargetClassWithConcreteClassAsTarget() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetClass(TestBean.class);
		Object proxy = pf.getProxy();
		assertTrue("Proxy is a CGLIB proxy", AopUtils.isCglibProxy(proxy));
		assertTrue(proxy instanceof TestBean);
		assertEquals(TestBean.class, AopProxyUtils.ultimateTargetClass(proxy));

		ProxyFactory pf2 = new ProxyFactory(proxy);
		pf2.setProxyTargetClass(true);
		Object proxy2 = pf2.getProxy();
		assertTrue("Proxy is a CGLIB proxy", AopUtils.isCglibProxy(proxy2));
		assertTrue(proxy2 instanceof TestBean);
		assertEquals(TestBean.class, AopProxyUtils.ultimateTargetClass(proxy2));
	}

	@Test
	@Ignore("Not implemented yet, see http://jira.springframework.org/browse/SPR-5708")
	public void testExclusionOfNonPublicInterfaces() {
		JFrame frame = new JFrame();
		ProxyFactory proxyFactory = new ProxyFactory(frame);
		Object proxy = proxyFactory.getProxy();
		assertTrue(proxy instanceof RootPaneContainer);
		assertTrue(proxy instanceof Accessible);
	}


	@SuppressWarnings("serial")
	private static class TimestampIntroductionInterceptor extends DelegatingIntroductionInterceptor
			implements TimeStamped {

		private long ts;

		public TimestampIntroductionInterceptor() {
		}

		public TimestampIntroductionInterceptor(long ts) {
			this.ts = ts;
		}

		public void setTime(long ts) {
			this.ts = ts;
		}

		public long getTimeStamp() {
			return ts;
		}
	}

}
