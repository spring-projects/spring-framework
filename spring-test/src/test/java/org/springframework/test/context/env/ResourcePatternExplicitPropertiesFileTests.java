/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * for resource patterns for resource locations.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@TestPropertySource("classpath*:/org/springframework/test/context/env/pattern/file?.properties")
class ResourcePatternExplicitPropertiesFileTests {

	@Test
	void verifyPropertiesAreAvailableInEnvironment(@Autowired Environment env) {
		assertEnvironmentContainsProperties(env, "from.p1", "from.p2", "from.p3");
	}


	private static void assertEnvironmentContainsProperties(Environment env, String... names) {
		for (String name : names) {
			assertThat(env.containsProperty(name)).as("environment contains property '%s'", name).isTrue();
		}
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
