/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.mockito.InOrder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrarTests.SampleRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ImportSelector} and {@link DeferredImportSelector}.
 *
 * @author Phillip Webb
 */
public class ImportSelectorTests {

	@Test
	public void importSelectors() {
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				beanFactory);
		context.register(Config.class);
		context.refresh();
		context.getBean(Config.class);
		InOrder ordered = inOrder(beanFactory);
		ordered.verify(beanFactory).registerBeanDefinition(eq("a"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("b"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("d"), (BeanDefinition) anyObject());
		ordered.verify(beanFactory).registerBeanDefinition(eq("c"), (BeanDefinition) anyObject());
	}

	@Test
	public void invokeAwareMethodsInImportSelector() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AwareConfig.class);
		context.getBean(MessageSource.class);

		assertThat(SampleRegistrar.beanFactory, is((BeanFactory) context.getBeanFactory()));
		assertThat(SampleRegistrar.classLoader, is(context.getBeanFactory().getBeanClassLoader()));
		assertThat(SampleRegistrar.resourceLoader, is(notNullValue()));
		assertThat(SampleRegistrar.environment, is((Environment) context.getEnvironment()));
	}

	@Configuration
	@Import(SampleImportSelector.class)
	static class AwareConfig {

	}

	static class SampleImportSelector implements ImportSelector, BeanClassLoaderAware, ResourceLoaderAware,
			BeanFactoryAware, EnvironmentAware {

		static ClassLoader classLoader;
		static ResourceLoader resourceLoader;
		static BeanFactory beanFactory;
		static Environment environment;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			SampleRegistrar.classLoader = classLoader;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			SampleRegistrar.beanFactory = beanFactory;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			SampleRegistrar.resourceLoader = resourceLoader;
		}

		@Override
		public void setEnvironment(Environment environment) {
			SampleRegistrar.environment = environment;
		}

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] {};
		}
	}

	@Sample
	@Configuration
	static class Config {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import({ DeferredImportSelector1.class, DeferredImportSelector2.class,
		ImportSelector1.class, ImportSelector2.class })
	public static @interface Sample {
	}

	public static class ImportSelector1 implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { ImportedSelector1.class.getName() };
		}
	}

	public static class ImportSelector2 implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { ImportedSelector2.class.getName() };
		}
	}

	public static class DeferredImportSelector1 implements DeferredImportSelector, Ordered {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { DeferredImportedSelector1.class.getName() };
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static class DeferredImportSelector2 implements DeferredImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { DeferredImportedSelector2.class.getName() };
		}

	}

	@Configuration
	public static class ImportedSelector1 {

		@Bean
		public String a() {
			return "a";
		}
	}

	@Configuration
	public static class ImportedSelector2 {

		@Bean
		public String b() {
			return "b";
		}
	}

	@Configuration
	public static class DeferredImportedSelector1 {

		@Bean
		public String c() {
			return "c";
		}
	}

	@Configuration
	public static class DeferredImportedSelector2 {

		@Bean
		public String d() {
			return "d";
		}
	}

}
