/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.NestedTestsWithSpringAndJUnitJupiterTestCase.TopLevelConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests that verify support for {@code @Nested} test classes
 * in conjunction with the {@link SpringExtension} in a JUnit 5 (Jupiter)
 * environment.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(TopLevelConfig.class)
class NestedTestsWithSpringAndJUnitJupiterTestCase {

	@Autowired
	String foo;


	@Test
	void topLevelTest() {
		assertEquals("foo", foo);
	}


	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class NestedTestCase {

		@Autowired
		String bar;


		@Test
		void nestedTest() throws Exception {
			// In contrast to nested test classes running in JUnit 4, the foo
			// field in the outer instance should have been injected from the
			// test ApplicationContext for the outer instance.
			assertEquals("foo", foo);
			assertEquals("bar", bar);
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
	static class NestedConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}

}
