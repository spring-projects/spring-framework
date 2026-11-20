/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Comparators}.
 *
 * @since 6.1.2
 * @author Mathieu Amblard
 * @author Sam Brannen
 */
class ComparatorsTests {

	@Test
	void nullsLow() {
		assertThat(Comparators.nullsLow().compare("boo", "boo")).isZero();
		assertThat(Comparators.nullsLow().compare(null, null)).isZero();
		assertThat(Comparators.nullsLow().compare(null, "boo")).isNegative();
		assertThat(Comparators.nullsLow().compare("boo", null)).isPositive();
	}

	@Test
	void nullsLowWithExplicitComparator() {
		assertThat(Comparators.nullsLow(String::compareTo).compare("boo", "boo")).isZero();
		assertThat(Comparators.nullsLow(String::compareTo).compare(null, null)).isZero();
		assertThat(Comparators.nullsLow(String::compareTo).compare(null, "boo")).isNegative();
		assertThat(Comparators.nullsLow(String::compareTo).compare("boo", null)).isPositive();
	}

	@Test
	void nullsHigh() {
		assertThat(Comparators.nullsHigh().compare("boo", "boo")).isZero();
		assertThat(Comparators.nullsHigh().compare(null, null)).isZero();
		assertThat(Comparators.nullsHigh().compare(null, "boo")).isPositive();
		assertThat(Comparators.nullsHigh().compare("boo", null)).isNegative();
	}

	@Test
	void nullsHighWithExplicitComparator() {
		assertThat(Comparators.nullsHigh(String::compareTo).compare("boo", "boo")).isZero();
		assertThat(Comparators.nullsHigh(String::compareTo).compare(null, null)).isZero();
		assertThat(Comparators.nullsHigh(String::compareTo).compare(null, "boo")).isPositive();
		assertThat(Comparators.nullsHigh(String::compareTo).compare("boo", null)).isNegative();
	}

}
