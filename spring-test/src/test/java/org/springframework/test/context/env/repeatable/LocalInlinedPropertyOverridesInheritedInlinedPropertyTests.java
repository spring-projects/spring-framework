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
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource(properties = "key1 = local value")
@TestPropertySource(properties = "second = local override")
class LocalInlinedPropertyOverridesInheritedInlinedPropertyTests extends RepeatedTestPropertySourceTests {

	@Test
	@Override
	void test() {
		assertEnvironmentValue("key1", "local value");
		assertEnvironmentValue("second", "local override");
		assertEnvironmentValue("first", "repeated override");
	}

}
