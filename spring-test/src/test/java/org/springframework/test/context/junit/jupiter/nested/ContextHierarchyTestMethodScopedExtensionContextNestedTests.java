/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.nested.ContextHierarchyTestMethodScopedExtensionContextNestedTests.ParentConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextHierarchy @ContextHierarchy} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 6.2.13
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy(@ContextConfiguration(classes = ParentConfig.class))
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class ContextHierarchyTestMethodScopedExtensionContextNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";
	private static final String QUX = "qux";


	@Autowired
	String foo;

	@Autowired
	ApplicationContext context;


	@Test
	void topLevelTest() {
		assertThat(this.context).as("local ApplicationContext").isNotNull();
		assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

		assertThat(foo).isEqualTo(FOO);
	}

	@Nested
	@ContextConfiguration(classes = NestedConfig.class)
	class NestedTests {

		@Autowired
		String bar;

		@Autowired
		ApplicationContext context;


		@Test
		void nestedTest() {
			assertThat(this.context).as("local ApplicationContext").isNotNull();
			assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

			// The foo field in the outer instance should have been injected from
			// the test ApplicationContext for NestedTests.
			assertThat(foo).isEqualTo(BAR);
			assertThat(this.bar).isEqualTo(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@ContextConfiguration(classes = Child1Config.class)
	class NestedInheritedCfgTests {

		@Autowired
		String bar;

		@Autowired
		ApplicationContext context;


		@Test
		void nestedTest() {
			assertThat(this.context).as("local ApplicationContext").isNotNull();
			assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

			// The foo field in the outer instance and the bar field in the inner
			// instance should both have been injected from the test ApplicationContext
			// for the inner instance.
			assertThat(foo).as("foo")
					.isEqualTo(this.context.getBean("foo", String.class))
					.isEqualTo(QUX + 1);
			assertThat(this.bar).isEqualTo(BAZ + 1);
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@ContextHierarchy({
			@ContextConfiguration(classes = ParentConfig.class),
			@ContextConfiguration(classes = Child2Config.class)
		})
		class DoubleNestedOverriddenCfgTests {

			@Autowired
			String bar;

			@Autowired
			ApplicationContext context;


			@Test
			void nestedTest() {
				assertThat(this.context).as("local ApplicationContext").isNotNull();
				assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

				assertThat(foo).as("foo")
						.isEqualTo(this.context.getBean("foo", String.class))
						.isEqualTo(QUX + 2);
				assertThat(this.bar).isEqualTo(BAZ + 2);
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedInheritedCfgAndTestInterfaceTests implements TestInterface {

				@Autowired
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;

				@Autowired
				ApplicationContext context;


				@Test
				void nestedTest() {
					assertThat(this.context).as("local ApplicationContext").isNotNull();
					assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();
					assertThat(this.context.getParent().getParent()).as("grandparent ApplicationContext").isNotNull();

					assertThat(foo).as("foo")
							.isEqualTo(this.localFoo)
							.isEqualTo(this.context.getBean("foo", String.class))
							.isEqualTo("test interface");
					assertThat(this.bar).isEqualTo(BAZ + 2);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class ParentConfig {

		@Bean
		String foo() {
			return FOO;
		}
	}

	@Configuration
	static class Child1Config {

		@Bean
		String foo() {
			return QUX + 1;
		}

		@Bean
		String bar() {
			return BAZ + 1;
		}
	}

	@Configuration
	static class Child2Config {

		@Bean
		String foo() {
			return QUX + 2;
		}

		@Bean
		String bar() {
			return BAZ + 2;
		}
	}

	@Configuration
	static class NestedConfig {

		@Bean
		String bar() {
			return BAR;
		}
	}

	@Configuration
	static class TestInterfaceConfig {

		@Bean
		String foo() {
			return "test interface";
		}
	}

	@ContextConfiguration(classes = TestInterfaceConfig.class)
	interface TestInterface {
	}

}
