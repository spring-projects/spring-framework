/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * Same as {@link ExplicitPropertiesFilesRepeatedTestPropertySourceTests}, but
 * with the order of the properties files reversed.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource("second.properties")
@TestPropertySource("first.properties")
class ReversedExplicitPropertiesFilesRepeatedTestPropertySourceTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("alpha", "beta");
		assertEnvironmentValue("first", "1111");
		assertEnvironmentValue("second", "1111");
	}

}
