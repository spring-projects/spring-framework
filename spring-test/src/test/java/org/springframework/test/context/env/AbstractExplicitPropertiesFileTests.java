/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 5.2
 */
@SpringJUnitConfig
abstract class AbstractExplicitPropertiesFileTests {

	@Autowired
	Environment env;


	@Test
	@DisplayName("verify properties are available in the Environment")
	void verifyPropertiesAreAvailableInEnvironment() {
		String userHomeKey = "user.home";
		assertThat(env.getProperty(userHomeKey)).isEqualTo(System.getProperty(userHomeKey));
		assertThat(env.getProperty("explicit")).isEqualTo("enigma");
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
