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
 * Integration tests that verify support for overriding properties from
 * properties files via inlined properties configured with
 * {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@TestPropertySource(properties = { "explicit = inlined", "extended = inlined1", "extended = inlined2" })
@DisabledInAotMode("Because ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode")
class MergedPropertiesFilesOverriddenByInlinedPropertiesTestPropertySourceTests extends
		MergedPropertiesFilesTestPropertySourceTests {

	@Test
	@Override
	void verifyPropertiesAreAvailableInEnvironment() {
		assertThat(env.getProperty("explicit")).isEqualTo("inlined");
	}

	@Test
	@Override
	void verifyExtendedPropertiesAreAvailableInEnvironment() {
		assertThat(env.getProperty("extended")).isEqualTo("inlined2");
	}

}
