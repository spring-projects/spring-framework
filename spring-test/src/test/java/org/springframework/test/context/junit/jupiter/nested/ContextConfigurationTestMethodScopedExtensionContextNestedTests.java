/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ContextConfigurationTestMethodScopedExtensionContextNestedTests.TopLevelConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextConfiguration @ContextConfiguration} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 6.2.13
 * @see ConstructorInjectionTestClassScopedExtensionContextNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ContextConfigurationTestMethodScopedExtensionContextNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";

	@Autowired(required = false)
	@Qualifier("foo")
	String foo;


	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo(FOO);
	}


	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class NestedTests {

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			assertThat(foo).as("foo bean should not be present").isNull();
			assertThat(this.localFoo).as("local foo bean should not be present").isNull();
			assertThat(this.bar).isEqualTo(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class NestedTestsWithInheritedConfigTests {

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			// Since the configuration is inherited, the foo field in the outer instance
			// and the bar field in the inner instance should both have been injected
			// from the test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.localFoo).isEqualTo(FOO);
			assertThat(this.bar).isEqualTo(FOO);
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(NestedConfig.class)
		class DoubleNestedWithOverriddenConfigTests {

			@Autowired(required = false)
			@Qualifier("foo")
			String localFoo;

			@Autowired
			String bar;


			@Test
			void test() {
				assertThat(foo).as("foo bean should not be present").isNull();
				assertThat(this.localFoo).as("local foo bean should not be present").isNull();
				assertThat(this.bar).isEqualTo(BAR);
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigTests {

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;


				@Test
				void test() {
					assertThat(foo).as("foo bean should not be present").isNull();
					assertThat(this.localFoo).as("local foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				@Qualifier("bar")
				String bar;

				@Autowired
				String baz;


				@Test
				void test() {
					assertThat(foo).as("foo bean should not be present").isNull();
					assertThat(this.localFoo).as("local foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
					assertThat(this.baz).isEqualTo(BAZ);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class TopLevelConfig {

		@Bean
		String foo() {
			return FOO;
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
		String baz() {
			return BAZ;
		}
	}

	@ContextConfiguration(classes = TestInterfaceConfig.class)
	interface TestInterface {
	}

}
