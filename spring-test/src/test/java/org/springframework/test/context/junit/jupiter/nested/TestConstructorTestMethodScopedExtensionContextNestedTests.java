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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.DEFAULT_SCOPE_PROPERTY_NAME;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
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
class TestConstructorTestMethodScopedExtensionContextNestedTests {

	@Test
	void runTests() {
		EngineTestKit.engine("junit-jupiter")
				.configurationParameter(DEFAULT_SCOPE_PROPERTY_NAME, TEST_METHOD.name())
				.selectors(selectClass(TestCase.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(8).succeeded(8).failed(0));
	}


	@SpringJUnitConfig(Config.class)
	@TestConstructor(autowireMode = ALL)
	@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
	static class TestCase {

		TestCase(String text) {
			assertThat(text).isEqualTo("enigma");
		}

		@Test
		void test() {
		}


		@Nested
		@SpringJUnitConfig(Config.class)
		@TestConstructor(autowireMode = ANNOTATED)
		class ConfigOverriddenByDefaultTestCase {

			@Autowired
			ConfigOverriddenByDefaultTestCase(String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}
		}

		@Nested
		@NestedTestConfiguration(INHERIT)
		class InheritedConfigTestCase {

			InheritedConfigTestCase(String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}


			@Nested
			class DoubleNestedWithImplicitlyInheritedConfigTestCase {

				DoubleNestedWithImplicitlyInheritedConfigTestCase(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}


				@Nested
				class TripleNestedWithImplicitlyInheritedConfigTestCase {

					TripleNestedWithImplicitlyInheritedConfigTestCase(String text) {
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
			class DoubleNestedWithOverriddenConfigTestCase {

				DoubleNestedWithOverriddenConfigTestCase(@Autowired String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}


				@Nested
				@NestedTestConfiguration(INHERIT)
				class TripleNestedWithInheritedConfigTestCase {

					@Autowired
					TripleNestedWithInheritedConfigTestCase(String text) {
						assertThat(text).isEqualTo("enigma");
					}

					@Test
					void test() {
					}
				}

				@Nested
				@NestedTestConfiguration(INHERIT)
				class TripleNestedWithInheritedConfigAndTestInterfaceTestCase implements TestInterface {

					TripleNestedWithInheritedConfigAndTestInterfaceTestCase(String text) {
						assertThat(text).isEqualTo("enigma");
					}

					@Test
					void test() {
					}
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
