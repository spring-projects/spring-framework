/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.context.annotation.EarlyBeanReferenceProxyCreator.FINAL_CLASS_ERROR_MESSAGE;
import static org.springframework.context.annotation.EarlyBeanReferenceProxyCreator.MISSING_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE;
import static org.springframework.context.annotation.EarlyBeanReferenceProxyCreator.PRIVATE_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodParameter;

import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Unit tests for {@link EarlyBeanReferenceProxyCreator}, ensuring that
 * {@link EarlyBeanReferenceProxy} objects behave properly.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class EarlyBeanReferenceProxyCreatorTests {

	private DefaultListableBeanFactory bf;

	@Before
	public void setUp() {
		bf = new DefaultListableBeanFactory();
	}

	@Test
	public void proxyToStringAvoidsEagerInstantiation() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);

		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(proxy.toString(), equalTo("EarlyBeanReferenceProxy for bean of type TestBean"));
	}

	@Test(expected=NoSuchBeanDefinitionException.class)
	public void proxyThrowsNoSuchBeanDefinitionExceptionWhenDelegatingMethodCallToNonExistentBean() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		proxy.getName();
	}

	@Test
	public void proxyHashCodeAvoidsEagerInstantiation() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(proxy.hashCode(), equalTo(System.identityHashCode(proxy)));
	}

	@Test
	public void proxyEqualsAvoidsEagerInstantiation() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);

		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(proxy.equals(new Object()), is(false));
		assertThat(proxy.equals(proxy), is(true));

		TestBean proxy2 = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(proxy, not(sameInstance(proxy2)));
		assertThat(proxy.equals(proxy2), is(false));
		assertThat(proxy2.equals(proxy), is(false));
		assertThat(proxy2.equals(proxy2), is(true));
	}

	@Test
	public void proxyFinalizeAvoidsEagerInstantiation() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);

		BeanWithFinalizer proxy = (BeanWithFinalizer) pc.createProxy(descriptorFor(BeanWithFinalizer.class));

		assertThat(BeanWithFinalizer.finalizerWasCalled, is(false));
		BeanWithFinalizer.class.getDeclaredMethod("finalize").invoke(proxy);
		assertThat(BeanWithFinalizer.finalizerWasCalled, is(false));
	}

	@Test
	public void proxyMethodsDelegateToTargetBeanCausingSingletonRegistrationIfNecessary() throws Exception {
		bf.registerBeanDefinition("testBean",
				BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "testBeanName").getBeanDefinition());
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(bf.containsBeanDefinition("testBean"), is(true));
		assertThat(bf.containsSingleton("testBean"), is(false));
		assertThat(proxy.getName(), equalTo("testBeanName"));
		assertThat(bf.containsSingleton("testBean"), is(true));
	}

	@Test
	public void beanAnnotatedMethodsReturnEarlyProxyAsWell() throws Exception {
		bf.registerBeanDefinition("componentWithInterfaceBeanMethod", new RootBeanDefinition(ComponentWithInterfaceBeanMethod.class));
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		ComponentWithInterfaceBeanMethod proxy = (ComponentWithInterfaceBeanMethod) pc.createProxy(descriptorFor(ComponentWithInterfaceBeanMethod.class));

		ITestBean bean = proxy.aBeanMethod();
		assertThat(bean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(bf.containsBeanDefinition("componentWithInterfaceBeanMethod"), is(true));
		assertThat("calling a @Bean method on an EarlyBeanReferenceProxy object " +
				"should not cause its instantation/registration",
				bf.containsSingleton("componentWithInterfaceBeanMethod"), is(false));

		Object obj = proxy.normalInstanceMethod();
		assertThat(bf.containsSingleton("componentWithInterfaceBeanMethod"), is(true));
		assertThat(obj, not(instanceOf(EarlyBeanReferenceProxy.class)));
	}

	@Test
	public void proxiesReturnedFromBeanAnnotatedMethodsDereferenceAndDelegateToTheirTargetBean() throws Exception {
		bf.registerBeanDefinition("componentWithConcreteBeanMethod", new RootBeanDefinition(ComponentWithConcreteBeanMethod.class));
		RootBeanDefinition beanMethodBeanDef = new RootBeanDefinition();
		beanMethodBeanDef.setFactoryBeanName("componentWithConcreteBeanMethod");
		beanMethodBeanDef.setFactoryMethodName("aBeanMethod");
		bf.registerBeanDefinition("aBeanMethod", beanMethodBeanDef);
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		ComponentWithConcreteBeanMethod proxy = (ComponentWithConcreteBeanMethod) pc.createProxy(descriptorFor(ComponentWithConcreteBeanMethod.class));

		TestBean bean = proxy.aBeanMethod();
		assertThat(bean.getName(), equalTo("concrete"));
	}

	@Test
	public void interfaceBeansAreProxied() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		ITestBean proxy = (ITestBean) pc.createProxy(descriptorFor(ITestBean.class));

		assertThat(proxy, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(AopUtils.isCglibProxyClass(proxy.getClass()), is(true));
		assertEquals(
				"interface-based bean proxies should have Object as superclass",
				proxy.getClass().getSuperclass(), Object.class);
	}

	@Test
	public void concreteBeansAreProxied() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		TestBean proxy = (TestBean) pc.createProxy(descriptorFor(TestBean.class));

		assertThat(proxy, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(AopUtils.isCglibProxyClass(proxy.getClass()), is(true));
		assertEquals(
				"concrete bean proxies should have the bean class as superclass",
				proxy.getClass().getSuperclass(), TestBean.class);
	}

	@Test
	public void beanAnnotatedMethodsWithInterfaceReturnTypeAreProxied() throws Exception {
		bf.registerBeanDefinition("componentWithInterfaceBeanMethod", new RootBeanDefinition(ComponentWithInterfaceBeanMethod.class));
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		ComponentWithInterfaceBeanMethod proxy = (ComponentWithInterfaceBeanMethod) pc.createProxy(descriptorFor(ComponentWithInterfaceBeanMethod.class));

		ITestBean bean = proxy.aBeanMethod();
		assertThat(bean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(AopUtils.isCglibProxyClass(bean.getClass()), is(true));
		assertEquals(
				"interface-based bean proxies should have Object as superclass",
				bean.getClass().getSuperclass(), Object.class);
	}

	@Test
	public void beanAnnotatedMethodsWithConcreteReturnTypeAreProxied() throws Exception {
		bf.registerBeanDefinition("componentWithConcreteBeanMethod", new RootBeanDefinition(ComponentWithConcreteBeanMethod.class));
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		ComponentWithConcreteBeanMethod proxy = (ComponentWithConcreteBeanMethod) pc.createProxy(descriptorFor(ComponentWithConcreteBeanMethod.class));

		TestBean bean = proxy.aBeanMethod();
		assertThat(bean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(AopUtils.isCglibProxyClass(bean.getClass()), is(true));
		assertEquals(
				"concrete bean proxies should have the bean class as superclass",
				bean.getClass().getSuperclass(), TestBean.class);
	}

	@Test
	public void attemptToProxyClassMissingNoArgConstructorFailsGracefully() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		try {
			pc.createProxy(descriptorFor(BeanMissingNoArgConstructor.class));
			fail("expected ProxyCreationException");
		} catch(ProxyCreationException ex) {
			assertThat(ex.getMessage(),
					equalTo(format(MISSING_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE, BeanMissingNoArgConstructor.class.getName())));
		}
	}

	@Test
	public void attemptToProxyClassWithPrivateNoArgConstructorFailsGracefully() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		try {
			pc.createProxy(descriptorFor(BeanWithPrivateNoArgConstructor.class));
			fail("expected ProxyCreationException");
		} catch(ProxyCreationException ex) {
			assertThat(ex.getMessage(),
					equalTo(format(PRIVATE_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE, BeanWithPrivateNoArgConstructor.class.getName())));
		}
	}

	@Test
	public void attemptToProxyFinalClassFailsGracefully() throws Exception {
		EarlyBeanReferenceProxyCreator pc = new EarlyBeanReferenceProxyCreator(bf);
		try {
			pc.createProxy(descriptorFor(FinalBean.class));
			fail("expected ProxyCreationException");
		} catch(ProxyCreationException ex) {
			assertThat(ex.getMessage(),
					equalTo(format(FINAL_CLASS_ERROR_MESSAGE, FinalBean.class.getName())));
		}
	}

	private DependencyDescriptor descriptorFor(Class<?> paramType) throws Exception {
		@SuppressWarnings("unused")
		class C {
			void m(ITestBean p) { }
			void m(TestBean p) { }
			void m(BeanMissingNoArgConstructor p) { }
			void m(BeanWithPrivateNoArgConstructor p) { }
			void m(FinalBean p) { }
			void m(BeanWithFinalizer p) { }
			void m(ComponentWithConcreteBeanMethod p) { }
			void m(ComponentWithInterfaceBeanMethod p) { }
		}

		Method targetMethod = C.class.getDeclaredMethod("m", new Class<?>[] { paramType });
		MethodParameter mp = new MethodParameter(targetMethod, 0);
		DependencyDescriptor dd = new DependencyDescriptor(mp, true, false);
		return dd;
	}


	static class BeanMissingNoArgConstructor {
		BeanMissingNoArgConstructor(Object o) { }
	}


	static class BeanWithPrivateNoArgConstructor {
		private BeanWithPrivateNoArgConstructor() { }
	}


	static final class FinalBean {
	}


	static class BeanWithFinalizer {
		static Boolean finalizerWasCalled = false;

		@Override
		protected void finalize() throws Throwable {
			finalizerWasCalled = true;
		}
	}


	static class ComponentWithConcreteBeanMethod {
		@Bean
		public TestBean aBeanMethod() {
			return new TestBean("concrete");
		}

		public Object normalInstanceMethod() {
			return new Object();
		}
	}


	static class ComponentWithInterfaceBeanMethod {
		@Bean
		public ITestBean aBeanMethod() {
			return new TestBean("interface");
		}

		public Object normalInstanceMethod() {
			return new Object();
		}
	}
}
