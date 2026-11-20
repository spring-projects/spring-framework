/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ConstructorInjectionTestClassScopedExtensionContextNestedTests.TopLevelConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link SpringExtension} in a JUnit Jupiter environment with test class
 * {@link ExtensionContextScope} ... when using constructor injection as opposed
 * to field injection (see SPR-16653).
 *
 * @author Sam Brannen
 * @since 5.0.5
 * @see ContextConfigurationTestClassScopedExtensionContextNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(TopLevelConfig.class)
@SpringExtensionConfig(useTestClassScopedExtensionContext = true)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ConstructorInjectionTestClassScopedExtensionContextNestedTests {

	final String foo;

	ConstructorInjectionTestClassScopedExtensionContextNestedTests(TestInfo testInfo, @Autowired String foo) {
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
