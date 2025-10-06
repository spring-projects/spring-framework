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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean} that use by-name lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class MockitoBeanByNameLookupIntegrationTests {

	@MockitoBean("field")
	ExampleService field;

	@MockitoBean("nonExistingBean")
	ExampleService nonExisting;

	@MockitoBean("prototypeScoped")
	ExampleService prototypeScoped;


	@Test
	void fieldAndRenamedFieldHaveSameOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(field);

		assertThat(field.greeting()).as("mocked greeting").isNull();
	}

	@Test
	void fieldIsMockedWhenNoOriginalBean(ApplicationContext ctx) {
		assertThat(ctx.getBean("nonExistingBean"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(nonExisting);

		assertThat(nonExisting.greeting()).as("mocked greeting").isNull();
	}

	@Test
	void fieldForPrototypeHasOverride(ConfigurableApplicationContext ctx) {
		assertThat(ctx.getBean("prototypeScoped"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(prototypeScoped);
		assertThat(ctx.getBeanFactory().getBeanDefinition("prototypeScoped").isSingleton()).as("isSingleton").isTrue();

		assertThat(prototypeScoped.greeting()).as("mocked greeting").isNull();
	}


	@Nested
	@DisplayName("With @MockitoBean in enclosing class and in @Nested class")
	public class MockitoBeanNestedTests {

		@Autowired
		@Qualifier("field")
		ExampleService localField;

		@Autowired
		@Qualifier("nonExistingBean")
		ExampleService localNonExisting;

		@MockitoBean("nestedField")
		ExampleService nestedField;

		@MockitoBean("nestedNonExistingBean")
		ExampleService nestedNonExisting;


		@Test
		void fieldAndRenamedFieldHaveSameOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("field"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(localField);

			assertThat(localField.greeting()).as("mocked greeting").isNull();
		}

		@Test
		void fieldIsMockedWhenNoOriginalBean(ApplicationContext ctx) {
			assertThat(ctx.getBean("nonExistingBean"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(localNonExisting);

			assertThat(localNonExisting.greeting()).as("mocked greeting").isNull();
		}

		@Test
		void nestedFieldAndRenamedFieldHaveSameOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(nestedField);
		}

		@Test
		void nestedFieldIsMockedWhenNoOriginalBean(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedNonExistingBean"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(nestedNonExisting);
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean("field")
		ExampleService bean1() {
			return new RealExampleService("Hello Field");
		}

		@Bean("nestedField")
		ExampleService bean2() {
			return new RealExampleService("Hello Nested Field");
		}

		@Bean("prototypeScoped")
		@Scope("prototype")
		ExampleService bean3() {
			return new RealExampleService("Hello Prototype Field");
		}
	}

}
