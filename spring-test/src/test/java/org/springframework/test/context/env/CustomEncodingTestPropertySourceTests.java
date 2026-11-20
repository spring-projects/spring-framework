/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
