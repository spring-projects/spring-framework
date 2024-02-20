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

package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13.03.2003
 */
class JdkDynamicProxyTests extends AbstractAopProxyTests {

	@Override
	protected Object createProxy(ProxyCreatorSupport as) {
		assertThat(as.isProxyTargetClass()).as("Not forcible CGLIB").isFalse();
		Object proxy = as.createAopProxy().getProxy();
		assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Should be a JDK proxy: " + proxy.getClass()).isTrue();
		return proxy;
	}

	@Override
	protected AopProxy createAopProxy(AdvisedSupport as) {
		return new JdkDynamicAopProxy(as);
	}


	@Test
	void nullConfig() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JdkDynamicAopProxy(null));
	}

	@Test
	void proxyIsJustInterface() {
		TestBean raw = new TestBean();
		raw.setAge(32);
		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.setTarget(raw);
		JdkDynamicAopProxy aop = new JdkDynamicAopProxy(pc);

		Object proxy = aop.getProxy();
		assertThat(proxy).isInstanceOf(ITestBean.class).isNotInstanceOf(TestBean.class);
	}

	@Test
	void interceptorIsInvokedWithNoTarget() {
		// Test return value
		final int age = 25;
		MethodInterceptor mi = (invocation -> age);

		AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
		pc.addAdvice(mi);
		AopProxy aop = createAopProxy(pc);

		ITestBean tb = (ITestBean) aop.getProxy();
		assertThat(tb.getAge()).as("correct return value").isEqualTo(age);
	}

	@Test
	void targetCanGetInvocationWithPrivateClass() {
		final ExposedInvocationTestBean expectedTarget = new ExposedInvocationTestBean() {
			@Override
			protected void assertions(MethodInvocation invocation) {
				assertThat(invocation.getThis()).isEqualTo(this);
				assertThat(invocation.getMethod().getDeclaringClass())
						.as("Invocation should be on ITestBean: " + invocation.getMethod())
						.isEqualTo(ITestBean.class);
			}
		};

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

	@Test
	void proxyNotWrappedIfIncompatible() {
		FooBar bean = new FooBar();
		ProxyCreatorSupport as = new ProxyCreatorSupport();
		as.setInterfaces(Foo.class);
		as.setTarget(bean);

		Foo proxy = (Foo) createProxy(as);
		assertThat(proxy.getBarThis()).as("Target should be returned when return types are incompatible").isSameAs(bean);
		assertThat(proxy.getFooThis()).as("Proxy should be returned when return types are compatible").isSameAs(proxy);
	}

	@Test
	void equalsAndHashCodeDefined() {
		Named named = new Person();
		AdvisedSupport as = new AdvisedSupport(Named.class);
		as.setTarget(named);

		Named proxy = (Named) new JdkDynamicAopProxy(as).getProxy();
		assertThat(proxy).isEqualTo(named);
		assertThat(named).hasSameHashCodeAs(proxy);

		proxy = (Named) new JdkDynamicAopProxy(as).getProxy();
		assertThat(proxy).isEqualTo(named);
		assertThat(named).hasSameHashCodeAs(proxy);
	}

	@Test  // SPR-13328
	void varargsWithEnumArray() {
		ProxyFactory proxyFactory = new ProxyFactory(new VarargTestBean());
		VarargTestInterface proxy = (VarargTestInterface) proxyFactory.getProxy();
		assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
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
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			if (!name.equals(person.name)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}


	public interface VarargTestInterface {

		@SuppressWarnings("unchecked")
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

		A, B
	}


	public enum MyOtherEnum implements MyInterface {

		C, D
	}

}
