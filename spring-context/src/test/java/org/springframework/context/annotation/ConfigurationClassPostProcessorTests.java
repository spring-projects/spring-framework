/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class ConfigurationClassPostProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	void setup() {
		QualifierAnnotationAutowireCandidateResolver acr = new QualifierAnnotationAutowireCandidateResolver();
		acr.setBeanFactory(this.beanFactory);
		this.beanFactory.setAutowireCandidateResolver(acr);
	}


	/**
	 * Enhanced {@link Configuration} classes are only necessary for respecting
	 * certain bean semantics, like singleton-scoping, scoped proxies, etc.
	 * <p>Technically, {@link ConfigurationClassPostProcessor} could fail to enhance the
	 * registered Configuration classes and many use cases would still work.
	 * Certain cases, however, like inter-bean singleton references would not.
	 * We test for such a case below, and in doing so prove that enhancement is working.
	 */
	@Test
	void enhancementIsPresentBecauseSingletonSemanticsAreRespected() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).contains(ClassUtils.CGLIB_CLASS_SEPARATOR);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
		assertThat(beanFactory.getDependentBeans("foo")).contains("bar");
		assertThat(beanFactory.getDependentBeans("config")).contains("foo");
		assertThat(beanFactory.getDependentBeans("config")).contains("bar");
	}

	@Test
	void enhancementIsPresentBecauseSingletonSemanticsAreRespectedUsingAsm() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).contains(ClassUtils.CGLIB_CLASS_SEPARATOR);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
		assertThat(beanFactory.getDependentBeans("foo")).contains("bar");
		assertThat(beanFactory.getDependentBeans("config")).contains("foo");
		assertThat(beanFactory.getDependentBeans("config")).contains("bar");
	}

	@Test  // gh-34663
	void enhancementIsPresentForAbstractConfigClassWithoutBeanMethods() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(AbstractConfigWithoutBeanMethods.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		RootBeanDefinition beanDefinition = (RootBeanDefinition) beanFactory.getBeanDefinition("config");
		assertThat(beanDefinition.hasBeanClass()).isTrue();
		assertThat(beanDefinition.getBeanClass().getName()).contains(ClassUtils.CGLIB_CLASS_SEPARATOR);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
		assertThat(beanFactory.getDependentBeans("foo")).contains("bar");
		String[] dependentsOfSingletonBeanConfig = beanFactory.getDependentBeans(SingletonBeanConfig.class.getName());
		assertThat(dependentsOfSingletonBeanConfig).containsOnly("foo", "bar");
	}

	@Test
	void enhancementIsNotPresentForProxyBeanMethodsFlagSetToFalse() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(NonEnhancedSingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isNotSameAs(foo);
	}

	@Test
	void enhancementIsNotPresentForProxyBeanMethodsFlagSetToFalseUsingAsm() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(NonEnhancedSingletonBeanConfig.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isNotSameAs(foo);
	}

	@Test
	void enhancementIsNotPresentForStaticMethods() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(StaticSingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("foo")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("bar")).hasBeanClass()).isTrue();
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isNotSameAs(foo);
	}

	@Test
	void enhancementIsNotPresentForStaticMethodsUsingAsm() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(StaticSingletonBeanConfig.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("foo")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("bar")).hasBeanClass()).isTrue();
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isNotSameAs(foo);
	}

	@Test  // gh-34486
	void enhancementIsNotPresentWithEmptyConfig() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(EmptyConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
		assertThat(((RootBeanDefinition) beanFactory.getBeanDefinition("config")).getBeanClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);
	}

	@Test
	void configurationIntrospectionOfInnerClassesWorksWithDotNameSyntax() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(getClass().getName() + ".SingletonBeanConfig"));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	/**
	 * Tests the fix for SPR-5655, a special workaround that prefers reflection over ASM
	 * if a bean class is already loaded.
	 */
	@Test
	void alreadyLoadedConfigurationClasses() {
		beanFactory.registerBeanDefinition("unloadedConfig", new RootBeanDefinition(UnloadedConfig.class.getName()));
		beanFactory.registerBeanDefinition("loadedConfig", new RootBeanDefinition(LoadedConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("foo");
		beanFactory.getBean("bar");
	}

	/**
	 * Tests whether a bean definition without a specified bean class is handled correctly.
	 */
	@Test
	void postProcessorIntrospectsInheritedDefinitionsCorrectly() {
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("parent", new RootBeanDefinition(TestBean.class));
		beanFactory.registerBeanDefinition("child", new ChildBeanDefinition("parent"));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ComposedConfigurationClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ComposedConfigurationClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForBasePackage.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForBasePackage.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForExcludeFilter.class);
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedConfigurationWithAttributeOverrideForExcludeFilter.class.getName());
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	void postProcessorWorksWithExtendedConfigurationWithAttributeOverrideForExcludesFilterUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ExtendedConfigurationWithAttributeOverrideForExcludeFilter.class);
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	void postProcessorWorksWithExtendedConfigurationWithAttributeOverrideForExcludesFilterUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ExtendedConfigurationWithAttributeOverrideForExcludeFilter.class.getName());
		assertSupportForComposedAnnotationWithExclude(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedComposedConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ComposedComposedConfigurationWithAttributeOverridesClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				MetaComponentScanConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingAsm() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				MetaComponentScanConfigurationWithAttributeOverridesClass.class.getName());
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingReflection() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				SubMetaComponentScanConfigurationWithAttributeOverridesClass.class);
		assertSupportForComposedAnnotation(beanDefinition);
	}

	@Test
	void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingAsm() {
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
		assertThat(simpleComponent).isNotNull();
	}

	private void assertSupportForComposedAnnotationWithExclude(RootBeanDefinition beanDefinition) {
		beanFactory.registerBeanDefinition("config", beanDefinition);
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.setEnvironment(new StandardEnvironment());
		pp.postProcessBeanFactory(beanFactory);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> beanFactory.getBean(SimpleComponent.class));
	}

	@Test
	void postProcessorOverridesNonApplicationBeanDefinitions() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setRole(RootBeanDefinition.ROLE_SUPPORT);
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		Foo foo = beanFactory.getBean("foo", Foo.class);
		Bar bar = beanFactory.getBean("bar", Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test
	void postProcessorDoesNotOverrideRegularBeanDefinitions() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
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
	void postProcessorDoesNotOverrideRegularBeanDefinitionsEvenWithScopedProxy() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
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
	void postProcessorFailsOnImplicitOverrideIfOverridingIsNotAllowed() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setResource(new DescriptiveResource("XML or something"));
		beanFactory.registerBeanDefinition("bar", rbd);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(SingletonBeanConfig.class));
		beanFactory.setAllowBeanDefinitionOverriding(false);
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> pp.postProcessBeanFactory(beanFactory))
				.withMessageContaining("bar")
				.withMessageContaining("SingletonBeanConfig")
				.withMessageContaining(TestBean.class.getName());
	}

	@Test  // gh-25430
	void detectAliasOverride() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
		beanFactory.setAllowBeanDefinitionOverriding(false);
		context.register(FirstConfiguration.class, SecondConfiguration.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
				.withMessageContaining("alias 'taskExecutor'")
				.withMessageContaining("name 'applicationTaskExecutor'")
				.withMessageContaining("bean definition 'taskExecutor'");
		context.close();
	}

	@Test
	void configurationClassesProcessedInCorrectOrder() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		Foo foo = beanFactory.getBean(Foo.class);
		boolean condition = foo instanceof ExtendedFoo;
		assertThat(condition).isTrue();
		Bar bar = beanFactory.getBean(Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test
	void configurationClassesWithValidOverridingForProgrammaticCall() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(OverridingAgainSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config3", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		Foo foo = beanFactory.getBean(Foo.class);
		boolean condition = foo instanceof ExtendedAgainFoo;
		assertThat(condition).isTrue();
		Bar bar = beanFactory.getBean(Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test
	void configurationClassesWithInvalidOverridingForProgrammaticCall() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config1", new RootBeanDefinition(InvalidOverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config2", new RootBeanDefinition(OverridingSingletonBeanConfig.class));
		beanFactory.registerBeanDefinition("config3", new RootBeanDefinition(SingletonBeanConfig.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> beanFactory.getBean(Bar.class))
				.withMessageContaining("OverridingSingletonBeanConfig.foo")
				.withMessageContaining(ExtendedFoo.class.getName())
				.withMessageContaining(Foo.class.getName())
				.withMessageContaining("InvalidOverridingSingletonBeanConfig");
	}

	@Test  // SPR-15384
	void nestedConfigurationClassesProcessedInCorrectOrder() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithOrderedNestedClasses.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		Foo foo = beanFactory.getBean(Foo.class);
		boolean condition = foo instanceof ExtendedFoo;
		assertThat(condition).isTrue();
		Bar bar = beanFactory.getBean(Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test  // SPR-16734
	void innerConfigurationClassesProcessedInCorrectOrder() {
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithOrderedInnerClasses.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());

		Foo foo = beanFactory.getBean(Foo.class);
		boolean condition = foo instanceof ExtendedFoo;
		assertThat(condition).isTrue();
		Bar bar = beanFactory.getBean(Bar.class);
		assertThat(bar.foo).isSameAs(foo);
	}

	@Test
	void scopedProxyTargetMarkedAsNonAutowireCandidate() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ScopedProxyConfigurationClass.class));
		beanFactory.registerBeanDefinition("consumer", new RootBeanDefinition(ScopedProxyConsumer.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		ITestBean injected = beanFactory.getBean("consumer", ScopedProxyConsumer.class).testBean;
		boolean condition = injected instanceof ScopedObject;
		assertThat(condition).isTrue();
		assertThat(injected).isSameAs(beanFactory.getBean("scopedClass"));
		assertThat(injected).isSameAs(beanFactory.getBean(ITestBean.class));
	}

	@Test
	void processingAllowedOnlyOncePerProcessorRegistryPair() {
		DefaultListableBeanFactory bf1 = new DefaultListableBeanFactory();
		DefaultListableBeanFactory bf2 = new DefaultListableBeanFactory();
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(bf1); // first invocation -- should succeed
		assertThatIllegalStateException().isThrownBy(() ->
				pp.postProcessBeanFactory(bf1)); // second invocation for bf1 -- should throw
		pp.postProcessBeanFactory(bf2); // first invocation for bf2 -- should succeed
		assertThatIllegalStateException().isThrownBy(() ->
				pp.postProcessBeanFactory(bf2)); // second invocation for bf2 -- should throw
	}

	@Test
	void genericsBasedInjection() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
		assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
	}

	@Test
	void genericsBasedInjectionWithScoped() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
		assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
	}

	@Test
	void genericsBasedInjectionWithScopedProxy() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedProxyRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.freezeConfiguration();

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
		assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
		assertThat(AopUtils.isCglibProxy(bean.stringRepository)).isTrue();
		assertThat(AopUtils.isCglibProxy(bean.integerRepository)).isTrue();
	}

	@Test
	void genericsBasedInjectionWithScopedProxyUsingAsm() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryInjectionBean.class.getName());
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ScopedProxyRepositoryConfiguration.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.freezeConfiguration();

		RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
		assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
		assertThat(AopUtils.isCglibProxy(bean.stringRepository)).isTrue();
		assertThat(AopUtils.isCglibProxy(bean.integerRepository)).isTrue();
	}

	@Test
	void genericsBasedInjectionWithImplTypeAtInjectionPoint() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(SpecificRepositoryInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(SpecificRepositoryConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		SpecificRepositoryInjectionBean bean = (SpecificRepositoryInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.genericRepository).isSameAs(beanFactory.getBean("genericRepo"));
	}

	@Test
	void genericsBasedInjectionWithFactoryBean() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(RepositoryFactoryBeanInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("annotatedBean", bd);
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryFactoryBeanConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		RepositoryFactoryBeanInjectionBean bean = (RepositoryFactoryBeanInjectionBean) beanFactory.getBean("annotatedBean");
		assertThat(bean.repositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
		assertThat(bean.qualifiedRepositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
		assertThat(bean.prefixQualifiedRepositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
	}

	@Test
	void genericsBasedInjectionWithRawMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawMatchingConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("rawRepo"));
	}

	@Test
	void genericsBasedInjectionWithWildcardMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardMatchingConfiguration.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("genericRepo"));
	}

	@Test
	void genericsBasedInjectionWithWildcardWithExtendsMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardWithExtendsConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("stringRepo"));
	}

	@Test
	void genericsBasedInjectionWithWildcardWithGenericExtendsMatch() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(WildcardWithGenericExtendsConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("genericRepo"));
	}

	@Test
	void genericsBasedInjectionWithEarlyGenericsMatching() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatching() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");
	}

	@Test
	void genericsBasedInjectionWithEarlyGenericsMatchingAndRawFactoryMethod() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).isEmpty();

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).isEmpty();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingAndRawFactoryMethod() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");
	}

	@Test
	void genericsBasedInjectionWithEarlyGenericsMatchingAndRawInstance() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawInstanceRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingAndRawInstance() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawInstanceRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");
	}

	@Test
	void genericsBasedInjectionWithEarlyGenericsMatchingOnCglibProxy() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setProxyTargetClass(true);
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxy() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setProxyTargetClass(true);
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxyAndRawFactoryMethod() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setProxyTargetClass(true);
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxyAndRawInstance() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawInstanceRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setProxyTargetClass(true);
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(Repository.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(Repository.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithEarlyGenericsMatchingOnJdkProxy() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));

		String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxy() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxyAndRawFactoryMethod() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxyAndRawInstance() {
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(RawInstanceRepositoryConfiguration.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(autoProxyCreator);
		beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		beanFactory.preInstantiateSingletons();

		String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class);
		assertThat(beanNames).contains("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		beanNames = beanFactory.getBeanNamesForType(ResolvableType.forClassWithGenerics(RepositoryInterface.class, String.class));
		assertThat(beanNames).hasSize(1);
		assertThat(beanNames[0]).isEqualTo("stringRepo");

		assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
	}

	@Test
	void testSelfReferenceExclusionForFactoryMethodOnSameBean() {
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
	void testConfigWithDefaultMethods() {
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
	void testConfigWithDefaultMethodsUsingAsm() {
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
	void testConfigWithFailingInit() {  // gh-23343
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		beanFactory.registerBeanDefinition("configClass", new RootBeanDefinition(ConcreteConfigWithFailingInit.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(beanFactory::preInstantiateSingletons);
		assertThat(beanFactory.containsSingleton("configClass")).isFalse();
		assertThat(beanFactory.containsSingleton("provider")).isFalse();
	}

	@Test
	void testCircularDependency() {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(bpp);
		beanFactory.registerBeanDefinition("configClass1", new RootBeanDefinition(A.class));
		beanFactory.registerBeanDefinition("configClass2", new RootBeanDefinition(AStrich.class));
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(beanFactory::preInstantiateSingletons)
				.withMessageContaining("Circular reference");
	}

	@Test
	void testCircularDependencyWithApplicationContext() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(A.class, AStrich.class))
				.withMessageContaining("Circular reference");
	}

	@Test
	void testPrototypeArgumentThroughBeanMethodCall() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithPrototype.class);
		ctx.getBean(FooFactory.class).createFoo(new BarArgument());
		ctx.close();
	}

	@Test
	void testSingletonArgumentThroughBeanMethodCall() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithSingleton.class);
		ctx.getBean(FooFactory.class).createFoo(new BarArgument());
		ctx.close();
	}

	@Test
	void testNullArgumentThroughBeanMethodCall() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BeanArgumentConfigWithNull.class);
		ctx.getBean("aFoo");
		ctx.close();
	}

	@Test
	void testInjectionPointMatchForNarrowTargetReturnType() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(FooBarConfiguration.class);
		assertThat(ctx.getBean(FooImpl.class).bar).isSameAs(ctx.getBean(BarImpl.class));
		ctx.close();
	}

	@Test
	void testVarargOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(VarargConfiguration.class, TestBean.class);
		VarargConfiguration bean = ctx.getBean(VarargConfiguration.class);
		assertThat(bean.testBeans).isNotNull();
		assertThat(bean.testBeans).hasSize(1);
		assertThat(bean.testBeans[0]).isSameAs(ctx.getBean(TestBean.class));
		ctx.close();
	}

	@Test
	void testEmptyVarargOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(VarargConfiguration.class);
		VarargConfiguration bean = ctx.getBean(VarargConfiguration.class);
		assertThat(bean.testBeans).isNotNull();
		assertThat(bean.testBeans).isEmpty();
		ctx.close();
	}

	@Test
	void testCollectionArgumentOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CollectionArgumentConfiguration.class, TestBean.class);
		CollectionArgumentConfiguration bean = ctx.getBean(CollectionArgumentConfiguration.class);
		assertThat(bean.testBeans).containsExactly(ctx.getBean(TestBean.class));
		ctx.close();
	}

	@Test
	void testEmptyCollectionArgumentOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CollectionArgumentConfiguration.class);
		CollectionArgumentConfiguration bean = ctx.getBean(CollectionArgumentConfiguration.class);
		assertThat(bean.testBeans).isEmpty();
		ctx.close();
	}

	@Test
	void testMapArgumentOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MapArgumentConfiguration.class, DummyRunnable.class);
		MapArgumentConfiguration bean = ctx.getBean(MapArgumentConfiguration.class);
		assertThat(bean.testBeans).hasSize(1).containsValue(ctx.getBean(Runnable.class));
		ctx.close();
	}

	@Test
	void testEmptyMapArgumentOnBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MapArgumentConfiguration.class);
		MapArgumentConfiguration bean = ctx.getBean(MapArgumentConfiguration.class);
		assertThat(bean.testBeans).isEmpty();
		ctx.close();
	}

	@Test
	void testCollectionInjectionFromSameConfigurationClass() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(CollectionInjectionConfiguration.class);
		CollectionInjectionConfiguration bean = ctx.getBean(CollectionInjectionConfiguration.class);
		assertThat(bean.testBeans).containsExactly(ctx.getBean(TestBean.class));
		ctx.close();
	}

	@Test
	void testMapInjectionFromSameConfigurationClass() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MapInjectionConfiguration.class);
		MapInjectionConfiguration bean = ctx.getBean(MapInjectionConfiguration.class);
		assertThat(bean.testBeans).containsOnly(Map.entry("testBean", ctx.getBean(Runnable.class)));
		ctx.close();
	}

	@Test
	void testBeanLookupFromSameConfigurationClass() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BeanLookupConfiguration.class);
		assertThat(ctx.getBean(BeanLookupConfiguration.class).getTestBean()).isSameAs(ctx.getBean(TestBean.class));
		ctx.close();
	}

	@Test
	void testNameClashBetweenConfigurationClassAndBean() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(MyTestBean.class).getBean("myTestBean", TestBean.class));
	}

	@Test
	void testBeanDefinitionRegistryPostProcessorConfig() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BeanDefinitionRegistryPostProcessorConfig.class);
		assertThat(ctx.getBean("myTestBean")).isInstanceOf(TestBean.class);
		ctx.close();
	}


	// -------------------------------------------------------------------------

	@Configuration
	@Order(1)
	static class SingletonBeanConfig {

		@Bean public Foo foo() {
			return new Foo();
		}

		@Bean public Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class NonEnhancedSingletonBeanConfig {

		@Bean public Foo foo() {
			return new Foo();
		}

		@Bean public Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration
	static final class StaticSingletonBeanConfig {

		@Bean
		public static Foo foo() {
			return new Foo();
		}

		@Bean
		public static Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration
	@Import(SingletonBeanConfig.class)
	abstract static class AbstractConfigWithoutBeanMethods {
		// This class intentionally does NOT declare @Bean methods.
	}

	@Configuration
	static final class EmptyConfig {
	}

	@Configuration
	@Order(2)
	static class OverridingSingletonBeanConfig {

		@Bean public ExtendedFoo foo() {
			return new ExtendedFoo();
		}

		@Bean public Bar bar() {
			return new Bar(foo());
		}
	}

	@Configuration
	static class OverridingAgainSingletonBeanConfig {

		@Bean public ExtendedAgainFoo foo() {
			return new ExtendedAgainFoo();
		}
	}

	@Configuration
	static class InvalidOverridingSingletonBeanConfig {

		@Bean public Foo foo() {
			return new Foo();
		}
	}

	@Configuration
	static class ConfigWithOrderedNestedClasses {

		@Configuration
		@Order(1)
		static class SingletonBeanConfig {

			@Bean public Foo foo() {
				return new Foo();
			}

			@Bean public Bar bar() {
				return new Bar(foo());
			}
		}

		@Configuration
		@Order(2)
		static class OverridingSingletonBeanConfig {

			@Bean public ExtendedFoo foo() {
				return new ExtendedFoo();
			}

			@Bean public Bar bar() {
				return new Bar(foo());
			}
		}
	}

	@Configuration
	static class ConfigWithOrderedInnerClasses {

		@Configuration
		@Order(1)
		class SingletonBeanConfig {

			public SingletonBeanConfig(ConfigWithOrderedInnerClasses other) {
			}

			@Bean public Foo foo() {
				return new Foo();
			}

			@Bean public Bar bar() {
				return new Bar(foo());
			}
		}

		@Configuration
		@Order(2)
		class OverridingSingletonBeanConfig {

			public OverridingSingletonBeanConfig(ObjectProvider<SingletonBeanConfig> other) {
				other.getObject();
			}

			@Bean public ExtendedFoo foo() {
				return new ExtendedFoo();
			}

			@Bean public Bar bar() {
				return new Bar(foo());
			}
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

		@Bean public Foo foo() {
			return new Foo();
		}
	}

	@Configuration
	static class LoadedConfig {

		@Bean public Bar bar() {
			return new Bar(new Foo());
		}
	}

	@Configuration
	static class FirstConfiguration {

		@Bean
		SyncTaskExecutor taskExecutor() {
			return new SyncTaskExecutor();
		}
	}

	@Configuration
	static class SecondConfiguration {

		@Bean(name = {"applicationTaskExecutor", "taskExecutor"})
		SimpleAsyncTaskExecutor simpleAsyncTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}
	}

	public static class ScopedProxyConsumer {

		@Autowired
		public ITestBean testBean;
	}

	@Configuration
	public static class ScopedProxyConfigurationClass {

		@Bean @Lazy @Scope(proxyMode = ScopedProxyMode.INTERFACES)
		public ITestBean scopedClass() {
			return new TestBean();
		}
	}

	public interface RepositoryInterface<T> {

		@Override
		String toString();
	}

	public static class Repository<T> implements RepositoryInterface<T> {
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
			return new Repository<>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		public Repository<Integer> integerRepo() {
			return new Repository<>() {
				@Override
				public String toString() {
					return "Repository<Integer>";
				}
			};
		}

		@Bean
		public Repository<?> genericRepo() {
			return new Repository<>() {
				@Override
				public String toString() {
					return "Repository<Object>";
				}
			};
		}
	}

	@Configuration
	public static class RawFactoryMethodRepositoryConfiguration {

		@SuppressWarnings("rawtypes") // intentionally a raw type
		@Bean
		public Repository stringRepo() {
			return new Repository<String>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}
	}

	@Configuration
	public static class RawInstanceRepositoryConfiguration {

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Bean
		public Repository<String> stringRepo() {
			return new Repository() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}
	}

	@Configuration
	public static class ScopedRepositoryConfiguration {

		@Bean
		@Scope("prototype")
		public Repository<String> stringRepo() {
			return new Repository<>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		@Scope("prototype")
		public Repository<Integer> integerRepo() {
			return new Repository<>() {
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
			return new Repository<>() {
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

		@AliasFor(annotation = Scope.class)
		ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
	}

	@Configuration
	public static class ScopedProxyRepositoryConfiguration {

		@Bean
		@Scope(scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public Repository<String> stringRepo() {
			return new Repository<>() {
				@Override
				public String toString() {
					return "Repository<String>";
				}
			};
		}

		@Bean
		@PrototypeScoped
		public Repository<Integer> integerRepo() {
			return new Repository<>() {
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

		@Bean
		public FactoryBean<Object> nullFactoryBean() {
			return null;
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
		public Repository<?> genericRepo() {
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

		@AliasFor(annotation = ComponentScan.class)
		String[] basePackages() default {};

		@AliasFor(annotation = ComponentScan.class)
		ComponentScan.Filter[] excludeFilters() default {};
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple")
	public static class ComposedConfigurationWithAttributeOverrideForBasePackage {
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.springframework.context.annotation.componentscan.simple",
			excludeFilters = @ComponentScan.Filter(Component.class))
	public static class ComposedConfigurationWithAttributeOverrideForExcludeFilter {
	}

	@ComponentScan(basePackages = "org.springframework.context.annotation.componentscan.base", excludeFilters = {})
	public static class BaseConfigurationWithEmptyExcludeFilters {
	}

	@ComponentScan(basePackages = "org.springframework.context.annotation.componentscan.simple",
			excludeFilters = @ComponentScan.Filter(Component.class))
	public static class ExtendedConfigurationWithAttributeOverrideForExcludeFilter extends BaseConfigurationWithEmptyExcludeFilters {
	}

	@ComposedConfigurationWithAttributeOverrides
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedComposedConfigurationWithAttributeOverrides {

		@AliasFor(annotation = ComposedConfigurationWithAttributeOverrides.class)
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

		@AliasFor(annotation = ComponentScan.class)
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
	public abstract static class AbstractConfig {

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
			Assert.notNull(provider, "No ServiceBeanProvider injected");
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
			Assert.notNull(provider, "No ServiceBeanProvider injected");
		}
	}

	@Configuration
	public static class ConcreteConfigWithFailingInit implements DefaultMethodsConfig, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Bean
		@Override
		public ServiceBeanProvider provider() {
			return new ServiceBeanProvider();
		}

		@PostConstruct
		public void validate() {
			beanFactory.getBean("provider");
			throw new IllegalStateException();
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
			Assert.notNull(bar, "No BarArgument injected");
		}
	}

	abstract static class FooFactory {

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

	public static class DummyRunnable implements Runnable {

		@Override
		public void run() {
			/* no-op */
		}
	}

	@Configuration
	static class VarargConfiguration {

		TestBean[] testBeans;

		@Bean(autowireCandidate = false)
		public TestBean thing(TestBean... testBeans) {
			this.testBeans = testBeans;
			return new TestBean();
		}
	}

	@Configuration
	static class CollectionArgumentConfiguration {

		List<TestBean> testBeans;

		@Bean(autowireCandidate = false)
		public TestBean thing(List<TestBean> testBeans) {
			this.testBeans = testBeans;
			return new TestBean();
		}
	}

	@Configuration
	public static class MapArgumentConfiguration {

		@Autowired
		ConfigurableEnvironment env;

		Map<String, Runnable> testBeans;

		@Bean(autowireCandidate = false)
		Runnable testBean(Map<String, Runnable> testBeans,
				@Qualifier("systemProperties") Map<String, String> sysprops,
				@Qualifier("systemEnvironment") Map<String, String> sysenv) {
			this.testBeans = testBeans;
			assertThat(sysprops).isSameAs(env.getSystemProperties());
			assertThat(sysenv).isSameAs(env.getSystemEnvironment());
			return () -> {};
		}

		// Unrelated, not to be considered as a factory method
		@SuppressWarnings("unused")
		private boolean testBean(boolean param) {
			return param;
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

	@Configuration
	public static class MapInjectionConfiguration {

		@Autowired
		private Map<String, Runnable> testBeans;

		@Bean
		Runnable testBean() {
			return () -> {};
		}

		// Unrelated, not to be considered as a factory method
		@SuppressWarnings("unused")
		private boolean testBean(boolean param) {
			return param;
		}
	}

	@Configuration
	abstract static class BeanLookupConfiguration {

		@Bean
		public TestBean thing() {
			return new TestBean();
		}

		@Lookup
		public abstract TestBean getTestBean();
	}

	@Configuration
	static class BeanDefinitionRegistryPostProcessorConfig {

		@Bean
		public static BeanDefinitionRegistryPostProcessor bdrpp() {
			return new BeanDefinitionRegistryPostProcessor() {
				@Override
				public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
					registry.registerBeanDefinition("myTestBean", new RootBeanDefinition(TestBean.class));
				}
				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
				}
			};
		}
	}

}
