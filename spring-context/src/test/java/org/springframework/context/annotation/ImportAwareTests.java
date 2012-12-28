/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.util.Assert;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.context.annotation.MetadataUtils.*;

/**
 * Tests that an ImportAware @Configuration classes gets injected with the
 * annotation metadata of the @Configuration class that imported it.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ImportAwareTests {

	@Test
	public void directlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingConfig.class);
		ctx.refresh();

		ctx.getBean("importedConfigBean");

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat("import metadata was not injected", importMetadata, notNullValue());
		assertThat(importMetadata.getClassName(), is(ImportingConfig.class.getName()));
		AnnotationAttributes importAttribs = attributesFor(importMetadata, Import.class);
		Class<?>[] importedClasses = importAttribs.getClassArray("value");
		assertThat(importedClasses[0].getName(), is(ImportedConfig.class.getName()));
	}

	@Test
	public void indirectlyAnnotatedWithImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(IndirectlyImportingConfig.class);
		ctx.refresh();

		ctx.getBean("importedConfigBean");

		ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
		AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
		assertThat("import metadata was not injected", importMetadata, notNullValue());
		assertThat(importMetadata.getClassName(), is(IndirectlyImportingConfig.class.getName()));
		AnnotationAttributes enableAttribs = attributesFor(importMetadata, EnableImportedConfig.class);
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
	}

	@Test
	public void importRegistrarWithImport() throws Exception {
		ImportedRegistrar.called = false;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportingRegistrarConfigWithImport.class);
		ctx.refresh();
		assertNotNull(ctx.getBean("registrarImportedBean"));
		assertNotNull(ctx.getBean(ImportedConfig.class));
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


	static class BPP implements BeanFactoryAware, BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
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
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			BeanDefinition beanDefinition = new GenericBeanDefinition();
			beanDefinition.setBeanClassName(String.class.getName());
			registry.registerBeanDefinition("registrarImportedBean", beanDefinition );
			Assert.state(called == false, "ImportedRegistrar called twice");
			called = true;
		}
	}
}
