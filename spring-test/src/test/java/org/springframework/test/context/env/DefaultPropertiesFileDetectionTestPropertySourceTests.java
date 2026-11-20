/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify detection of a default properties file
 * when {@link TestPropertySource @TestPropertySource} is <em>empty</em>.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource
class DefaultPropertiesFileDetectionTestPropertySourceTests {

	@Autowired
	protected Environment env;


	@Test
	void verifyPropertiesAreAvailableInEnvironment() {
		// from DefaultPropertiesFileDetectionTestPropertySourceTests.properties
		assertEnvironmentValue("riddle", "auto detected");
	}

	protected void assertEnvironmentValue(String key, String expected) {
		assertThat(env.getProperty(key)).as("Value of key [" + key + "].").isEqualTo(expected);
	}


	// -------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
