/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ParameterizedConstructorInjectionTestMethodScopedExtensionContextNestedTests.TopLevelConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Parameterized variant of {@link ConstructorInjectionTestMethodScopedExtensionContextNestedTests}
 * which tests {@link ParameterizedClass @ParameterizedClass} support.
 *
 * @author Sam Brannen
 * @since 7.0
 */
@SpringJUnitConfig(TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
class ParameterizedConstructorInjectionTestMethodScopedExtensionContextNestedTests {

	final String beanName;
	final String foo;
	final ApplicationContext context;


	ParameterizedConstructorInjectionTestMethodScopedExtensionContextNestedTests(
			String beanName, TestInfo testInfo, @Autowired String foo, ApplicationContext context) {

		this.context = context;
		this.beanName = beanName;
		this.foo = foo;
	}


	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo("foo");
		if (beanName.equals("foo")) {
			assertThat(context.getBean(beanName, String.class)).isEqualTo(beanName);
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorTests {

		final String bar;
		final ApplicationContext localContext;

		@Autowired
		AutowiredConstructorTests(String bar, ApplicationContext context) {
			this.bar = bar;
			this.localContext = context;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("bar");
			assertThat(bar).isEqualTo("bar");
			if (beanName.equals("bar")) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorParameterTests {

		final String bar;
		final ApplicationContext localContext;

		AutowiredConstructorParameterTests(@Autowired String bar, ApplicationContext context) {
			this.bar = bar;
			this.localContext = context;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("bar");
			assertThat(bar).isEqualTo("bar");
			if (beanName.equals("bar")) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class QualifiedConstructorParameterTests {

		final String bar;
		final ApplicationContext localContext;

		QualifiedConstructorParameterTests(TestInfo testInfo, @Qualifier("bar") String s, ApplicationContext context) {
			this.bar = s;
			this.localContext = context;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("bar");
			assertThat(bar).isEqualTo("bar");
			if (beanName.equals("bar")) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class SpelConstructorParameterTests {

		final String bar;
		final int answer;
		final ApplicationContext localContext;

		SpelConstructorParameterTests(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer, ApplicationContext context) {
			this.bar = bar;
			this.answer = answer;
			this.localContext = context;
		}

		@Test
		void nestedTest() {
			assertThat(foo).isEqualTo("bar");
			assertThat(bar).isEqualTo("bar");
			assertThat(answer).isEqualTo(42);
			if (beanName.equals("bar")) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class TopLevelConfig {

		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class NestedConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}

}
