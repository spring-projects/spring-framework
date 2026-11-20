/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.nested;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests.TopLevelConfig;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration tests for <em>nested</em> test classes that are
 * executed via a custom JUnit 4 {@link HierarchicalContextRunner} and Spring's
 * {@link SpringClassRule} and {@link SpringMethodRule} support.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.junit.jupiter.nested.ContextConfigurationTestClassScopedExtensionContextNestedTests
 * @see org.springframework.test.context.junit.jupiter.nested.ContextConfigurationTestMethodScopedExtensionContextNestedTests
 */
@RunWith(HierarchicalContextRunner.class)
@ContextConfiguration(classes = TopLevelConfig.class)
public class NestedTestsWithSpringRulesTests extends SpringRuleConfigurer {

	@Autowired
	String foo;


	@Test
	public void topLevelTest() {
		assertThat(foo).isEqualTo("foo");
	}


	@ContextConfiguration(classes = NestedConfig.class)
	public class NestedTests extends SpringRuleConfigurer {

		@Autowired
		String bar;


		@Test
		public void nestedTest() {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	public static class TopLevelConfig {

		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	public static class NestedConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}

}
