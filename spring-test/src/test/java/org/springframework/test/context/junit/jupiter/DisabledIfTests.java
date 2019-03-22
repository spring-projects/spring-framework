/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.SpringJUnitJupiterTestSuite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf}
 * in conjunction with the {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Tadaya Tsuyukubo
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIfConditionTests
 * @see DisabledIf
 * @see SpringExtension
 */
class DisabledIfTests {

	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "foo = true")
	@Nested
	class DisabledIfOnMethodTests {

		@Test
		@DisabledIf("true")
		void disabledIfWithStringTrue() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   true   ")
		void disabledIfWithStringTrueWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("TrUe")
		void disabledIfWithStringTrueIgnoreCase() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("${__EnigmaPropertyShouldNotExist__:true}")
		void disabledIfWithPropertyPlaceholderForNonexistentPropertyWithDefaultValue() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "${foo}", loadContext = true)
		void disabledIfWithPropertyPlaceholder() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "\t${foo}   ", loadContext = true)
		void disabledIfWithPropertyPlaceholderWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{T(Boolean).TRUE}")
		void disabledIfWithSpelBoolean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   #{T(Boolean).TRUE}   ")
		void disabledIfWithSpelBooleanWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{'tr' + 'ue'}")
		void disabledIfWithSpelStringConcatenation() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{6 * 7 == 42}")
		void disabledIfWithSpelArithmeticComparison() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledOnMac
		void disabledIfWithSpelOsCheckInCustomComposedAnnotation() {
			assertFalse(System.getProperty("os.name").contains("Mac"), "This test must be disabled on Mac OS");
		}

		@Test
		@DisabledIf(expression = "#{@booleanTrueBean}", loadContext = true)
		void disabledIfWithSpelBooleanTrueBean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "#{@stringTrueBean}", loadContext = true)
		void disabledIfWithSpelStringTrueBean() {
			fail("This test must be disabled");
		}

	}

	@SpringJUnitConfig(Config.class)
	@Nested
	@DisabledIf("true")
	class DisabledIfOnClassTests {

		@Test
		void foo() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("false")
		void bar() {
			fail("This test must be disabled due to class-level condition");
		}

	}

	@Configuration
	static class Config {

		@Bean
		Boolean booleanTrueBean() {
			return Boolean.TRUE;
		}

		@Bean
		String stringTrueBean() {
			return "true";
		}
	}

}
