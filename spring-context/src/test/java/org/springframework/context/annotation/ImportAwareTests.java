/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that an ImportAware @Configuration class gets injected with the
 * annotation metadata of the @Configuration class that imported it.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class ImportAwareTests {

	@Test
	@SuppressWarnings("resource")
	void directlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean("importedConfigBean")).isNotNull();

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat(importMetadata).isNotNull();
		assertThat(importMetadata.getClassName()).isEqualTo(ImportingConfig.class.getName());
		AnnotationAttributes importAttribs = AnnotationConfigUtils.attributesFor(importMetadata, Import.class);
		Class<?>[] importedClasses = importAttribs.getClassArray("value");
		assertThat(importedClasses[0].getName()).isEqualTo(ImportedConfig.class.getName());
	}

	@Test
	@SuppressWarnings("resource")
	void indirectlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(IndirectlyImportingConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean("importedConfigBean")).isNotNull();

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat(importMetadata).isNotNull();
		assertThat(importMetadata.getClassName()).isEqualTo(IndirectlyImportingConfig.class.getName());
		AnnotationAttributes enableAttribs = AnnotationConfigUtils.attributesFor(importMetadata, EnableImportedConfig.class);
		String foo = enableAttribs.getString("foo");
		assertThat(foo).isEqualTo("xyz");
	}

	@Test
	@SuppressWarnings("resource")
	void directlyAnnotatedWithImportLite() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingConfigLite.class);
		ctx.refresh();
		assertThat(ctx.getBean("importedConfigBean")).isNotNull();

		ImportedConfigLite importAwareConfig = ctx.getBean(ImportedConfigLite.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat(importMetadata).isNotNull();
		assertThat(importMetadata.getClassName()).isEqualTo(ImportingConfigLite.class.getName());
		AnnotationAttributes importAttribs = AnnotationConfigUtils.attributesFor(importMetadata, Import.class);
		Class<?>[] importedClasses = importAttribs.getClassArray("value");
		assertThat(importedClasses[0].getName()).isEqualTo(ImportedConfigLite.class.getName());
	}

	@Test
	@SuppressWarnings("resource")
	void importRegistrar() {
		ImportedRegistrar.called = false;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingRegistrarConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean("registrarImportedBean")).isNotNull();
		assertThat(ctx.getBean("otherImportedConfigBean")).isNotNull();
	}

	@Test
	@SuppressWarnings("resource")
	void importRegistrarWithImport() {
		ImportedRegistrar.called = false;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingRegistrarConfigWithImport.class);
		ctx.refresh();
		assertThat(ctx.getBean("registrarImportedBean")).isNotNull();
		assertThat(ctx.getBean("otherImportedConfigBean")).isNotNull();
		assertThat(ctx.getBean("importedConfigBean")).isNotNull();
		assertThat(ctx.getBean(ImportedConfig.class)).isNotNull();
	}

	@Test
	@SuppressWarnings("resource")
	void metadataFromImportsOneThenTwo() {
		AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
				ConfigurationOne.class, ConfigurationTwo.class)
				.getBean(MetadataHolder.class).importMetadata;
		assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
	}

	@Test
	@SuppressWarnings("resource")
	void metadataFromImportsTwoThenOne() {
		AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
				ConfigurationTwo.class, ConfigurationOne.class)
				.getBean(MetadataHolder.class).importMetadata;
		assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
	}

	@Test
	@SuppressWarnings("resource")
	void metadataFromImportsOneThenThree() {
		AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
				ConfigurationOne.class, ConfigurationThree.class)
				.getBean(MetadataHolder.class).importMetadata;
		assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
	}

	@Test
	@SuppressWarnings("resource")
	void importAwareWithAnnotationAttributes() {
		new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
	}


	@Configuration
	@Import(ImportedConfig.class)
	static class ImportingConfig {
	}


	@Configuration
	@EnableImportedConfig(foo = "xyz")
	static class IndirectlyImportingConfig {
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(ImportedConfig.class)
	public @interface EnableImportedConfig {
		String foo() default "";
	}


	@Configuration
	static class ImportedConfig implements ImportAware {

		AnnotationMetadata importMetadata;

		@Override
		public void setImportMetadata(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}

		@Bean
		public BPP importedConfigBean() {
			return new BPP();
		}

		@Bean
		public AsyncAnnotationBeanPostProcessor asyncBPP() {
			return new AsyncAnnotationBeanPostProcessor();
		}
	}


	@Configuration
	static class OtherImportedConfig {

		@Bean
		public String otherImportedConfigBean() {
			return "";
		}
	}


	@Configuration
	@Import(ImportedConfigLite.class)
	static class ImportingConfigLite {
	}


	@Configuration(proxyBeanMethods = false)
	static class ImportedConfigLite implements ImportAware {

		AnnotationMetadata importMetadata;

		@Override
		public void setImportMetadata(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}

		@Bean
		public BPP importedConfigBean() {
			return new BPP();
		}

		@Bean
		public AsyncAnnotationBeanPostProcessor asyncBPP() {
			return new AsyncAnnotationBeanPostProcessor();
		}
	}


	static class BPP implements BeanPostProcessor, BeanFactoryAware {

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}
	}


	@Configuration
	@EnableImportRegistrar
	static class ImportingRegistrarConfig {
	}


	@Configuration
	@EnableImportRegistrar
	@Import(ImportedConfig.class)
	static class ImportingRegistrarConfigWithImport {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(ImportedRegistrar.class)
	public @interface EnableImportRegistrar {
	}


	static class ImportedRegistrar implements ImportBeanDefinitionRegistrar {

		static boolean called;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
			beanDefinition.setBeanClassName(String.class.getName());
			registry.registerBeanDefinition("registrarImportedBean", beanDefinition);
			GenericBeanDefinition beanDefinition2 = new GenericBeanDefinition();
			beanDefinition2.setBeanClass(OtherImportedConfig.class);
			registry.registerBeanDefinition("registrarImportedConfig", beanDefinition2);
			Assert.state(!called, "ImportedRegistrar called twice");
			called = true;
		}
	}


	@EnableSomeConfiguration("bar")
	@Configuration
	public static class ConfigurationOne {
	}


	@Conditional(OnMissingBeanCondition.class)
	@EnableSomeConfiguration("foo")
	@Configuration
	public static class ConfigurationTwo {
	}


	@Conditional(OnMissingBeanCondition.class)
	@EnableLiteConfiguration("foo")
	@Configuration
	public static class ConfigurationThree {
	}


	@Import(SomeConfiguration.class)
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EnableSomeConfiguration {

		String value() default "";
	}


	@Configuration
	public static class SomeConfiguration implements ImportAware {

		private AnnotationMetadata importMetadata;

		@Override
		public void setImportMetadata(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}

		@Bean
		public MetadataHolder holder() {
			return new MetadataHolder(this.importMetadata);
		}
	}


	@Import(LiteConfiguration.class)
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EnableLiteConfiguration {

		String value() default "";
	}


	@Configuration(proxyBeanMethods = false)
	public static class LiteConfiguration implements ImportAware {

		private AnnotationMetadata importMetadata;

		@Override
		public void setImportMetadata(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}

		@Bean
		public MetadataHolder holder() {
			return new MetadataHolder(this.importMetadata);
		}
	}


	public static class MetadataHolder {

		private final AnnotationMetadata importMetadata;

		public MetadataHolder(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}
	}


	private static final class OnMissingBeanCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return (context.getBeanFactory().getBeanNamesForType(MetadataHolder.class, true, false).length == 0);
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}


	@Configuration
	@EnableFeature(policies = {
			@EnableFeature.FeaturePolicy(name = "one"),
			@EnableFeature.FeaturePolicy(name = "two")
	})
	public static class ApplicationConfiguration {
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(FeatureConfiguration.class)
	public @interface EnableFeature {

		FeaturePolicy[] policies() default {};

		@interface FeaturePolicy {

			String name();
		}
	}


	@Configuration
	public static class FeatureConfiguration implements ImportAware {

		@Override
		public void setImportMetadata(AnnotationMetadata annotationMetadata) {
			AnnotationAttributes enableFeatureAttributes =
					AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableFeature.class.getName()));
			assertThat(enableFeatureAttributes.annotationType()).isEqualTo(EnableFeature.class);
			Arrays.stream(enableFeatureAttributes.getAnnotationArray("policies")).forEach(featurePolicyAttributes -> assertThat(featurePolicyAttributes.annotationType()).isEqualTo(EnableFeature.FeaturePolicy.class));
		}
	}

}
