/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
class TestPropertySourceInterfaceTests implements TestPropertySourceTestInterface {

	@Autowired
	Environment env;


	@Test
	void propertiesAreAvailableInEnvironment() {
		assertThat(property("foo")).isEqualTo("bar");
		assertThat(property("enigma")).isEqualTo("42");
	}

	private String property(String key) {
		return env.getProperty(key);
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
