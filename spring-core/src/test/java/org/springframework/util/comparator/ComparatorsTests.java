/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
