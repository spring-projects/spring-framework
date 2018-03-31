/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.NestedTestsWithConstructorInjectionWithSpringAndJUnitJupiterTestCase.TopLevelConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link SpringExtension} in a JUnit Jupiter environment ... when using
 * constructor injection as opposed to field injection (see SPR-16653).
 *
 * <p>
 * To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0.5
 * @see NestedTestsWithSpringAndJUnitJupiterTestCase
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(TopLevelConfig.class)
class NestedTestsWithConstructorInjectionWithSpringAndJUnitJupiterTestCase {

	final String foo;

	NestedTestsWithConstructorInjectionWithSpringAndJUnitJupiterTestCase(TestInfo testInfo, @Autowired String foo) {
		this.foo = foo;
	}

	@Test
	void topLevelTest() {
		assertEquals("foo", foo);
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructor {

		final String bar;

		@Autowired
		AutowiredConstructor(String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() throws Exception {
			assertEquals("foo", foo);
			assertEquals("bar", bar);
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorParameter {

		final String bar;

		AutowiredConstructorParameter(@Autowired String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() throws Exception {
			assertEquals("foo", foo);
			assertEquals("bar", bar);
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class QualifiedConstructorParameter {

		final String bar;

		QualifiedConstructorParameter(TestInfo testInfo, @Qualifier("bar") String s) {
			this.bar = s;
		}

		@Test
		void nestedTest() throws Exception {
			assertEquals("foo", foo);
			assertEquals("bar", bar);
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class SpelConstructorParameter {

		final String bar;
		final int answer;

		SpelConstructorParameter(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer) {
			this.bar = bar;
			this.answer = answer;
		}

		@Test
		void nestedTest() throws Exception {
			assertEquals("foo", foo);
			assertEquals("bar", bar);
			assertEquals(42, answer);
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
