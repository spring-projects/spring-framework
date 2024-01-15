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
 * @see org.springframework.test.context.junit.jupiter.nested.ContextConfigurationNestedTests
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
