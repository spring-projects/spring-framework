/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for contributing additional properties
 * files to the Spring {@code Environment} via {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@TestPropertySource("extended.properties")
@DisabledInAotMode("Because ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode")
class MergedPropertiesFilesTestPropertySourceTests extends
		ExplicitPropertiesFileInClasspathTestPropertySourceTests {

	@Test
	void verifyExtendedPropertiesAreAvailableInEnvironment() {
		assertThat(env.getProperty("extended", Integer.class)).isEqualTo(42);
	}

}
