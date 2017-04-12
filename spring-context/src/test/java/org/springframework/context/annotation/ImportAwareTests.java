/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests that an ImportAware @Configuration classes gets injected with the
 * annotation metadata of the @Configuration class that imported it.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ImportAwareTests {

	@Test
	public void directlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingConfig.class);
		ctx.refresh();
		assertNotNull(ctx.getBean("importedConfigBean"));

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat("import metadata was not injected", importMetadata, notNullValue());
		assertThat(importMetadata.getClassName(), is(ImportingConfig.class.getName()));
		AnnotationAttributes importAttribs = AnnotationConfigUtils.attributesFor(importMetadata, Import.class);
		Class<?>[] importedClasses = importAttribs.getClassArray("value");
		assertThat(importedClasses[0].getName(), is(ImportedConfig.class.getName()));
	}

	@Test
	public void indirectlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(IndirectlyImportingConfig.class);
		ctx.refresh();
		assertNotNull(ctx.getBean("importedConfigBean"));

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat("import metadata was not injected", importMetadata, notNullValue());
		assertThat(importMetadata.getClassName(), is(IndirectlyImportingConfig.class.getName()));
		AnnotationAttributes enableAttribs = AnnotationConfigUtils.attributesFor(importMetadata, EnableImportedConfig.class);
		String foo = enableAttribs.getString("foo");
		assertThat(foo, is("xyz"));
	}

	@Test
	public void importRegistrar() throws Exception {
		ImportedRegistrar.called = false;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingRegistrarConfig.class);
		ctx.refresh();
		assertNotNull(ctx.getBean("registrarImportedBean"));
		assertNotNull(ctx.getBean("otherImportedConfigBean"));
	}

	@Test
	public void importRegistrarWithImport() throws Exception {
		ImportedRegistrar.called = false;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingRegistrarConfigWithImport.class);
		ctx.refresh();
		assertNotNull(ctx.getBean("registrarImportedBean"));
		assertNotNull(ctx.getBean("otherImportedConfigBean"));
		assertNotNull(ctx.getBean("importedConfigBean"));
		assertNotNull(ctx.getBean(ImportedConfig.class));
	}

	@Test
	public void metadataFromImportsOneThenTwo() {
		AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
				ConfigurationOne.class, ConfigurationTwo.class)
				.getBean(MetadataHolder.class).importMetadata;
		assertEquals(ConfigurationOne.class,
				((StandardAnnotationMetadata) importMetadata).getIntrospectedClass());
	}

	@Test
	public void metadataFromImportsTwoThenOne() {
		AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
				ConfigurationTwo.class, ConfigurationOne.class)
				.getBean(MetadataHolder.class).importMetadata;
		assertEquals(ConfigurationOne.class,
				((StandardAnnotationMetadata) importMetadata).getIntrospectedClass());
	}


	@Configuration
	@Import(ImportedConfig.class)
	static class ImportingConfig {
	}


	@Configuration
	@EnableImportedConfig(foo="xyz")
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

}
