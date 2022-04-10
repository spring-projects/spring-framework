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

package org.springframework.beans.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @Bean annotation lookup.
 *
 * @author tao zhang
 */
class DefaultListableBeanFactoryAnnotationLookupTests {

	@Test
	void beanDefinedInInstanceMethodDoesNotHaveAnnotationsFromItsConfigurationClass() {
		beanDoesNotHaveAnnotationsFromItsConfigurationClass(InstanceBeanMethodConfiguration.class);
	}

	@Test
	void beanDefinedInStaticMethodDoesNotHaveAnnotationsFromItsConfigurationClass() {
		beanDoesNotHaveAnnotationsFromItsConfigurationClass(StaticBeanMethodConfiguration.class);
	}

	void beanDoesNotHaveAnnotationsFromItsConfigurationClass(Class<?> config) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(config)) {
			ExampleAnnotation annotation = context.getBeanFactory().findAnnotationOnBean("exampleBean",
					ExampleAnnotation.class);
			assertThat(annotation).isNull();
		}
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExampleAnnotation {

	}

	@Configuration
	@ExampleAnnotation
	static class StaticBeanMethodConfiguration {

		@Bean
		static String exampleBean() {
			return "example";
		}

	}

	@Configuration
	@ExampleAnnotation
	static class InstanceBeanMethodConfiguration {

		@Bean
		String exampleBean() {
			return "example";
		}

	}

}
