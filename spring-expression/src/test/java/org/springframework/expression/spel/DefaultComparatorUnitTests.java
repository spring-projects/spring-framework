/*
 * Copyright 2002-2021 the original author or authors.
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
 * Unit tests for type comparison
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 */
class DefaultComparatorUnitTests {

	@Test
	void testPrimitives() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		// primitive int
		assertThat(comparator.compare(1, 2) < 0).isTrue();
		assertThat(comparator.compare(1, 1) == 0).isTrue();
		assertThat(comparator.compare(2, 1) > 0).isTrue();

		assertThat(comparator.compare(1.0d, 2) < 0).isTrue();
		assertThat(comparator.compare(1.0d, 1) == 0).isTrue();
		assertThat(comparator.compare(2.0d, 1) > 0).isTrue();

		assertThat(comparator.compare(1.0f, 2) < 0).isTrue();
		assertThat(comparator.compare(1.0f, 1) == 0).isTrue();
		assertThat(comparator.compare(2.0f, 1) > 0).isTrue();

		assertThat(comparator.compare(1L, 2) < 0).isTrue();
		assertThat(comparator.compare(1L, 1) == 0).isTrue();
		assertThat(comparator.compare(2L, 1) > 0).isTrue();

		assertThat(comparator.compare(1, 2L) < 0).isTrue();
		assertThat(comparator.compare(1, 1L) == 0).isTrue();
		assertThat(comparator.compare(2, 1L) > 0).isTrue();

		assertThat(comparator.compare(1L, 2L) < 0).isTrue();
		assertThat(comparator.compare(1L, 1L) == 0).isTrue();
		assertThat(comparator.compare(2L, 1L) > 0).isTrue();
	}

	@Test
	void testNonPrimitiveNumbers() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();

		BigDecimal bdOne = new BigDecimal("1");
		BigDecimal bdTwo = new BigDecimal("2");

		assertThat(comparator.compare(bdOne, bdTwo) < 0).isTrue();
		assertThat(comparator.compare(bdOne, new BigDecimal("1")) == 0).isTrue();
		assertThat(comparator.compare(bdTwo, bdOne) > 0).isTrue();

		assertThat(comparator.compare(1, bdTwo) < 0).isTrue();
		assertThat(comparator.compare(1, bdOne) == 0).isTrue();
		assertThat(comparator.compare(2, bdOne) > 0).isTrue();

		assertThat(comparator.compare(1.0d, bdTwo) < 0).isTrue();
		assertThat(comparator.compare(1.0d, bdOne) == 0).isTrue();
		assertThat(comparator.compare(2.0d, bdOne) > 0).isTrue();

		assertThat(comparator.compare(1.0f, bdTwo) < 0).isTrue();
		assertThat(comparator.compare(1.0f, bdOne) == 0).isTrue();
		assertThat(comparator.compare(2.0f, bdOne) > 0).isTrue();

		assertThat(comparator.compare(1L, bdTwo) < 0).isTrue();
		assertThat(comparator.compare(1L, bdOne) == 0).isTrue();
		assertThat(comparator.compare(2L, bdOne) > 0).isTrue();

	}

	@Test
	void testNulls() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare(null,"abc")<0).isTrue();
		assertThat(comparator.compare(null,null)==0).isTrue();
		assertThat(comparator.compare("abc",null)>0).isTrue();
	}

	@Test
	void testObjects() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare("a","a")==0).isTrue();
		assertThat(comparator.compare("a","b")<0).isTrue();
		assertThat(comparator.compare("b","a")>0).isTrue();
	}

	@Test
	void testCanCompare() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.canCompare(null,1)).isTrue();
		assertThat(comparator.canCompare(1,null)).isTrue();

		assertThat(comparator.canCompare(2,1)).isTrue();
		assertThat(comparator.canCompare("abc","def")).isTrue();
		assertThat(comparator.canCompare("abc",3)).isTrue();
		assertThat(comparator.canCompare(String.class,3)).isFalse();
	}

}
