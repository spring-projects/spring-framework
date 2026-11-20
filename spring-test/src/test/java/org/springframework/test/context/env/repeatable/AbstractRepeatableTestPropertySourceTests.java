/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests involving
 * {@link TestPropertySource @TestPropertySource} as a repeatable annotation.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
abstract class AbstractRepeatableTestPropertySourceTests {

	@Autowired
	Environment env;


	protected void assertEnvironmentValue(String key, String expected) {
		assertThat(env.getProperty(key)).as("Value of key [" + key + "].").isEqualTo(expected);
	}


	@Configuration
	static class Config {
	}

}
