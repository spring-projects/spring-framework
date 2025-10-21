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
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.DEFAULT_SCOPE_PROPERTY_NAME;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link SpringExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD} ... when using constructor injection
 * as opposed to field injection (see SPR-16653).
 *
 * @author Sam Brannen
 * @since 6.2.13
 * @see ContextConfigurationTestClassScopedExtensionContextNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
class ConstructorInjectionTestMethodScopedExtensionContextNestedTests {

	@Test
	void runTests() {
		EngineTestKit.engine("junit-jupiter")
				.configurationParameter(DEFAULT_SCOPE_PROPERTY_NAME, TEST_METHOD.name())
				.selectors(selectClass(TestCase.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(5).succeeded(5).failed(0));
	}


	@SpringJUnitConfig(TopLevelConfig.class)
	@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
	static class TestCase {

		final String foo;

		TestCase(TestInfo testInfo, @Autowired String foo) {
			this.foo = foo;
		}

		@Test
		void topLevelTest() {
			assertThat(foo).isEqualTo("foo");
		}

		@Nested
		@SpringJUnitConfig(NestedConfig.class)
		class AutowiredConstructorTestCase {

			final String bar;

			@Autowired
			AutowiredConstructorTestCase(String bar) {
				this.bar = bar;
			}

			@Test
			void nestedTest() {
				assertThat(foo).isEqualTo("bar");
				assertThat(bar).isEqualTo("bar");
			}
		}

		@Nested
		@SpringJUnitConfig(NestedConfig.class)
		class AutowiredConstructorParameterTestCase {

			final String bar;

			AutowiredConstructorParameterTestCase(@Autowired String bar) {
				this.bar = bar;
			}

			@Test
			void nestedTest() {
				assertThat(foo).isEqualTo("bar");
				assertThat(bar).isEqualTo("bar");
			}
		}

		@Nested
		@SpringJUnitConfig(NestedConfig.class)
		class QualifiedConstructorParameterTestCase {

			final String bar;

			QualifiedConstructorParameterTestCase(TestInfo testInfo, @Qualifier("bar") String s) {
				this.bar = s;
			}

			@Test
			void nestedTest() {
				assertThat(foo).isEqualTo("bar");
				assertThat(bar).isEqualTo("bar");
			}
		}

		@Nested
		@SpringJUnitConfig(NestedConfig.class)
		class SpelConstructorParameterTestCase {

			final String bar;
			final int answer;

			SpelConstructorParameterTestCase(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer) {
				this.bar = bar;
				this.answer = answer;
			}

			@Test
			void nestedTest() {
				assertThat(foo).isEqualTo("bar");
				assertThat(bar).isEqualTo("bar");
				assertThat(answer).isEqualTo(42);
			}
		}

	}

	// -------------------------------------------------------------------------

	@Configuration
	static class TopLevelConfig {

		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	static class NestedConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}

}
