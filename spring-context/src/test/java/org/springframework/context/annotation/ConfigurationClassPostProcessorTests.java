/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Chris Beams
 */
public class ConfigurationClassPostProcessorTests {

	/**
	 * Enhanced {@link Configuration} classes are only necessary for respecting
	 * certain bean semantics, like singleton-scoping, scoped proxies, etc.
	 * <p>Technically, {@link ConfigurationClassPostProcessor} could fail to enhance the
	 * registered Configuration classes and many use cases would still work.
	 * Certain cases, however, like inter-bean singleton references would not.
	 * We test for such a case below, and in doing so prove that enhancement is
	 * working.
	 */
	@Test
	public void testEnhancementIsPresentBecauseSingletonSemanticsAreRespected() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertSame(foo, bar.foo);
	}

	/**
	 * Tests the fix for SPR-5655, a special workaround that prefers reflection
	 * over ASM if a bean class is already loaded.
	 */
	@Test
	public void testAlreadyLoadedConfigurationClasses() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("unloadedConfig",
				new RootBeanDefinition(UnloadedConfig.class.getName(), null, null));
		beanFactory.registerBeanDefinition("loadedConfig", new RootBeanDefinition(LoadedConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("foo");
		beanFactory.getBean("bar");
	}

	/**
	 * Tests whether a bean definition without a specified bean class is handled
	 * correctly.
	 */
	@Test
	public void testPostProcessorIntrospectsInheritedDefinitionsCorrectly() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("parent", new RootBeanDefinition(TestBean.class));
		beanFactory.registerBeanDefinition("child", new ChildBeanDefinition("parent"));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertSame(foo, bar.foo);
	}

	@Test
	public void testPostProcessorOverridesNonApplicationBeanDefinitions() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setRole(RootBeanDefinition.ROLE_SUPPORT);
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertSame(foo, bar.foo);
	}

	@Test
	public void testPostProcessorDoesNotOverrideRegularBeanDefinitions() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setResource(new DescriptiveResource("XML or something"));
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		TestBean bar = beanFactory.getBean("bar", TestBean.class);
	}

	@Test
	public void testProcessingAllowedOnlyOncePerProcessorRegistryPair() {
		DefaultListableBeanFactory bf1 = new DefaultListableBeanFactory();
		DefaultListableBeanFactory bf2 = new DefaultListableBeanFactory();
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(bf1); // first invocation -- should succeed
		try {
			pp.postProcessBeanFactory(bf1); // second invocation for bf1 -- should throw
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
		}
		pp.postProcessBeanFactory(bf2); // first invocation for bf2 -- should succeed
		try {
			pp.postProcessBeanFactory(bf2); // second invocation for bf2 -- should throw
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
		}
	}

	@Test
	public void testGenericsBasedInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		bf.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(bf);

		RepositoryInjectionBean bean = (RepositoryInjectionBean) bf.getBean("annotatedBean");
		assertSame(bf.getBean("stringRepo"), bean.stringRepository);
		assertSame(bf.getBean("integerRepo"), bean.integerRepository);
	}


	@Configuration
	static class SingletonBeanConfig {

		public @Bean Foo foo() {
			return new Foo();
		}

		public @Bean Bar bar() {
			return new Bar(foo());
		}
	}


	static class Foo {
	}


	static class Bar {
		final Foo foo;
		public Bar(Foo foo) { this.foo = foo; }
	}


	@Configuration
	static class UnloadedConfig {
		public @Bean Foo foo() {
			return new Foo();
		}
	}


	@Configuration
	static class LoadedConfig {
		public @Bean Bar bar() {
			return new Bar(new Foo());
		}
	}


	public interface Repository<T> {
	}


	public static class RepositoryInjectionBean {

		@Autowired
		public Repository<String> stringRepository;

		@Autowired
		public Repository<Integer> integerRepository;
	}


	@Configuration
	public static class RepositoryConfiguration {

		@Bean
		public Repository<String> stringRepo() {
			return new Repository<String>() {
			};
		}

		@Bean
		public Repository<Integer> integerRepo() {
			return new Repository<Integer>() {
			};
		}
	}

}
