/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 */
class FactoryMethodResolutionTests {

	@Test
	void factoryMethodCanBeResolvedWithBeanMetadataCachingEnabled() {
		assertThatFactoryMethodCanBeResolved(true);
	}

	@Test
	void factoryMethodCanBeResolvedWithBeanMetadataCachingDisabled() {
		assertThatFactoryMethodCanBeResolved(false);
	}

	private void assertThatFactoryMethodCanBeResolved(boolean cache) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getBeanFactory().setCacheBeanMetadata(cache);
			context.register(ImportSelectorConfiguration.class);
			context.refresh();
			BeanDefinition definition = context.getBeanFactory().getMergedBeanDefinition("exampleBean");
			assertThat(((RootBeanDefinition)definition).getResolvedFactoryMethod()).isNotNull();
		}
	}


	@Configuration
	@Import(ExampleImportSelector.class)
	static class ImportSelectorConfiguration {
	}


	static class ExampleImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { TestConfiguration.class.getName() };
		}
	}


	@Configuration
	static class TestConfiguration {

		@Bean
		@ExampleAnnotation
		public ExampleBean exampleBean() {
			return new ExampleBean();
		}
	}


	static class ExampleBean {
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {
	}

}
