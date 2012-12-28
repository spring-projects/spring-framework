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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import test.beans.ITestBean;
import test.beans.TestBean;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Tests that scopes are properly supported by using a custom Scope implementations
 * and scoped proxy {@link Bean} declarations.
 *
 * @author Costin Leau
 * @author Chris Beams
 */
public class ScopingTests {

	public static String flag = "1";

	private static final String SCOPE = "my scope";

	private CustomScope customScope;

	private GenericApplicationContext ctx;

	@Before
	public void setUp() throws Exception {
		customScope = new CustomScope();
		ctx = createContext(customScope, ScopedConfigurationClass.class);
	}

	@After
	public void tearDown() throws Exception {
		if (ctx != null) {
			ctx.close();
		}
		ctx = null;
		customScope = null;
	}

	private GenericApplicationContext createContext(org.springframework.beans.factory.config.Scope customScope, Class<?> configClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if (customScope != null) {
			beanFactory.registerScope(SCOPE, customScope);
		}
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(configClass));
		GenericApplicationContext ctx = new GenericApplicationContext(beanFactory);
		ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor());
		ctx.refresh();
		return ctx;
	}

	@Test
	public void testScopeOnClasses() throws Exception {
		genericTestScope("scopedClass");
	}

	@Test
	public void testScopeOnInterfaces() throws Exception {
		genericTestScope("scopedInterface");
	}

	@Test
	public void testSameScopeOnDifferentBeans() throws Exception {
		Object beanAInScope = ctx.getBean("scopedClass");
		Object beanBInScope = ctx.getBean("scopedInterface");

		assertNotSame(beanAInScope, beanBInScope);

		customScope.createNewScope = true;

		Object newBeanAInScope = ctx.getBean("scopedClass");
		Object newBeanBInScope = ctx.getBean("scopedInterface");

		assertNotSame(newBeanAInScope, newBeanBInScope);
		assertNotSame(newBeanAInScope, beanAInScope);
		assertNotSame(newBeanBInScope, beanBInScope);
	}

	@Test
	public void testRawScopes() throws Exception {
		String beanName = "scopedProxyInterface";

		// get hidden bean
		Object bean = ctx.getBean("scopedTarget." + beanName);

		assertFalse(bean instanceof ScopedObject);
	}

	@Test
	public void testScopedProxyConfiguration() throws Exception {
		TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedInterfaceDep");
		ITestBean spouse = singleton.getSpouse();
		assertTrue("scoped bean is not wrapped by the scoped-proxy", spouse instanceof ScopedObject);

		String beanName = "scopedProxyInterface";

		String scopedBeanName = "scopedTarget." + beanName;

		// get hidden bean
		assertEquals(flag, spouse.getName());

		ITestBean spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		assertEquals(spouse.getName(), spouseFromBF.getName());
		// the scope proxy has kicked in
		assertNotSame(spouse, spouseFromBF);

		// create a new bean
		customScope.createNewScope = true;

		// get the bean again from the BF
		spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		// make sure the name has been updated
		assertSame(spouse.getName(), spouseFromBF.getName());
		assertNotSame(spouse, spouseFromBF);

		// get the bean again
		spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		assertSame(spouse.getName(), spouseFromBF.getName());
	}


	@Test
	public void testScopedProxyConfigurationWithClasses() throws Exception {

		TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedClassDep");
		ITestBean spouse = singleton.getSpouse();
		assertTrue("scoped bean is not wrapped by the scoped-proxy", spouse instanceof ScopedObject);

		String beanName = "scopedProxyClass";

		String scopedBeanName = "scopedTarget." + beanName;

		// get hidden bean
		assertEquals(flag, spouse.getName());

		TestBean spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		assertEquals(spouse.getName(), spouseFromBF.getName());
		// the scope proxy has kicked in
		assertNotSame(spouse, spouseFromBF);

		// create a new bean
		customScope.createNewScope = true;
		flag = "boo";

		// get the bean again from the BF
		spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		// make sure the name has been updated
		assertSame(spouse.getName(), spouseFromBF.getName());
		assertNotSame(spouse, spouseFromBF);

		// get the bean again
		spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		assertSame(spouse.getName(), spouseFromBF.getName());
	}


	@Test
	public void testScopedConfigurationBeanDefinitionCount() throws Exception {
		// count the beans
		// 6 @Beans + 1 Configuration + 2 scoped proxy + 1 importRegistry
		assertEquals(10, ctx.getBeanDefinitionCount());
	}

//	/**
//	 * SJC-254 caught a regression in handling scoped proxies starting in 1.0 m4.
//	 * The ScopedProxyFactoryBean object was having its scope set to that of its delegate
//	 * whereas it should have remained singleton.
//	 */
//	@Test
//	public void sjc254() {
//		JavaConfigWebApplicationContext ctx = new JavaConfigWebApplicationContext();
//		ctx.setConfigLocations(new String[] { ScopeTestConfiguration.class.getName() });
//		ctx.refresh();
//
//		// should be fine
//		ctx.getBean(Bar.class);
//
//		boolean threw = false;
//		try {
//			ctx.getBean(Foo.class);
//		} catch (BeanCreationException ex) {
//			if(ex.getCause() instanceof IllegalStateException) {
//				threw = true;
//			}
//		}
//		assertTrue(threw);
//	}

	@Configuration
	static class ScopeTestConfiguration {

		@Bean
		@Scope(value="session", proxyMode=ScopedProxyMode.INTERFACES)
		public Foo foo() {
			return new Foo();
		}

		@Bean
		public Bar bar() {
			return new Bar(foo());
		}
	}

	static class Foo {
		public Foo() {
			//System.out.println("created foo: " + this.getClass().getName());
		}

		public void doSomething() {
			//System.out.println("interesting: " + this);
		}
	}

	static class Bar {

		private final Foo foo;

		public Bar(Foo foo) {
			this.foo = foo;
			//System.out.println("created bar: " + this);
		}

		public Foo getFoo() {
			return foo;
		}

	}

	private void genericTestScope(String beanName) throws Exception {
		String message = "scope is ignored";
		Object bean1 = ctx.getBean(beanName);
		Object bean2 = ctx.getBean(beanName);

		assertSame(message, bean1, bean2);

		Object bean3 = ctx.getBean(beanName);

		assertSame(message, bean1, bean3);

		// make the scope create a new object
		customScope.createNewScope = true;

		Object newBean1 = ctx.getBean(beanName);
		assertNotSame(message, bean1, newBean1);

		Object sameBean1 = ctx.getBean(beanName);

		assertSame(message, newBean1, sameBean1);

		// make the scope create a new object
		customScope.createNewScope = true;

		Object newBean2 = ctx.getBean(beanName);
		assertNotSame(message, newBean1, newBean2);

		// make the scope create a new object .. again
		customScope.createNewScope = true;

		Object newBean3 = ctx.getBean(beanName);
		assertNotSame(message, newBean2, newBean3);
	}

	@Configuration
	public static class InvalidProxyOnPredefinedScopesConfiguration {

		@Bean @Scope(proxyMode=ScopedProxyMode.INTERFACES)
		public Object invalidProxyOnPredefinedScopes() { return new Object(); }
	}

	@Configuration
	public static class ScopedConfigurationClass {

		@Bean
		@MyScope
		public TestBean scopedClass() {
			TestBean tb = new TestBean();
			tb.setName(flag);
			return tb;
		}

		@Bean
		@MyScope
		public ITestBean scopedInterface() {
			TestBean tb = new TestBean();
			tb.setName(flag);
			return tb;
		}

		@Bean
		@MyProxiedScope
		public ITestBean scopedProxyInterface() {
			TestBean tb = new TestBean();
			tb.setName(flag);
			return tb;
		}

		@MyProxiedScope
		public TestBean scopedProxyClass() {
			TestBean tb = new TestBean();
			tb.setName(flag);
			return tb;
		}

		@Bean
		public TestBean singletonWithScopedClassDep() {
			TestBean singleton = new TestBean();
			singleton.setSpouse(scopedProxyClass());
			return singleton;
		}

		@Bean
		public TestBean singletonWithScopedInterfaceDep() {
			TestBean singleton = new TestBean();
			singleton.setSpouse(scopedProxyInterface());
			return singleton;
		}
	}


	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Scope(SCOPE)
	@interface MyScope {
	}


	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Bean
	@Scope(value=SCOPE, proxyMode=ScopedProxyMode.TARGET_CLASS)
	@interface MyProxiedScope {
	}


	/**
	 * Simple scope implementation which creates object based on a flag.
	 * @author Costin Leau
	 * @author Chris Beams
	 */
	static class CustomScope implements org.springframework.beans.factory.config.Scope {

		public boolean createNewScope = true;

		private Map<String, Object> beans = new HashMap<String, Object>();

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			if (createNewScope) {
				beans.clear();
				// reset the flag back
				createNewScope = false;
			}

			Object bean = beans.get(name);
			// if a new object is requested or none exists under the current
			// name, create one
			if (bean == null) {
				beans.put(name, objectFactory.getObject());
			}

			return beans.get(name);
		}

		@Override
		public String getConversationId() {
			return null;
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
			// do nothing
		}

		@Override
		public Object remove(String name) {
			return beans.remove(name);
		}

		@Override
		public Object resolveContextualObject(String key) {
			return null;
		}
	}

}
