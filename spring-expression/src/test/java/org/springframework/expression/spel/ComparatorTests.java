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

package org.springframework.expression.spel;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeComparator;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for type comparison
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 */
public class ComparatorTests {

	@Test
	void testPrimitives() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		// primitive int
		assertThat(comparator.compare(1, 2)).isLessThan(0);
		assertThat(comparator.compare(1, 1)).isEqualTo(0);
		assertThat(comparator.compare(2, 1)).isGreaterThan(0);

		assertThat(comparator.compare(1.0d, 2)).isLessThan(0);
		assertThat(comparator.compare(1.0d, 1)).isEqualTo(0);
		assertThat(comparator.compare(2.0d, 1)).isGreaterThan(0);

		assertThat(comparator.compare(1.0f, 2)).isLessThan(0);
		assertThat(comparator.compare(1.0f, 1)).isEqualTo(0);
		assertThat(comparator.compare(2.0f, 1)).isGreaterThan(0);

		assertThat(comparator.compare(1L, 2)).isLessThan(0);
		assertThat(comparator.compare(1L, 1)).isEqualTo(0);
		assertThat(comparator.compare(2L, 1)).isGreaterThan(0);

		assertThat(comparator.compare(1, 2L)).isLessThan(0);
		assertThat(comparator.compare(1, 1L)).isEqualTo(0);
		assertThat(comparator.compare(2, 1L)).isGreaterThan(0);

		assertThat(comparator.compare(1L, 2L)).isLessThan(0);
		assertThat(comparator.compare(1L, 1L)).isEqualTo(0);
		assertThat(comparator.compare(2L, 1L)).isGreaterThan(0);
	}

	@Test
	void testNonPrimitiveNumbers() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();

		BigDecimal bdOne = new BigDecimal("1");
		BigDecimal bdTwo = new BigDecimal("2");

		assertThat(comparator.compare(bdOne, bdTwo)).isLessThan(0);
		assertThat(comparator.compare(bdOne, new BigDecimal("1"))).isEqualTo(0);
		assertThat(comparator.compare(bdTwo, bdOne)).isGreaterThan(0);

		assertThat(comparator.compare(1, bdTwo)).isLessThan(0);
		assertThat(comparator.compare(1, bdOne)).isEqualTo(0);
		assertThat(comparator.compare(2, bdOne)).isGreaterThan(0);

		assertThat(comparator.compare(1.0d, bdTwo)).isLessThan(0);
		assertThat(comparator.compare(1.0d, bdOne)).isEqualTo(0);
		assertThat(comparator.compare(2.0d, bdOne)).isGreaterThan(0);

		assertThat(comparator.compare(1.0f, bdTwo)).isLessThan(0);
		assertThat(comparator.compare(1.0f, bdOne)).isEqualTo(0);
		assertThat(comparator.compare(2.0f, bdOne)).isGreaterThan(0);

		assertThat(comparator.compare(1L, bdTwo)).isLessThan(0);
		assertThat(comparator.compare(1L, bdOne)).isEqualTo(0);
		assertThat(comparator.compare(2L, bdOne)).isGreaterThan(0);

	}

	@Test
	void testNulls() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare(null,"abc")).isLessThan(0);
		assertThat(comparator.compare(null,null)).isEqualTo(0);
		assertThat(comparator.compare("abc",null)).isGreaterThan(0);
	}

	@Test
	void testObjects() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.compare("a","a")).isEqualTo(0);
		assertThat(comparator.compare("a","b")).isLessThan(0);
		assertThat(comparator.compare("b","a")).isGreaterThan(0);
	}

	@Test
	void testCanCompare() throws EvaluationException {
		TypeComparator comparator = new StandardTypeComparator();
		assertThat(comparator.canCompare(null,1)).isTrue();
		assertThat(comparator.canCompare(1,null)).isTrue();

		assertThat(comparator.canCompare(2,1)).isTrue();
		assertThat(comparator.canCompare("abc","def")).isTrue();
		assertThat(comparator.canCompare("abc",3)).isFalse();
		assertThat(comparator.canCompare(String.class,3)).isFalse();
	}

	@Test
	public void customComparatorWorksWithEquality() {
		final StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setTypeComparator(customComparator);

		ExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("'1' == 1");

		assertThat(expr.getValue(ctx, Boolean.class)).isTrue();

	}

	// A silly comparator declaring everything to be equal
	private TypeComparator customComparator = new TypeComparator() {
		@Override
		public boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject) {
			return true;
		}

		@Override
		public int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException {
			return 0;
		}

	};

}
