/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

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


	@BeforeEach
	public void setUp() throws Exception {
		customScope = new CustomScope();
		ctx = createContext(ScopedConfigurationClass.class);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (ctx != null) {
			ctx.close();
		}
	}

	private GenericApplicationContext createContext(Class<?> configClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if (customScope != null) {
			beanFactory.registerScope(SCOPE, customScope);
		}
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(configClass));
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(beanFactory);
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

	private void genericTestScope(String beanName) throws Exception {
		String message = "scope is ignored";
		Object bean1 = ctx.getBean(beanName);
		Object bean2 = ctx.getBean(beanName);

		assertThat(bean2).as(message).isSameAs(bean1);

		Object bean3 = ctx.getBean(beanName);

		assertThat(bean3).as(message).isSameAs(bean1);

		// make the scope create a new object
		customScope.createNewScope = true;

		Object newBean1 = ctx.getBean(beanName);
		assertThat(newBean1).as(message).isNotSameAs(bean1);

		Object sameBean1 = ctx.getBean(beanName);

		assertThat(sameBean1).as(message).isSameAs(newBean1);

		// make the scope create a new object
		customScope.createNewScope = true;

		Object newBean2 = ctx.getBean(beanName);
		assertThat(newBean2).as(message).isNotSameAs(newBean1);

		// make the scope create a new object .. again
		customScope.createNewScope = true;

		Object newBean3 = ctx.getBean(beanName);
		assertThat(newBean3).as(message).isNotSameAs(newBean2);
	}

	@Test
	public void testSameScopeOnDifferentBeans() throws Exception {
		Object beanAInScope = ctx.getBean("scopedClass");
		Object beanBInScope = ctx.getBean("scopedInterface");

		assertThat(beanBInScope).isNotSameAs(beanAInScope);

		customScope.createNewScope = true;

		Object newBeanAInScope = ctx.getBean("scopedClass");
		Object newBeanBInScope = ctx.getBean("scopedInterface");

		assertThat(newBeanBInScope).isNotSameAs(newBeanAInScope);
		assertThat(beanAInScope).isNotSameAs(newBeanAInScope);
		assertThat(beanBInScope).isNotSameAs(newBeanBInScope);
	}

	@Test
	public void testRawScopes() throws Exception {
		String beanName = "scopedProxyInterface";

		// get hidden bean
		Object bean = ctx.getBean("scopedTarget." + beanName);

		boolean condition = bean instanceof ScopedObject;
		assertThat(condition).isFalse();
	}

	@Test
	public void testScopedProxyConfiguration() throws Exception {
		TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedInterfaceDep");
		ITestBean spouse = singleton.getSpouse();
		boolean condition = spouse instanceof ScopedObject;
		assertThat(condition).as("scoped bean is not wrapped by the scoped-proxy").isTrue();

		String beanName = "scopedProxyInterface";

		String scopedBeanName = "scopedTarget." + beanName;

		// get hidden bean
		assertThat(spouse.getName()).isEqualTo(flag);

		ITestBean spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		assertThat(spouseFromBF.getName()).isEqualTo(spouse.getName());
		// the scope proxy has kicked in
		assertThat(spouseFromBF).isNotSameAs(spouse);

		// create a new bean
		customScope.createNewScope = true;

		// get the bean again from the BF
		spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		// make sure the name has been updated
		assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
		assertThat(spouseFromBF).isNotSameAs(spouse);

		// get the bean again
		spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
		assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
	}

	@Test
	public void testScopedProxyConfigurationWithClasses() throws Exception {
		TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedClassDep");
		ITestBean spouse = singleton.getSpouse();
		boolean condition = spouse instanceof ScopedObject;
		assertThat(condition).as("scoped bean is not wrapped by the scoped-proxy").isTrue();

		String beanName = "scopedProxyClass";

		String scopedBeanName = "scopedTarget." + beanName;

		// get hidden bean
		assertThat(spouse.getName()).isEqualTo(flag);

		TestBean spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		assertThat(spouseFromBF.getName()).isEqualTo(spouse.getName());
		// the scope proxy has kicked in
		assertThat(spouseFromBF).isNotSameAs(spouse);

		// create a new bean
		customScope.createNewScope = true;
		flag = "boo";

		// get the bean again from the BF
		spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		// make sure the name has been updated
		assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
		assertThat(spouseFromBF).isNotSameAs(spouse);

		// get the bean again
		spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
		assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
	}


	static class Foo {

		public Foo() {
		}

		public void doSomething() {
		}
	}


	static class Bar {

		private final Foo foo;

		public Bar(Foo foo) {
			this.foo = foo;
		}

		public Foo getFoo() {
			return foo;
		}
	}


	@Configuration
	public static class InvalidProxyOnPredefinedScopesConfiguration {

		@Bean @Scope(proxyMode=ScopedProxyMode.INTERFACES)
		public Object invalidProxyOnPredefinedScopes() {
			return new Object();
		}
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

		private Map<String, Object> beans = new HashMap<>();

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
			throw new IllegalStateException("Not supposed to be called");
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
