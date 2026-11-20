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
 * <p>Tests multiple local {@link TestPropertySource} declarations.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource(properties = "first = repeated")
@TestPropertySource(properties = "second = repeated")
@TestPropertySource(properties = "first = repeated override")
class RepeatedTestPropertySourceTests extends AbstractRepeatableTestPropertySourceTests {

	@Test
	void test() {
		assertEnvironmentValue("first", "repeated override");
		assertEnvironmentValue("second", "repeated");
	}

}
