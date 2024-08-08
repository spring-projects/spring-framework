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
import org.mockito.Mockito;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBeanForByNameLookupIntegrationTests.Config;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoSpyBean} that use by-name lookup.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@SpringJUnitConfig(Config.class)
public class MockitoSpyBeanForByNameLookupIntegrationTests {

	@MockitoSpyBean(name = "field")
	ExampleService field;

	@MockitoSpyBean(name = "nestedField")
	ExampleService nestedField;

	@MockitoSpyBean(name = "field")
	ExampleService renamed1;

	@MockitoSpyBean(name = "nestedField")
	ExampleService renamed2;


	@Test
	void fieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field"))
				.isInstanceOf(ExampleService.class)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
				.isSameAs(this.field);

		assertThat(this.field.greeting()).isEqualTo("Hello Field");
	}

	@Test
	void renamedFieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field"))
				.isInstanceOf(ExampleService.class)
				.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
				.isSameAs(this.renamed1);

		assertThat(this.renamed1.greeting()).isEqualTo("Hello Field");
	}

	@Nested
	@DisplayName("With @MockitoSpyBean in enclosing class")
	public class MockitoSpyBeanNestedTests {

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField"))
					.isInstanceOf(ExampleService.class)
					.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
					.isSameAs(nestedField);

			assertThat(nestedField.greeting()).isEqualTo("Hello Nested Field");
		}

		@Test
		void renamedFieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField"))
					.isInstanceOf(ExampleService.class)
					.satisfies(o -> assertThat(Mockito.mockingDetails(o).isSpy()).as("isSpy").isTrue())
					.isSameAs(renamed2);

			assertThat(renamed2.greeting()).isEqualTo("Hello Nested Field");
		}
	}

	@Configuration
	static class Config {

		@Bean("field")
		ExampleService bean1() {
			return new RealExampleService("Hello Field");
		}

		@Bean("nestedField")
		ExampleService bean2() {
			return new RealExampleService("Hello Nested Field");
		}
	}

}
