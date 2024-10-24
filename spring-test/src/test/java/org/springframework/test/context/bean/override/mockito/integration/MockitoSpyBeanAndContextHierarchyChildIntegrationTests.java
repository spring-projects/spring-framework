/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests which verify that {@link MockitoBean @MockitoBean} and
 * {@link MockitoSpyBean @MockitoSpyBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockitoBeanAndContextHierarchyParentIntegrationTests
 */
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
public class MockitoSpyBeanAndContextHierarchyChildIntegrationTests extends
		MockitoBeanAndContextHierarchyParentIntegrationTests {

	@MockitoSpyBean
	ExampleServiceCaller serviceCaller;

	@Autowired
	ApplicationContext context;


	@Test
	@Override
	void test() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
		assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();

		ApplicationContext parentContext = context.getParent();
		assertThat(parentContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller.class)).isEmpty();

		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

		assertThat(service.greeting()).isEqualTo("mock");
		assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
	}


	@Configuration(proxyBeanMethods = false)
	static class ChildConfig {

		@Bean
		ExampleServiceCaller serviceCaller(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
