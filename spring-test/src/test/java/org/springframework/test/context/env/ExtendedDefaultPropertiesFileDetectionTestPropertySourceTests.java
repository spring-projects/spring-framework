/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests that verify detection of default properties files
 * when {@link TestPropertySource @TestPropertySource} is <em>empty</em>
 * at multiple levels within a class hierarchy.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@TestPropertySource
class ExtendedDefaultPropertiesFileDetectionTestPropertySourceTests extends
		DefaultPropertiesFileDetectionTestPropertySourceTests {

	@Test
	@Override
	void verifyPropertiesAreAvailableInEnvironment() {
		super.verifyPropertiesAreAvailableInEnvironment();
		// from ExtendedDefaultPropertiesFileDetectionTestPropertySourceTests.properties
		assertEnvironmentValue("enigma", "auto detected");
	}

}
