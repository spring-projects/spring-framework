/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.mockito.MockingDetails;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.bean.override.example.ExampleGenericService;
import org.springframework.test.context.bean.override.example.StringExampleGenericService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} on a field with generics can
 * be used to replace an existing bean with matching generics that's produced by a
 * {@link FactoryBean} that's programmatically registered via an
 * {@link ImportBeanDefinitionRegistrar}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests {

	@MockitoSpyBean("exampleService")
	ExampleGenericService<String> exampleService;


	@Test
	void testSpying() {
		MockingDetails mockingDetails = mockingDetails(this.exampleService);
		assertThat(mockingDetails.isSpy()).isTrue();
		assertThat(mockingDetails.getMockCreationSettings().getSpiedInstance())
				.isInstanceOf(StringExampleGenericService.class);
	}


	@Configuration(proxyBeanMethods = false)
	@Import(FactoryBeanRegistrar.class)
	static class Config {
	}

	static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {

			RootBeanDefinition definition = new RootBeanDefinition(ExampleGenericServiceFactoryBean.class);
			ResolvableType targetType = ResolvableType.forClassWithGenerics(
					ExampleGenericServiceFactoryBean.class, null, ExampleGenericService.class);
			definition.setTargetType(targetType);
			registry.registerBeanDefinition("exampleService", definition);
		}
	}

	static class ExampleGenericServiceFactoryBean<T, U extends ExampleGenericService<T>> implements FactoryBean<U> {

		@Override
		@SuppressWarnings("unchecked")
		public U getObject() throws Exception {
			return (U) new StringExampleGenericService("Enigma");
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Class<ExampleGenericService> getObjectType() {
			return ExampleGenericService.class;
		}
	}

}
