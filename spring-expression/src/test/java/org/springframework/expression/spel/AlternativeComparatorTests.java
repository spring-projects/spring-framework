/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import org.junit.Test;
import org.springframework.lang.Nullable;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.*;

/**
 * Test construction of arrays.
 *
 * @author Andy Clement
 */
public class AlternativeComparatorTests {
	private ExpressionParser parser = new SpelExpressionParser();

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

	@Test
	public void customComparatorWorksWithEquality() {
		final StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setTypeComparator(customComparator);

		Expression expr = parser.parseExpression("'1' == 1");

		assertEquals(true, expr.getValue(ctx, Boolean.class));

	}
}
