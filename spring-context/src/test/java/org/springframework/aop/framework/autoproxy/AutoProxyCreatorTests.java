/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.IndexedTestBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.DummyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

/**
 * @since 09.12.2003
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class AutoProxyCreatorTests {

	@Test
	public void testBeanNameAutoProxyCreator() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testInterceptor", TestInterceptor.class);

		RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
		proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
		proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,innerBean,singletonFactoryToBeProxied");
		sac.getDefaultListableBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class, RootBeanDefinition.AUTOWIRE_BY_TYPE);
		RootBeanDefinition innerBean = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().add("spouse", new BeanDefinitionHolder(innerBean, "innerBean"));
		sac.getDefaultListableBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
		sac.registerSingleton("autowiredIndexedTestBean", IndexedTestBean.class);

		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		assertFalse(Proxy.isProxyClass(messageSource.getClass()));
		assertTrue(Proxy.isProxyClass(singletonToBeProxied.getClass()));
		assertTrue(Proxy.isProxyClass(singletonToBeProxied.getSpouse().getClass()));

		// test whether autowiring succeeded with auto proxy creation
		assertEquals(sac.getBean("autowiredIndexedTestBean"), singletonToBeProxied.getNestedIndexedBean());

		TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
		// already 2: getSpouse + getNestedIndexedBean calls above
		assertEquals(2, ti.nrOfInvocations);
		singletonToBeProxied.getName();
		singletonToBeProxied.getSpouse().getName();
		assertEquals(5, ti.nrOfInvocations);

		ITestBean tb = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
		assertTrue(AopUtils.isJdkDynamicProxy(tb));
		assertEquals(5, ti.nrOfInvocations);
		tb.getAge();
		assertEquals(6, ti.nrOfInvocations);

		ITestBean tb2 = (ITestBean) sac.getBean("singletonFactoryToBeProxied");
		assertSame(tb, tb2);
		assertEquals(6, ti.nrOfInvocations);
		tb2.getAge();
		assertEquals(7, ti.nrOfInvocations);
	}

	@Test
	public void testBeanNameAutoProxyCreatorWithFactoryBeanProxy() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testInterceptor", TestInterceptor.class);

		RootBeanDefinition proxyCreator = new RootBeanDefinition(BeanNameAutoProxyCreator.class);
		proxyCreator.getPropertyValues().add("interceptorNames", "testInterceptor");
		proxyCreator.getPropertyValues().add("beanNames", "singletonToBeProxied,&singletonFactoryToBeProxied");
		sac.getDefaultListableBeanFactory().registerBeanDefinition("beanNameAutoProxyCreator", proxyCreator);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		sac.getDefaultListableBeanFactory().registerBeanDefinition("singletonToBeProxied", bd);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

		sac.refresh();

		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		assertTrue(Proxy.isProxyClass(singletonToBeProxied.getClass()));

		// 2 invocations coming from FactoryBean inspection during lifecycle startup

		TestInterceptor ti = (TestInterceptor) sac.getBean("testInterceptor");
		assertEquals(2, ti.nrOfInvocations);
		singletonToBeProxied.getName();
		assertEquals(3, ti.nrOfInvocations);

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertTrue(Proxy.isProxyClass(factory.getClass()));
		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertFalse(AopUtils.isAopProxy(tb));
		assertEquals(5, ti.nrOfInvocations);
		tb.getAge();
		assertEquals(5, ti.nrOfInvocations);
	}

	@Test
	public void testCustomAutoProxyCreator() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);
		sac.registerSingleton("singletonNoInterceptor", TestBean.class);
		sac.registerSingleton("singletonToBeProxied", TestBean.class);
		sac.registerPrototype("prototypeToBeProxied", TestBean.class);
		sac.refresh();

		MessageSource messageSource = (MessageSource) sac.getBean("messageSource");
		ITestBean singletonNoInterceptor = (ITestBean) sac.getBean("singletonNoInterceptor");
		ITestBean singletonToBeProxied = (ITestBean) sac.getBean("singletonToBeProxied");
		ITestBean prototypeToBeProxied = (ITestBean) sac.getBean("prototypeToBeProxied");
		assertFalse(AopUtils.isCglibProxy(messageSource));
		assertTrue(AopUtils.isCglibProxy(singletonNoInterceptor));
		assertTrue(AopUtils.isCglibProxy(singletonToBeProxied));
		assertTrue(AopUtils.isCglibProxy(prototypeToBeProxied));

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
		assertEquals(0, tapc.testInterceptor.nrOfInvocations);
		singletonNoInterceptor.getName();
		assertEquals(0, tapc.testInterceptor.nrOfInvocations);
		singletonToBeProxied.getAge();
		assertEquals(1, tapc.testInterceptor.nrOfInvocations);
		prototypeToBeProxied.getSpouse();
		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
	}

	@Test
	public void testAutoProxyCreatorWithFactoryBean() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);
		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);
		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(factory));

		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(tb));
		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
		tb.getAge();
		assertEquals(3, tapc.testInterceptor.nrOfInvocations);
	}

	@Test
	public void testAutoProxyCreatorWithFactoryBeanAndPrototype() {
		StaticApplicationContext sac = new StaticApplicationContext();
		sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("singleton", "false");
		sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(prototypeFactory));
		TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(tb));

		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
		tb.getAge();
		assertEquals(3, tapc.testInterceptor.nrOfInvocations);
	}

	@Test
	public void testAutoProxyCreatorWithFactoryBeanAndProxyObjectOnly() {
		StaticApplicationContext sac = new StaticApplicationContext();

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("proxyFactoryBean", "false");
		sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class, pvs);

		sac.registerSingleton("singletonFactoryToBeProxied", DummyFactory.class);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> factory = (FactoryBean<?>) sac.getBean("&singletonFactoryToBeProxied");
		assertFalse(AopUtils.isAopProxy(factory));

		TestBean tb = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(tb));
		assertEquals(0, tapc.testInterceptor.nrOfInvocations);
		tb.getAge();
		assertEquals(1, tapc.testInterceptor.nrOfInvocations);

		TestBean tb2 = (TestBean) sac.getBean("singletonFactoryToBeProxied");
		assertSame(tb, tb2);
		assertEquals(1, tapc.testInterceptor.nrOfInvocations);
		tb2.getAge();
		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
	}

	@Test
	public void testAutoProxyCreatorWithFactoryBeanAndProxyFactoryBeanOnly() {
		StaticApplicationContext sac = new StaticApplicationContext();

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("proxyObject", "false");
		sac.registerSingleton("testAutoProxyCreator", TestAutoProxyCreator.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("singleton", "false");
		sac.registerSingleton("prototypeFactoryToBeProxied", DummyFactory.class, pvs);

		sac.refresh();

		TestAutoProxyCreator tapc = (TestAutoProxyCreator) sac.getBean("testAutoProxyCreator");
		tapc.testInterceptor.nrOfInvocations = 0;

		FactoryBean<?> prototypeFactory = (FactoryBean<?>) sac.getBean("&prototypeFactoryToBeProxied");
		assertTrue(AopUtils.isCglibProxy(prototypeFactory));
		TestBean tb = (TestBean) sac.getBean("prototypeFactoryToBeProxied");
		assertFalse(AopUtils.isCglibProxy(tb));

		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
		tb.getAge();
		assertEquals(2, tapc.testInterceptor.nrOfInvocations);
	}


	@SuppressWarnings("serial")
	public static class TestAutoProxyCreator extends AbstractAutoProxyCreator {

		private boolean proxyFactoryBean = true;

		private boolean proxyObject = true;

		public TestInterceptor testInterceptor = new TestInterceptor();

		public TestAutoProxyCreator() {
			setProxyTargetClass(true);
			setOrder(0);
		}

		public void setProxyFactoryBean(boolean proxyFactoryBean) {
			this.proxyFactoryBean = proxyFactoryBean;
		}

		public void setProxyObject(boolean proxyObject) {
			this.proxyObject = proxyObject;
		}

		protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String name, TargetSource customTargetSource) {
			if (StaticMessageSource.class.equals(beanClass)) {
				return DO_NOT_PROXY;
			}
			else if (name.endsWith("ToBeProxied")) {
				boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
				if ((this.proxyFactoryBean && isFactoryBean) || (this.proxyObject && !isFactoryBean)) {
					return new Object[] {this.testInterceptor};
				}
				else {
					return DO_NOT_PROXY;
				}
			}
			else {
				return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
			}
		}
	}


	/**
	 * Interceptor that counts the number of non-finalize method calls.
	 */
	public static class TestInterceptor implements MethodInterceptor {

		public int nrOfInvocations = 0;

		public Object invoke(MethodInvocation invocation) throws Throwable {
			if (!invocation.getMethod().getName().equals("finalize")) {
				this.nrOfInvocations++;
			}
			return invocation.proceed();
		}
	}

}
