/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specific {@link DirectFieldAccessor} tests.
 *
 * @author Jose Luis Martin
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class DirectFieldAccessorTests extends AbstractPropertyAccessorTests {

	@Override
	protected DirectFieldAccessor createAccessor(Object target) {
		return new DirectFieldAccessor(target);
	}


	@Test
	void withShadowedField() {
		final StringBuilder sb = new StringBuilder();

		TestBean target = new TestBean() {
			@SuppressWarnings("unused")
			StringBuilder name = sb;
		};

		DirectFieldAccessor dfa = createAccessor(target);
		assertThat(dfa.getPropertyType("name")).isEqualTo(StringBuilder.class);
		assertThat(dfa.getPropertyValue("name")).isEqualTo(sb);
	}

}
