/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an explicitly named properties file that overrides a
 * system property.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource("SystemPropertyOverridePropertiesFileTestPropertySourceTests.properties")
class SystemPropertyOverridePropertiesFileTestPropertySourceTests {

	private static final String KEY = SystemPropertyOverridePropertiesFileTestPropertySourceTests.class.getSimpleName() + ".riddle";

	@Autowired
	protected Environment env;


	@BeforeAll
	static void setSystemProperty() {
		System.setProperty(KEY, "override me!");
	}

	@AfterAll
	static void removeSystemProperty() {
		System.setProperty(KEY, "");
	}

	@Test
	void verifyPropertiesAreAvailableInEnvironment() {
		assertThat(env.getProperty(KEY)).isEqualTo("enigma");
	}


	// -------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
