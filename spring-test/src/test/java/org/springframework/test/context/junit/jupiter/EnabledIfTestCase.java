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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests which verify support for {@link EnabledIf @EnabledIf}
 * in conjunction with the {@link SpringExtension} in a JUnit 5 (Jupiter)
 * environment.
 *
 * @author Tadaya Tsuyukubo
 * @author Sam Brannen
 * @since 5.0
 * @see EnabledIfConditionTestCase
 * @see EnabledIf
 * @see SpringExtension
 */
class EnabledIfTestCase {

	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "foo = false")
	@Nested
	class EnabledIfOnMethodTestCase {

		@Test
		@EnabledIf("false")
		void enabledIfWithStringFalse() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("   false   ")
		void enabledIfWithStringFalseWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("FaLsE")
		void enabledIfWithStringFalseIgnoreCase() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("${foo}")
		void enabledIfWithPropertyPlaceholder() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("\t${foo}   ")
		void enabledIfWithPropertyPlaceholderWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("#{T(Boolean).FALSE}")
		void enabledIfWithSpelBoolean() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("   #{T(Boolean).FALSE}   ")
		void enabledIfWithSpelBooleanWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("#{'fal' + 'se'}")
		void enabledIfWithSpelStringConcatenation() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("#{1 + 2 == 4}")
		void enabledIfWithSpelArithmeticComparison() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledOnMac
		void enabledIfWithSpelOsCheckInCustomComposedAnnotation() {
			String os = System.getProperty("os.name").toLowerCase();
			assertTrue(os.contains("mac"), "This test must be enabled on Mac OS");
			assertFalse(os.contains("win"), "This test must be disabled on Windows");
		}

		@Test
		@EnabledIf("#{@booleanFalseBean}")
		void enabledIfWithSpelBooleanFalseBean() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("#{@stringFalseBean}")
		void enabledIfWithSpelStringFalseBean() {
			fail("This test must be disabled");
		}
	}

	@SpringJUnitConfig(Config.class)
	@Nested
	@EnabledIf("false")
	class EnabledIfOnClassTestCase {

		@Test
		void foo() {
			fail("This test must be disabled");
		}

		@Test
		@EnabledIf("true")
		void bar() {
			fail("This test must be disabled due to class-level condition");
		}
	}

	@Configuration
	static class Config {

		@Bean
		Boolean booleanFalseBean() {
			return Boolean.FALSE;
		}

		@Bean
		String stringFalseBean() {
			return "false";
		}
	}

}
