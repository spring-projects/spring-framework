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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests which demonstrate usage of {@link DisabledIf @DisabledIf}
 * enabled by {@link SpringExtension} in a JUnit 5 (Jupiter) environment.
 *
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see DisabledIf
 * @see SpringExtension
 */
class DisabledIfTestCase {

	@ExtendWith(SpringExtension.class)
	@ContextConfiguration(classes = Config.class)
	@TestPropertySource(properties = "foo = true")
	@Nested
	class DisabledIfOnMethodTestCase {

		@Test
		@DisabledIf("true")
		void disabledByStringTrue() {
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
		@DisabledIf("#{T(java.lang.Boolean).TRUE}")
		void disabledBySpelBoolean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{'tr' + 'ue'}")
		void disabledBySpelStringConcatenation() {
			fail("This test must be disabled");
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

	@ExtendWith(SpringExtension.class)
	@ContextConfiguration(classes = Config.class)
	@Nested
	@DisabledIf("true")
	class DisabledIfOnClassTestCase {

		@Test
		void foo() {
			fail("This test must be disabled");
		}

		// Even though method level condition is not disabling test, class level condition should take precedence
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
