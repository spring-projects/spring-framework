/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@MetaInlinedTestProperty
@MetaComposedTestProperty
class MetaInlinedPropertyOverridesMetaMetaInlinedPropertyTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("enigma", "meta");
	}

}
