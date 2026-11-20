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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoSpyBean} that use by-name lookup with test class
 * {@link ExtensionContextScope}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@SpringExtensionConfig(useTestClassScopedExtensionContext = true)
public class MockitoSpyBeanByNameLookupTestClassScopedExtensionContextIntegrationTests {

	@MockitoSpyBean("field1")
	ExampleService field;

	@MockitoSpyBean("field3")
	ExampleService prototypeScoped;


	@Test
	void fieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field1"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(field);

		assertThat(field.greeting()).isEqualTo("bean1");
	}

	@Test
	void fieldForPrototypeHasOverride(ConfigurableApplicationContext ctx) {
		assertThat(ctx.getBean("field3"))
				.isInstanceOf(ExampleService.class)
				.satisfies(MockitoAssertions::assertIsSpy)
				.isSameAs(prototypeScoped);
		assertThat(ctx.getBeanFactory().getBeanDefinition("field3").isSingleton()).as("isSingleton").isTrue();

		assertThat(prototypeScoped.greeting()).isEqualTo("bean3");
	}

	@Nested
	@DisplayName("With @MockitoSpyBean in enclosing class and in @Nested class")
	public class MockitoSpyBeanNestedTests {

		@Autowired
		@Qualifier("field1")
		ExampleService localField;

		@MockitoSpyBean("field2")
		ExampleService nestedField;

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("field1"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(localField);

			assertThat(localField.greeting()).isEqualTo("bean1");
		}

		@Test
		void nestedFieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("field2"))
					.isInstanceOf(ExampleService.class)
					.satisfies(MockitoAssertions::assertIsSpy)
					.isSameAs(nestedField);

			assertThat(nestedField.greeting()).isEqualTo("bean2");
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

		@Bean("field3")
		@Scope("prototype")
		ExampleService bean3() {
			return new RealExampleService("bean3");
		}
	}

}
