/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an explicitly named properties file that overrides an
 * application-level property configured via
 * {@link PropertySource @PropertySource} on an
 * {@link Configuration @Configuration} class.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource("ApplicationPropertyOverridePropertiesFileTestPropertySourceTests.properties")
class ApplicationPropertyOverridesPropertiesFileTestPropertySourceTests {

	@Autowired
	protected Environment env;


	@Test
	void verifyPropertiesAreAvailableInEnvironment() {
		assertThat(env.getProperty("explicit")).isEqualTo("test override");
	}


	// -------------------------------------------------------------------

	@Configuration
	@PropertySource("classpath:/org/springframework/test/context/env/explicit.properties")
	static class Config {
		/* no user beans required for these tests */
	}

}
