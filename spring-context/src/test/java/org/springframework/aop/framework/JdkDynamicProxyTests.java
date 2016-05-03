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

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.junit.Test;

import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.tests.sample.beans.IOther;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13.03.2003
 */
@SuppressWarnings("serial")
public class JdkDynamicProxyTests extends AbstractAopProxyTests implements Serializable {

	@Override
	protected Object createProxy(ProxyCreatorSupport as) {
		assertFalse("Not forcible CGLIB", as.isProxyTargetClass());
		Object proxy = as.createAopProxy().getProxy();
		assertTrue("Should be a JDK proxy: " + proxy.getClass(), AopUtils.isJdkDynamicProxy(proxy));
		return proxy;
	}

	@Override
	protected AopProxy createAopProxy(AdvisedSupport as) {
		return new JdkDynamicAopProxy(as);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testNullConfig() {
		new JdkDynamicAopProxy(null);
	}

	@Test
	public void testProxyIsJustInterface() throws Throwable {
		TestBean raw = new TestBean();
		raw.setAge(32);
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.setTarget(raw);
		JdkDynamicAopProxy aop = new JdkDynamicAopProxy(pc);

		Object proxy = aop.getProxy();
		assertTrue(proxy instanceof ITestBean);
		assertFalse(proxy instanceof TestBean);
	}

	@Test
	public void testInterceptorIsInvokedWithNoTarget() throws Throwable {
		// Test return value
		final int age = 25;
		MethodInterceptor mi = (invocation -> age);

		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.addAdvice(mi);
		AopProxy aop = createAopProxy(pc);

		ITestBean tb = (ITestBean) aop.getProxy();
		assertEquals("correct return value", age, tb.getAge());
	}

	@Test
	public void testTargetCanGetInvocationWithPrivateClass() throws Throwable {
		final ExposedInvocationTestBean expectedTarget = new ExposedInvocationTestBean() {
			@Override
			protected void assertions(MethodInvocation invocation) {
				assertEquals(this, invocation.getThis());
				assertEquals("Invocation should be on ITestBean: " + invocation.getMethod(),
						ITestBean.class, invocation.getMethod().getDeclaringClass());
			}
		};

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

	@Test
	public void testProxyNotWrappedIfIncompatible() {
		FooBar bean = new FooBar();
		ProxyCreatorSupport as = new ProxyCreatorSupport();
		as.setInterfaces(Foo.class);
		as.setTarget(bean);

		Foo proxy = (Foo) createProxy(as);
		assertSame("Target should be returned when return types are incompatible", bean, proxy.getBarThis());
		assertSame("Proxy should be returned when return types are compatible", proxy, proxy.getFooThis());
	}

	@Test
	public void testEqualsAndHashCodeDefined() throws Exception {
		AdvisedSupport as = new AdvisedSupport(Named.class);
		as.setTarget(new Person());
		JdkDynamicAopProxy aopProxy = new JdkDynamicAopProxy(as);
		Named proxy = (Named) aopProxy.getProxy();
		Named named = new Person();
		assertEquals("equals()", proxy, named);
		assertEquals("hashCode()", proxy.hashCode(), named.hashCode());
	}

	@Test  // SPR-13328
	public void testVarargsWithEnumArray() throws Exception {
		ProxyFactory proxyFactory = new ProxyFactory(new VarargTestBean());
		VarargTestInterface proxy = (VarargTestInterface) proxyFactory.getProxy();
		assertTrue(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C));
	}


	public interface Foo {

		Bar getBarThis();

		Foo getFooThis();
	}


	public interface Bar {
	}


	public static class FooBar implements Foo, Bar {

		@Override
		public Bar getBarThis() {
			return this;
		}

		@Override
		public Foo getFooThis() {
			return this;
		}
	}


	public interface Named {

		String getName();

		@Override
		boolean equals(Object other);

		@Override
		int hashCode();
	}


	public static class Person implements Named {

		private final String name = "Rob Harrop";

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Person person = (Person) o;
			if (!name.equals(person.name)) return false;
			return true;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}


	public interface VarargTestInterface {

		<V extends MyInterface> boolean doWithVarargs(V... args);
	}


	public static class VarargTestBean implements VarargTestInterface {

		@SuppressWarnings("unchecked")
		@Override
		public <V extends MyInterface> boolean doWithVarargs(V... args) {
			return true;
		}
	}


	public interface MyInterface {
	}


	public enum MyEnum implements MyInterface {

		A, B;
	}


	public enum MyOtherEnum implements MyInterface {

		C, D;
	}

}
