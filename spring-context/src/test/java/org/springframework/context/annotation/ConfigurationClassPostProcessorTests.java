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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import javax.annotation.PostConstruct;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.stereotype.Component;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
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
	public void enhancementIsPresentBecauseSingletonSemanticsAreRespected() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertSame(foo, bar.foo);
	}

	@Test
	public void configurationIntrospectionOfInnerClassesWorksWithDotNameSyntax() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(getClass().getName() + ".SingletonBeanConfig"));
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
	public void alreadyLoadedConfigurationClasses() {
		beanFactory.registerBeanDefinition("unloadedConfig", new RootBeanDefinition(UnloadedConfig.class.getName()));
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
	public void postProcessorIntrospectsInheritedDefinitionsCorrectly() {
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
	public void postProcessorWorksWithComposedConfigurationUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ComposedConfigurationClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedConfigurationUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ComposedConfigurationClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForBasePackage.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForBasePackage.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForExcludeFilter.class);
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForExcludeFilter.class.getName());
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedComposedConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedComposedConfigurationWithAttributeOverridesClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				MetaComponentScanConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				MetaComponentScanConfigurationWithAttributeOverridesClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				SubMetaComponentScanConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	public void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				SubMetaComponentScanConfigurationWithAttributeOverridesClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	private void assertSupportForComposedAnnotation(RootBeanDefinition beanDefinition) {
		beanFactory.registerBeanDefinition("config", beanDefinition);
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.setEnvironment(new StandardEnvironment());
		pp.postProcessBeanFactory(beanFactory);
		SimpleComponent simpleComponent = beanFactory.getBean(SimpleComponent.class);
		assertNotNull(simpleComponent);
	}

	private void assertSupportForComposedAnnotationWithExclude(RootBeanDefinition beanDefinition) {
		beanFactory.registerBeanDefinition("config", beanDefinition);
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.setEnvironment(new StandardEnvironment());
		pp.postProcessBeanFactory(beanFactory);
		try {
			beanFactory.getBean(SimpleComponent.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void postProcessorOverridesNonApplicationBeanDefinitions() {
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
	public void postProcessorDoesNotOverrideRegularBeanDefinitions() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setResource(new DescriptiveResource("XML or something"));
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("foo", Foo.class);
		beanFactory.getBean("bar", TestBean.class);
	}

	@Test
	public void postProcessorDoesNotOverrideRegularBeanDefinitionsEvenWithScopedProxy() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setResource(new DescriptiveResource("XML or something"));
		BeanDefinitionHolder proxied = ScopedProxyUtils.createScopedProxy(
				new BeanDefinitionHolder(rbd, "bar"), beanFactory, true);
		beanFactory.registerBeanDefinition("bar", proxied.getBeanDefinition());
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("foo", Foo.class);
		beanFactory.getBean("bar", TestBean.class);
	}

	@Test
	public void postProcessorFailsOnImplicitOverrideIfOverridingIsNotAllowed() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setResource(new DescriptiveResource("XML or something"));
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		beanFactory.setAllowBeanDefinitionOverriding(false);
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		try {
			pp.postProcessBeanFactory(beanFactory);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			assertTrue(ex.getMessage().contains("bar"));
			assertTrue(ex.getMessage().contains("SingletonBeanConfig"));
			assertTrue(ex.getMessage().contains(TestBean.class.getName()));
		}
	}

	@Test
	public void configurationClassesProcessedInCorrectOrder() {
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		Foo foo = beanFactory.getBean(Foo.class);
		assertTrue(foo instanceof ExtendedFoo);
		Bar bar = beanFactory.getBean(Bar.class);
		assertSame(foo, bar.foo);
	}

	@Test
	public void configurationClassesWithValidOverridingForProgrammaticCall() {
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(OverridingAgainSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config3", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		Foo foo = beanFactory.getBean(Foo.class);
		assertTrue(foo instanceof ExtendedAgainFoo);
		Bar bar = beanFactory.getBean(Bar.class);
		assertSame(foo, bar.foo);
	}

	@Test
	public void configurationClassesWithInvalidOverridingForProgrammaticCall() {
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(InvalidOverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config3", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		try {
			beanFactory.getBean(Bar.class);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("OverridingSingletonBeanConfig.foo"));
			assertTrue(ex.getMessage().contains(ExtendedFoo.class.getName()));
			assertTrue(ex.getMessage().contains(Foo.class.getName()));
			assertTrue(ex.getMessage().contains("InvalidOverridingSingletonBeanConfig"));
		}
	}

	@Test
	public void scopedProxyTargetMarkedAsNonAutowireCandidate() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ScopedProxyConfigurationClass.class));
		beanFactory.registerBeanDefinition("consumer", new RootBeanDefinition(ScopedProxyConsumer.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		ITestBean injected = beanFactory.getBean("consumer", ScopedProxyConsumer.class).testBean;
		assertTrue(injected instanceof ScopedObject);
		assertSame(beanFactory.getBean("scopedClass"), injected);
		assertSame(beanFactory.getBean(ITestBean.class), injected);
	}

	@Test
	public void processingAllowedOnlyOncePerProcessorRegistryPair() {
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
	public void genericsBasedInjection() {
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
		assertEquals("Repository<String>", bean.stringRepository.toString());
		assertEquals("Repository<Integer>", bean.integerRepository.toString());
	}

	@Test
	public void genericsBasedInjectionWithScoped() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertEquals("Repository<String>", bean.stringRepository.toString());
		assertEquals("Repository<Integer>", bean.integerRepository.toString());
	}

	@Test
	public void genericsBasedInjectionWithScopedProxy() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedProxyRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.freezeConfiguration();

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertEquals("Repository<String>", bean.stringRepository.toString());
		assertEquals("Repository<Integer>", bean.integerRepository.toString());
		assertTrue(AopUtils.isCglibProxy(bean.stringRepository));
		assertTrue(AopUtils.isCglibProxy(bean.integerRepository));
	}

	@Test
	public void genericsBasedInjectionWithScopedProxyUsingAsm() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class.getName());
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedProxyRepositoryConfiguration.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.freezeConfiguration();

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertEquals("Repository<String>", bean.stringRepository.toString());
		assertEquals("Repository<Integer>", bean.integerRepository.toString());
		assertTrue(AopUtils.isCglibProxy(bean.stringRepository));
		assertTrue(AopUtils.isCglibProxy(bean.integerRepository));
	}

	@Test
	public void genericsBasedInjectionWithImplTypeAtInjectionPoint() {
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
	public void genericsBasedInjectionWithFactoryBean() {
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
	public void genericsBasedInjectionWithRawMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawMatchingConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertSame(beanFactory.getBean("rawRepo"), beanFactory.getBean("repoConsumer"));
	}

	@Test
	public void genericsBasedInjectionWithWildcardMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardMatchingConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertSame(beanFactory.getBean("genericRepo"), beanFactory.getBean("repoConsumer"));
	}

	@Test
	public void genericsBasedInjectionWithWildcardWithExtendsMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardWithExtendsConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertSame(beanFactory.getBean("stringRepo"), beanFactory.getBean("repoConsumer"));
	}

	@Test
	public void genericsBasedInjectionWithWildcardWithGenericExtendsMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardWithGenericExtendsConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertSame(beanFactory.getBean("genericRepo"), beanFactory.getBean("repoConsumer"));
	}

	@Test
	public void genericsBasedInjectionWithLateGenericsMatching() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertTrue(ObjectUtils.containsElement(beanNames, "stringRepo"));

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertEquals(1, beanNames.length);
		assertEquals("stringRepo", beanNames[0]);
	}

	@Test
	public void genericsBasedInjectionWithEarlyGenericsMatching() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertTrue(ObjectUtils.containsElement(beanNames, "stringRepo"));

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertEquals(1, beanNames.length);
		assertEquals("stringRepo", beanNames[0]);
	}

	@Test
	public void testSelfReferenceExclusionForFactoryMethodOnSameBean() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ConcreteConfig.class));
		beanFactory.registerBeanDefinition("serviceBeanProvider", new RootBeanDefinition(ServiceBeanProvider.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		beanFactory.getBean(ServiceBean.class);
	}

	@Test
	public void testConfigWithDefaultMethods() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ConcreteConfigWithDefaultMethods.class));
		beanFactory.registerBeanDefinition("serviceBeanProvider", new RootBeanDefinition(ServiceBeanProvider.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		beanFactory.getBean(ServiceBean.class);
	}

	@Test
	public void testConfigWithDefaultMethodsUsingAsm() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ConcreteConfigWithDefaultMethods.class.getName()));
		beanFactory.registerBeanDefinition("serviceBeanProvider", new RootBeanDefinition(ServiceBeanProvider.class.getName()));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		beanFactory.getBean(ServiceBean.class);
	}

	@Test
	public void testCircularDependency() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.registerBeanDefinition("configClass1", new RootBeanDefinition(A.class));
		beanFactory.registerBeanDefinition("configClass2", new RootBeanDefinition(AStrich.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		try {
			beanFactory.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("Circular reference"));
		}
	}

	@Test
	public void testCircularDependencyWithApplicationContext() {
		try {
			new AnnotationConfigApplicationContext(A.class, AStrich.class);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("Circular reference"));
		}
	}

	@Test
	public void testPrototypeArgumentThroughBeanMethodCall() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithPrototype.class);
		ctx.getBean(FooFactory.class).createFoo(new BarArgument());
	}

	@Test
	public void testSingletonArgumentThroughBeanMethodCall() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithSingleton.class);
		ctx.getBean(FooFactory.class).createFoo(new BarArgument());
	}

	@Test
	public void testNullArgumentThroughBeanMethodCall() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithNull.class);
		ctx.getBean("aFoo");
	}

	@Test
	public void testInjectionPointMatchForNarrowTargetReturnType() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(FooBarConfiguration.class);
		assertSame(ctx.getBean(BarImpl.class), ctx.getBean(FooImpl.class).bar);
	}

	@Test
	public void testCollectionInjectionFromSameConfigurationClass() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(CollectionInjectionConfiguration.class);
		CollectionInjectionConfiguration bean = ctx.getBean(CollectionInjectionConfiguration.class);
		assertNotNull(bean.testBeans);
		assertEquals(1, bean.testBeans.size());
		assertSame(ctx.getBean(TestBean.class), bean.testBeans.get(0));
	}


	// -------------------------------------------------------------------------

	@Configuration
	@Order(1)
	static class SingletonBeanConfig {

		public @Bean Foo foo() {
			return new Foo();
		}

		public @Bean Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration
	@Order(2)
	static class OverridingSingletonBeanConfig {

		public @Bean ExtendedFoo foo() {
			return new ExtendedFoo();
		}

		public @Bean Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration
	static class OverridingAgainSingletonBeanConfig {

		public @Bean ExtendedAgainFoo foo() {
			return new ExtendedAgainFoo();
		}
	}

	@Configuration
	static class InvalidOverridingSingletonBeanConfig {

		public @Bean Foo foo() {
			return new Foo();
		}
	}

	static class Foo {
	}

	static class ExtendedFoo extends Foo {
	}

	static class ExtendedAgainFoo extends ExtendedFoo {
	}

	static class Bar {

		final Foo foo;

		public Bar(Foo foo) {
			this.foo = foo;
		}
	}

	@Configuration
	static class UnloadedConfig {

		public @Bean
		Foo foo() {
			return new Foo();
		}
	}

	@Configuration
	static class LoadedConfig {

		public @Bean
		Bar bar() {
			return new Bar(new Foo());
		}
	}

	public static class ScopedProxyConsumer {

		@Autowired
		public ITestBean testBean;
	}

	@Configuration
	public static class ScopedProxyConfigurationClass {

		@Bean
		@Lazy
		@Scope(proxyMode = ScopedProxyMode.INTERFACES)
		public ITestBean scopedClass() {
			return new TestBean();
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
			return new Repository<String>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		public Repository<Integer> integerRepo() {
			return new Repository<Integer>() {
				@Override
				public String toString() {
					return "Repository<Integer>";
				}
			};
		}

		@Bean
		public Repository<?> genericRepo() {
			return new Repository<Object>() {
				@Override
				public String toString() {
					return "Repository<Object>";
				}
			};
		}
	}

	@Configuration
	public static class ScopedRepositoryConfiguration {

		@Bean
		@Scope("prototype")
		public Repository<String> stringRepo() {
			return new Repository<String>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		@Scope("prototype")
		public Repository<Integer> integerRepo() {
			return new Repository<Integer>() {
				@Override
				public String toString() {
					return "Repository<Integer>";
				}
			};
		}

		@Bean
		@Scope("prototype")
		@SuppressWarnings("rawtypes")
		public Repository genericRepo() {
			return new Repository<Object>() {
				@Override
				public String toString() {
					return "Repository<Object>";
				}
			};
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Scope(scopeName = "prototype")
	public @interface PrototypeScoped {

		ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
	}

	@Configuration
	public static class ScopedProxyRepositoryConfiguration {

		@Bean
		@Scope(scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public Repository<String> stringRepo() {
			return new Repository<String>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		@PrototypeScoped
		public Repository<Integer> integerRepo() {
			return new Repository<Integer>() {
				@Override
				public String toString() {
					return "Repository<Integer>";
				}
			};
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
			return new GenericRepository<>();
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
		@SuppressWarnings("rawtypes")
		public Repository rawRepo() {
			return new Repository();
		}

		@Bean
		public Object repoConsumer(Repository<String> repo) {
			return repo;
		}
	}

	@Configuration
	public static class WildcardMatchingConfiguration {

		@Bean
		@SuppressWarnings("rawtypes")
		public Repository<?> genericRepo() {
			return new Repository();
		}

		@Bean
		public Object repoConsumer(Repository<String> repo) {
			return repo;
		}
	}

	@Configuration
	public static class WildcardWithExtendsConfiguration {

		@Bean
		public Repository<? extends String> stringRepo() {
			return new Repository<>();
		}

		@Bean
		public Repository<? extends Number> numberRepo() {
			return new Repository<>();
		}

		@Bean
		public Object repoConsumer(Repository<? extends String> repo) {
			return repo;
		}
	}

	@Configuration
	public static class WildcardWithGenericExtendsConfiguration {

		@Bean
		public Repository<? extends Object> genericRepo() {
			return new Repository<String>();
		}

		@Bean
		public Repository<? extends Number> numberRepo() {
			return new Repository<>();
		}

		@Bean
		public Object repoConsumer(Repository<String> repo) {
			return repo;
		}
	}

	@Configuration
	@ComponentScan(basePackages = "org.springframework.context.annotation.componentscan.simple")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedConfiguration {
	}

	@ComposedConfiguration
	public static class ComposedConfigurationClass {
	}

	@Configuration
	@ComponentScan
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedConfigurationWithAttributeOverrides {

		String[] basePackages() default {};

		ComponentScan.Filter[] excludeFilters() default {};
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple")
	public static class ComposedConfigurationWithAttributeOverrideForBasePackage {
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple",
			excludeFilters = @ComponentScan.Filter(Component.class))
	public static class ComposedConfigurationWithAttributeOverrideForExcludeFilter {
	}

	@ComposedConfigurationWithAttributeOverrides
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedComposedConfigurationWithAttributeOverrides {

		String[] basePackages() default {};
	}

	@ComposedComposedConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple")
	public static class ComposedComposedConfigurationWithAttributeOverridesClass {
	}

	@ComponentScan
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface MetaComponentScan {
	}

	@MetaComponentScan
	@Configuration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface MetaComponentScanConfigurationWithAttributeOverrides {

		String[] basePackages() default {};
	}

	@MetaComponentScanConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple")
	public static class MetaComponentScanConfigurationWithAttributeOverridesClass {
	}

	@Configuration
	public static class SubMetaComponentScanConfigurationWithAttributeOverridesClass extends
			MetaComponentScanConfigurationWithAttributeOverridesClass {
	}

	public static class ServiceBean {

		private final String parameter;

		public ServiceBean(String parameter) {
			this.parameter = parameter;
		}

		public String getParameter() {
			return parameter;
		}
	}

	@Configuration
	public static abstract class AbstractConfig {

		@Bean
		public ServiceBean serviceBean() {
			return provider().getServiceBean();
		}

		@Bean
		public ServiceBeanProvider provider() {
			return new ServiceBeanProvider();
		}
	}

	@Configuration
	public static class ConcreteConfig extends AbstractConfig {

		@Autowired
		private ServiceBeanProvider provider;

		@Bean
		@Override
		public ServiceBeanProvider provider() {
			return provider;
		}

		@PostConstruct
		public void validate() {
			Assert.notNull(provider);
		}
	}

	public interface BaseInterface {

		ServiceBean serviceBean();
	}

	public interface BaseDefaultMethods extends BaseInterface {

		@Bean
		default ServiceBeanProvider provider() {
			return new ServiceBeanProvider();
		}

		@Bean
		@Override
		default ServiceBean serviceBean() {
			return provider().getServiceBean();
		}
	}

	public interface DefaultMethodsConfig extends BaseDefaultMethods {

	}

	@Configuration
	public static class ConcreteConfigWithDefaultMethods implements DefaultMethodsConfig {

		@Autowired
		private ServiceBeanProvider provider;

		@Bean
		@Override
		public ServiceBeanProvider provider() {
			return provider;
		}

		@PostConstruct
		public void validate() {
			Assert.notNull(provider);
		}
	}

	@Primary
	public static class ServiceBeanProvider {

		public ServiceBean getServiceBean() {
			return new ServiceBean("message");
		}
	}

	@Configuration
	public static class A {

		@Autowired(required = true)
		Z z;

		@Bean
		public B b() {
			if (z == null) {
				throw new NullPointerException("z is null");
			}
			return new B(z);
		}
	}

	@Configuration
	public static class AStrich {

		@Autowired
		B b;

		@Bean
		public Z z() {
			return new Z();
		}
	}

	public static class B {

		public B(Z z) {
		}
	}

	public static class Z {
	}

	@Configuration
	static class BeanArgumentConfigWithPrototype {

		@Bean
		@Scope("prototype")
		public DependingFoo foo(BarArgument bar) {
			return new DependingFoo(bar);
		}

		@Bean
		public FooFactory fooFactory() {
			return new FooFactory() {
				@Override
				public DependingFoo createFoo(BarArgument bar) {
					return foo(bar);
				}
			};
		}
	}

	@Configuration
	static class BeanArgumentConfigWithSingleton {

		@Bean @Lazy
		public DependingFoo foo(BarArgument bar) {
			return new DependingFoo(bar);
		}

		@Bean
		public FooFactory fooFactory() {
			return new FooFactory() {
				@Override
				public DependingFoo createFoo(BarArgument bar) {
					return foo(bar);
				}
			};
		}
	}

	@Configuration
	static class BeanArgumentConfigWithNull {

		@Bean
		public DependingFoo aFoo() {
			return foo(null);
		}

		@Bean @Lazy
		public DependingFoo foo(BarArgument bar) {
			return new DependingFoo(bar);
		}

		@Bean
		public BarArgument bar() {
			return new BarArgument();
		}
	}

	static class BarArgument {
	}

	static class DependingFoo {

		DependingFoo(BarArgument bar) {
			Assert.notNull(bar);
		}
	}

	static abstract class FooFactory {

		abstract DependingFoo createFoo(BarArgument bar);
	}

	interface BarInterface {
	}

	static class BarImpl implements BarInterface {
	}

	static class FooImpl {

		@Autowired
		public BarImpl bar;
	}

	@Configuration
	static class FooBarConfiguration {

		@Bean
		public BarInterface bar() {
			return new BarImpl();
		}

		@Bean
		public FooImpl foo() {
			return new FooImpl();
		}
	}

	@Configuration
	static class CollectionInjectionConfiguration {

		@Autowired(required = false)
		public List<TestBean> testBeans;

		@Bean
		public TestBean thing() {
			return new TestBean();
		}
	}

}
