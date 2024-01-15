/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ConstructorInjectionNestedTests.TopLevelConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link SpringExtension} in a JUnit Jupiter environment ... when using
 * constructor injection as opposed to field injection (see SPR-16653).
 *
 * @author Sam Brannen
 * @since 5.0.5
 * @see ContextConfigurationNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ConstructorInjectionNestedTests {

	final String foo;

	ConstructorInjectionNestedTests(TestInfo testInfo, @Autowired String foo) {
		this.foo = foo;
	}

	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo("foo");
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorTests {

		final String bar;

		@Autowired
		AutowiredConstructorTests(String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorParameterTests {

		final String bar;

		AutowiredConstructorParameterTests(@Autowired String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class QualifiedConstructorParameterTests {

		final String bar;

		QualifiedConstructorParameterTests(TestInfo testInfo, @Qualifier("bar") String s) {
			this.bar = s;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class SpelConstructorParameterTests {

		final String bar;
		final int answer;

		SpelConstructorParameterTests(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer) {
			this.bar = bar;
			this.answer = answer;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
			assertThat(answer).isEqualTo(42);
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
