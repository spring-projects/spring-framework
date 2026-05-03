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

package org.springframework.test.context.bean.override.mockito.constructor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} that use by-name lookup
 * on constructor parameters.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36096">gh-36096</a>
 * @see org.springframework.test.context.bean.override.mockito.MockitoBeanByNameLookupTestMethodScopedExtensionContextIntegrationTests
 */
@SpringJUnitConfig
class MockitoBeanByNameLookupForConstructorParametersIntegrationTests {

	final ExampleService service0A;

	final ExampleService service0B;

	final ExampleService service0C;

	final ExampleService nonExisting;


	MockitoBeanByNameLookupForConstructorParametersIntegrationTests(
			@MockitoBean ExampleService s0A,
			@MockitoBean(name = "s0B") ExampleService service0B,
			@MockitoBean @Qualifier("s0C") ExampleService service0C,
			@MockitoBean("nonExistingBean") ExampleService nonExisting) {

		this.service0A = s0A;
		this.service0B = service0B;
		this.service0C = service0C;
		this.nonExisting = nonExisting;
	}


	@Test
	void parameterNameIsUsedAsBeanName(ApplicationContext ctx) {
		assertThat(this.service0A)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("s0A"));

		assertThat(this.service0A.greeting()).as("mocked greeting").isNull();
	}

	@Test
	void explicitBeanNameOverridesParameterName(ApplicationContext ctx) {
		assertThat(this.service0B)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("s0B"));

		assertThat(this.service0B.greeting()).as("mocked greeting").isNull();
	}

	@Test
	void qualifierIsUsedToResolveByName(ApplicationContext ctx) {
		assertThat(this.service0C)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("s0C"));

		assertThat(this.service0C.greeting()).as("mocked greeting").isNull();
	}

	@Test
	void mockIsCreatedWhenNoBeanExistsWithProvidedName(ApplicationContext ctx) {
		assertThat(this.nonExisting)
				.satisfies(MockitoAssertions::assertIsMock)
				.isSameAs(ctx.getBean("nonExistingBean"));

		assertThat(this.nonExisting.greeting()).as("mocked greeting").isNull();
	}


	@Nested
	class NestedTests {

		@Autowired
		@Qualifier("s0A")
		ExampleService localService0A;

		@Autowired
		@Qualifier("nonExistingBean")
		ExampleService localNonExisting;

		final ExampleService nestedNonExisting;


		NestedTests(@MockitoBean("nestedNonExistingBean") ExampleService nestedNonExisting) {
			this.nestedNonExisting = nestedNonExisting;
		}


		@Test
		void mockFromEnclosingClassIsAccessibleViaAutowiring(ApplicationContext ctx) {
			assertThat(this.localService0A)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(service0A)
					.isSameAs(ctx.getBean("s0A"));

			assertThat(this.localService0A.greeting()).as("mocked greeting").isNull();
		}

		@Test
		void mockForNonExistingBeanFromEnclosingClassIsAccessibleViaAutowiring(ApplicationContext ctx) {
			assertThat(this.localNonExisting)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(nonExisting)
					.isSameAs(ctx.getBean("nonExistingBean"));

			assertThat(this.localNonExisting.greeting()).as("mocked greeting").isNull();
		}

		@Test
		void nestedConstructorParameterIsMockedWhenNoBeanExistsWithProvidedName(ApplicationContext ctx) {
			assertThat(this.nestedNonExisting)
					.satisfies(MockitoAssertions::assertIsMock)
					.isSameAs(ctx.getBean("nestedNonExistingBean"));

			assertThat(this.nestedNonExisting.greeting()).as("mocked greeting").isNull();
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService s0A() {
			return new RealExampleService("prod s0A");
		}

		@Bean
		ExampleService s0B() {
			return new RealExampleService("prod s0B");
		}

		@Bean
		ExampleService s0C() {
			return new RealExampleService("prod s0C");
		}
	}

}
