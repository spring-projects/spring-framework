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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.DEFAULT_SCOPE_PROPERTY_NAME;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
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
class ContextConfigurationTestMethodScopedExtensionContextNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";


	@Test
	void runTests() {
		EngineTestKit.engine("junit-jupiter")
				.configurationParameter(DEFAULT_SCOPE_PROPERTY_NAME, TEST_METHOD.name())
				.selectors(selectClass(TestCase.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(6).succeeded(6).failed(0));
	}


	@SpringJUnitConfig(TopLevelConfig.class)
	@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
	static class TestCase {

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
		class NestedTestCase {

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
		class NestedTestCaseWithInheritedConfigTestCase {

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
			class DoubleNestedWithOverriddenConfigTestCase {

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
				class TripleNestedWithInheritedConfigTestCase {

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
				class TripleNestedWithInheritedConfigAndTestInterfaceTestCase implements TestInterface {

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
