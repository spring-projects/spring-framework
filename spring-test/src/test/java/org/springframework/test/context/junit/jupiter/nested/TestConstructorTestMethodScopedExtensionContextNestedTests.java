/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;
import static org.springframework.test.context.TestConstructor.AutowireMode.ANNOTATED;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestConstructor @TestConstructor} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 6.2.13
 */
@SpringJUnitConfig
@TestConstructor(autowireMode = ALL)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestConstructorTestMethodScopedExtensionContextNestedTests {

	TestConstructorTestMethodScopedExtensionContextNestedTests(String text) {
		assertThat(text).isEqualTo("enigma");
	}

	@Test
	void test() {
	}


	@Nested
	@SpringJUnitConfig(Config.class)
	@TestConstructor(autowireMode = ANNOTATED)
	class ConfigOverriddenByDefaultTests {

		@Autowired
		ConfigOverriddenByDefaultTests(String text) {
			assertThat(text).isEqualTo("enigma");
		}

		@Test
		void test() {
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		InheritedConfigTests(String text) {
			assertThat(text).isEqualTo("enigma");
		}

		@Test
		void test() {
		}


		@Nested
		class DoubleNestedWithImplicitlyInheritedConfigTests {

			DoubleNestedWithImplicitlyInheritedConfigTests(String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}


			@Nested
			class TripleNestedWithImplicitlyInheritedConfigTests {

				TripleNestedWithImplicitlyInheritedConfigTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(Config.class)
		@TestConstructor(autowireMode = ANNOTATED)
		class DoubleNestedWithOverriddenConfigTests {

			DoubleNestedWithOverriddenConfigTests(@Autowired String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigTests {

				@Autowired
				TripleNestedWithInheritedConfigTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				TripleNestedWithInheritedConfigAndTestInterfaceTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		String text() {
			return "enigma";
		}
	}

	@TestConstructor(autowireMode = ALL)
	interface TestInterface {
	}

}
