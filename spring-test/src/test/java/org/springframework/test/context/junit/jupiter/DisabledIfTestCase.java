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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf}
 * in conjunction with the {@link SpringExtension} in a JUnit 5 (Jupiter)
 * environment.
 *
 * @author Tadaya Tsuyukubo
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIfConditionTestCase
 * @see DisabledIf
 * @see SpringExtension
 */
class DisabledIfTestCase {

	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "foo = true")
	@Nested
	class DisabledIfOnMethodTestCase {

		@Test
		@DisabledIf("true")
		void disabledByStringTrue() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   true   ")
		void disabledByStringTrueWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("TrUe")
		void disabledByStringTrueIgnoreCase() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("${foo}")
		void disabledByPropertyPlaceholder() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("\t${foo}   ")
		void disabledByPropertyPlaceholderWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{T(java.lang.Boolean).TRUE}")
		void disabledBySpelBoolean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   #{T(java.lang.Boolean).TRUE}   ")
		void disabledBySpelBooleanWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{'tr' + 'ue'}")
		void disabledBySpelStringConcatenation() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{6 * 7 == 42}")
		void disabledBySpelMathematicalComparison() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledOnMac
		void disabledBySpelOsCheckInCustomComposedAnnotation() {
			assertFalse(System.getProperty("os.name").contains("Mac"), "This test must be disabled on Mac OS");
		}

		@Test
		@DisabledIf("#{@booleanTrueBean}")
		void disabledBySpelBooleanTrueBean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{@stringTrueBean}")
		void disabledBySpelStringTrueBean() {
			fail("This test must be disabled");
		}

	}

	@SpringJUnitConfig(Config.class)
	@Nested
	@DisabledIf("true")
	class DisabledIfOnClassTestCase {

		@Test
		void foo() {
			fail("This test must be disabled");
		}

		// Even though method level condition is not disabling test, class level condition
		// should take precedence
		@Test
		@DisabledIf("false")
		void bar() {
			fail("This test must be disabled");
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
