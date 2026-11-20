/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean} that use by-name lookup with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2.13
 */
@SpringJUnitConfig
public class MockitoBeanByNameLookupTestMethodScopedExtensionContextIntegrationTests {

	@MockitoBean("field")
	ExampleService field;

	@MockitoBean("nonExistingBean")
	ExampleService nonExisting;


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
	}

}
