/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf}
 * in conjunction with the {@link SpringExtension} in a JUnit Jupiter environment.
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
			String os = System.getProperty("os.name").toLowerCase();
			assertThat(os).as("This test must be disabled on Mac OS").doesNotContain("mac");
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
