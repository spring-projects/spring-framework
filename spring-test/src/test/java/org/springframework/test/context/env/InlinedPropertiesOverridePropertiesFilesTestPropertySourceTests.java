/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties that overrides properties files.
 *
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource(locations = "explicit.properties", properties = "explicit = inlined")
class InlinedPropertiesOverridePropertiesFilesTestPropertySourceTests {

	@Autowired
	Environment env;

	@Value("${explicit}")
	String explicit;


	@Test
	void inlinedPropertyOverridesValueFromPropertiesFile() {
		assertThat(env.getProperty("explicit")).isEqualTo("inlined");
		assertThat(this.explicit).isEqualTo("inlined");
	}


	@Configuration
	static class Config {
	}

}
