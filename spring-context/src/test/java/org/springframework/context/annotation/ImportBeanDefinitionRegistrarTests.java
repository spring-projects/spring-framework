/*
 * Copyright 2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.MessageSource;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Tests for handling of {@link ImportBeanDefinitionRegistrar}s.
 * 
 * @author Oliver Gierke
 */
public class ImportBeanDefinitionRegistrarTests {

	@Test
	public void invokesAwareMethodsInImportBeanDefinitionRegistrar() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		context.getBean(MessageSource.class);

		assertThat(SampleRegistrar.beanFactory, is((BeanFactory) context.getBeanFactory()));
		assertThat(SampleRegistrar.classLoader, is(context.getBeanFactory().getBeanClassLoader()));
		assertThat(SampleRegistrar.resourceLoader, is(notNullValue()));
	}

	@Sample
	@Configuration
	static class Config {

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(SampleRegistrar.class)
	public static @interface Sample {

	}

	static class SampleRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware, ResourceLoaderAware,
			BeanFactoryAware {

		static ClassLoader classLoader;
		static ResourceLoader resourceLoader;
		static BeanFactory beanFactory;

		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		}
	}
}
