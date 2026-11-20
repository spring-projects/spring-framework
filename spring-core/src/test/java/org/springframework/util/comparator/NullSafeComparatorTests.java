/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NullSafeComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
@Deprecated
class NullSafeComparatorTests {

	@Test
	@SuppressWarnings("unchecked")
	void shouldCompareWithNullsLow() {
		@SuppressWarnings("deprecation")
		Comparator<String> c = NullSafeComparator.NULLS_LOW;

		assertThat(c.compare("boo", "boo")).isZero();
		assertThat(c.compare(null, null)).isZero();
		assertThat(c.compare(null, "boo")).isNegative();
		assertThat(c.compare("boo", null)).isPositive();
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldCompareWithNullsHigh() {
		@SuppressWarnings("deprecation")
		Comparator<String> c = NullSafeComparator.NULLS_HIGH;

		assertThat(c.compare("boo", "boo")).isZero();
		assertThat(c.compare(null, null)).isZero();
		assertThat(c.compare(null, "boo")).isPositive();
		assertThat(c.compare("boo", null)).isNegative();
	}

}
