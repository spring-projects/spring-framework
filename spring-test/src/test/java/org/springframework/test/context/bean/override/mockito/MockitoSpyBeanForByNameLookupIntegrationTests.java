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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeanForByNameLookupIntegrationTests.Config;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoSpyBean} that use by-name lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig(Config.class)
public class MockitoSpyBeanForByNameLookupIntegrationTests {

	@MockitoSpyBean("field1")
	ExampleService field;

	@MockitoSpyBean("field1")
	ExampleService renamed1;


	@Test
	void fieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field1"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(field);

		assertThat(field.greeting()).isEqualTo("bean1");
	}

	@Test
	void renamedFieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field1"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(renamed1);

		assertThat(renamed1.greeting()).isEqualTo("bean1");
	}

	@Nested
	@DisplayName("With @MockitoSpyBean in enclosing class and in @Nested class")
	public class MockitoSpyBeanNestedTests {

		@MockitoSpyBean("field2")
		ExampleService nestedField;

		@MockitoSpyBean("field2")
		ExampleService renamed2;

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("field2"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(nestedField);

			assertThat(nestedField.greeting()).isEqualTo("bean2");
		}

		@Test
		void renamedFieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("field2"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(renamed2);

			assertThat(renamed2.greeting()).isEqualTo("bean2");
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean("field1")
		ExampleService bean1() {
			return new RealExampleService("bean1");
		}

		@Bean("field2")
		ExampleService bean2() {
			return new RealExampleService("bean2");
		}
	}

}
