/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.support.StandardTypeComparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for type comparison
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 */
class StandardTypeComparatorTests {

	@Test
	void testPrimitives() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		// primitive int
		assertThat(comparator.compare(1, 2)).isNegative();
		assertThat(comparator.compare(1, 1)).isZero();
		assertThat(comparator.compare(2, 1)).isPositive();

		assertThat(comparator.compare(1.0d, 2)).isNegative();
		assertThat(comparator.compare(1.0d, 1)).isZero();
		assertThat(comparator.compare(2.0d, 1)).isPositive();

		assertThat(comparator.compare(1.0f, 2)).isNegative();
		assertThat(comparator.compare(1.0f, 1)).isZero();
		assertThat(comparator.compare(2.0f, 1)).isPositive();

		assertThat(comparator.compare(1L, 2)).isNegative();
		assertThat(comparator.compare(1L, 1)).isZero();
		assertThat(comparator.compare(2L, 1)).isPositive();

		assertThat(comparator.compare(1, 2L)).isNegative();
		assertThat(comparator.compare(1, 1L)).isZero();
		assertThat(comparator.compare(2, 1L)).isPositive();

		assertThat(comparator.compare(1L, 2L)).isNegative();
		assertThat(comparator.compare(1L, 1L)).isZero();
		assertThat(comparator.compare(2L, 1L)).isPositive();
	}

	@Test
	void testNonPrimitiveNumbers() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();

		BigDecimal bdOne = new BigDecimal("1");
		BigDecimal bdTwo = new BigDecimal("2");

		assertThat(comparator.compare(bdOne, bdTwo)).isNegative();
		assertThat(comparator.compare(bdOne, new BigDecimal("1"))).isZero();
		assertThat(comparator.compare(bdTwo, bdOne)).isPositive();

		assertThat(comparator.compare(1, bdTwo)).isNegative();
		assertThat(comparator.compare(1, bdOne)).isZero();
		assertThat(comparator.compare(2, bdOne)).isPositive();

		assertThat(comparator.compare(1.0d, bdTwo)).isNegative();
		assertThat(comparator.compare(1.0d, bdOne)).isZero();
		assertThat(comparator.compare(2.0d, bdOne)).isPositive();

		assertThat(comparator.compare(1.0f, bdTwo)).isNegative();
		assertThat(comparator.compare(1.0f, bdOne)).isZero();
		assertThat(comparator.compare(2.0f, bdOne)).isPositive();

		assertThat(comparator.compare(1L, bdTwo)).isNegative();
		assertThat(comparator.compare(1L, bdOne)).isZero();
		assertThat(comparator.compare(2L, bdOne)).isPositive();

	}

	@Test
	void testNulls() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare(null, "abc")).isNegative();
		assertThat(comparator.compare(null, null)).isZero();
		assertThat(comparator.compare("abc", null)).isPositive();
	}

	@Test
	void testObjects() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare("a", "a")).isZero();
		assertThat(comparator.compare("a", "b")).isNegative();
		assertThat(comparator.compare("b", "a")).isPositive();
	}

	@Test
	void testCanCompare() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.canCompare(null, 1)).isTrue();
		assertThat(comparator.canCompare(1, null)).isTrue();

		assertThat(comparator.canCompare(2, 1)).isTrue();
		assertThat(comparator.canCompare("abc", "def")).isTrue();
		assertThat(comparator.canCompare("abc", 3)).isFalse();
		assertThat(comparator.canCompare(String.class, 3)).isFalse();
	}

	@Test
	void shouldUseCustomComparator() {
		TypeComparator comparator = new StandardTypeComparator();
		ComparableType t1 = new ComparableType(1);
		ComparableType t2 = new ComparableType(2);

		assertThat(comparator.canCompare(t1, 2)).isFalse();
		assertThat(comparator.canCompare(t1, t2)).isTrue();
		assertThat(comparator.compare(t1, t1)).isZero();
		assertThat(comparator.compare(t1, t2)).isNegative();
		assertThat(comparator.compare(t2, t1)).isPositive();
	}


	static class ComparableType implements Comparable<ComparableType> {

		private final int id;

		public ComparableType(int id) {
			this.id = id;
		}

		@Override
		public int compareTo(ComparableType other) {
			return this.id - other.id;
		}
	}

}
