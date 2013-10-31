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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ConfigurationClassPostProcessorTests {

	private DefaultListableBeanFactory beanFactory;

	@Before
	public void setUp() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		QualifierAnnotationAutowireCandidateResolver acr = new QualifierAnnotationAutowireCandidateResolver();
		acr.setBeanFactory(bf);
		bf.setAutowireCandidateResolver(acr);
		this.beanFactory = bf;
	}

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
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertSame(beanFactory.getBean("stringRepo"), bean.stringRepository);
		assertSame(beanFactory.getBean("integerRepo"), bean.integerRepository);
	}

	@Test
	public void testGenericsBasedInjectionWithImplTypeAtInjectionPoint() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(SpecificRepositoryInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(SpecificRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		SpecificRepositoryInjectionBean bean = (SpecificRepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertSame(beanFactory.getBean("genericRepo"), bean.genericRepository);
	}

	@Test
	public void testGenericsBasedInjectionWithFactoryBean() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryFactoryBeanInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryFactoryBeanConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		RepositoryFactoryBeanInjectionBean bean = (RepositoryFactoryBeanInjectionBean) beanFactory.getBean("annotatedBean");
		assertSame(beanFactory.getBean("&repoFactoryBean"), bean.repositoryFactoryBean);
		assertSame(beanFactory.getBean("&repoFactoryBean"), bean.qualifiedRepositoryFactoryBean);
		assertSame(beanFactory.getBean("&repoFactoryBean"), bean.prefixQualifiedRepositoryFactoryBean);
	}

	@Test
	public void testGenericsBasedInjectionWithRawMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawMatchingConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertSame(beanFactory.getBean("repo"), beanFactory.getBean("repoConsumer"));
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

		public Bar(Foo foo) {
			this.foo = foo;
		}
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


	public static class Repository<T> {
	}


	public static class GenericRepository<T> extends Repository<T> {
	}


	public static class RepositoryFactoryBean<T> implements FactoryBean<T> {

		@Override
		public T getObject() {
			throw new IllegalStateException();
		}

		@Override
		public Class<?> getObjectType() {
			return Object.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}
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
			return new Repository<String>();
		}

		@Bean
		public Repository<Integer> integerRepo() {
			return new Repository<Integer>();
		}
	}


	public static class SpecificRepositoryInjectionBean {

		@Autowired
		public GenericRepository<?> genericRepository;
	}


	@Configuration
	public static class SpecificRepositoryConfiguration {

		@Bean
		public Repository<Object> genericRepo() {
			return new GenericRepository<Object>();
		}
	}


	public static class RepositoryFactoryBeanInjectionBean {

		@Autowired
		public RepositoryFactoryBean<?> repositoryFactoryBean;

		@Autowired
		@Qualifier("repoFactoryBean")
		public RepositoryFactoryBean<?> qualifiedRepositoryFactoryBean;

		@Autowired
		@Qualifier("&repoFactoryBean")
		public RepositoryFactoryBean<?> prefixQualifiedRepositoryFactoryBean;
	}


	@Configuration
	public static class RepositoryFactoryBeanConfiguration {

		@Bean
		public RepositoryFactoryBean<Object> repoFactoryBean() {
			return new RepositoryFactoryBean<>();
		}
	}


	@Configuration
	public static class RawMatchingConfiguration {

		@Bean
		public Repository repo() {
			return new Repository();
		}

		@Bean
		public Object repoConsumer(Repository<String> repo) {
			return repo;
		}
	}

}
