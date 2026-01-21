/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.hierarchies.MockitoBeanByTypeInChildContextHierarchyTests.Config1;
import org.springframework.test.context.bean.override.mockito.hierarchies.MockitoBeanByTypeInChildContextHierarchyTests.Config2;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotMock;

/**
 * Verifies that {@link MockitoBean @MockitoBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy} with named context levels, when
 * a bean is only mocked "by type" in the child.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
	@ContextConfiguration(classes = Config1.class),
	@ContextConfiguration(classes = Config2.class, name = "child")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockitoBeanByTypeInChildContextHierarchyTests {

	@MockitoBean(contextName = "child")
	ExampleService service;

	@Autowired
	ExampleServiceCaller serviceCaller1;

	@Autowired
	ExampleServiceCaller serviceCaller2;


	@Test
	void test(ApplicationContext context) {
		ExampleService serviceInParent = context.getParent().getBean(ExampleService.class);

		assertIsNotMock(serviceInParent);

		when(service.greeting()).thenReturn("Mock 2");

		assertThat(service.greeting()).isEqualTo("Mock 2");
		assertThat(serviceCaller1.getService()).isSameAs(serviceInParent);
		assertThat(serviceCaller2.getService()).isSameAs(service);
		assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Service 1");
		assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Mock 2");
	}


	@Configuration
	static class Config1 {

		@Bean
		ExampleService service() {
			return new RealExampleService("Service 1");
		}

		@Bean
		ExampleServiceCaller serviceCaller1(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

	@Configuration
	static class Config2 {

		@Bean
		ExampleService service() {
			return new RealExampleService("Service 2");
		}

		@Bean
		ExampleServiceCaller serviceCaller2(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
