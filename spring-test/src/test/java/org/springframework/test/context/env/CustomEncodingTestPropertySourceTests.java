/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.env;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * with custom resource encoding.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
class CustomEncodingTestPropertySourceTests {

	@Nested
	@TestPropertySource(locations = "test-ISO-8859-1.properties", encoding = "UTF-8" )
	@DirtiesContext
	class IncorrectEncodingTests {

		@Test
		void propertyIsAvailableInEnvironment(@Autowired Environment env) {
			// The "é" characters in "Générales" get converted to U+FFFD : REPLACEMENT CHARACTER.
			assertThat(env.getProperty("text")).isEqualTo("G\uFFFDn\uFFFDrales");
		}
	}

	@Nested
	@TestPropertySource(locations = "test-ISO-8859-1.properties", encoding = "ISO-8859-1" )
	@DirtiesContext
	class ExplicitEncodingTests {

		@Test
		void propertyIsAvailableInEnvironment(@Autowired Environment env) {
			assertThat(env.getProperty("text")).isEqualTo("Générales");
		}
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
