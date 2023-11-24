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

package org.springframework.expression.spel.ast;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InlineList} and {@link InlineMap}.
 *
 * @author Semyon Danilov
 * @author Sam Brannen
 * @since 6.1
 */
class InlineCollectionTests {

	private final ExpressionParser parser = new SpelExpressionParser();


	@Nested
	class InlineListTests {

		@Test
		void listIsCached() {
			InlineList list = parseList("{1, -2, 3, 4}");
			assertThat(list.isConstant()).isTrue();
			assertThat(list.getConstantValue()).isEqualTo(List.of(1, -2, 3, 4));
		}

		@Test
		void dynamicListIsNotCached() {
			InlineList list = parseList("{1, (5 - 3), 3, 4}");
			assertThat(list.isConstant()).isFalse();
			assertThat(list.getValue(null)).isEqualTo(List.of(1, 2, 3, 4));
		}

		@Test
		void listWithVariableIsNotCached() {
			StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
			ExpressionState expressionState = new ExpressionState(evaluationContext);

			InlineList list = parseList("{1, -#num, 3, 4}");
			assertThat(list.isConstant()).isFalse();

			evaluationContext.setVariable("num", 2);
			assertThat(list.getValue(expressionState)).isEqualTo(List.of(1, -2, 3, 4));
		}

		@Test
		void listWithPropertyAccessIsNotCached() {
			StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new NumberHolder());
			ExpressionState expressionState = new ExpressionState(evaluationContext);

			InlineList list = parseList("{1, -num, 3, 4}");
			assertThat(list.isConstant()).isFalse();
			assertThat(list.getValue(expressionState)).isEqualTo(List.of(1, -99, 3, 4));

			parser.parseExpression("num = 2").getValue(evaluationContext);
			assertThat(list.getValue(expressionState)).isEqualTo(List.of(1, -2, 3, 4));
		}

		@Test
		void listCanBeCompiled() {
			SpelExpression listExpression = parseExpression("{1, -2, 3, 4}");
			assertThat(((SpelNodeImpl) listExpression.getAST()).isCompilable()).isTrue();
			assertThat(SpelCompiler.compile(listExpression)).isTrue();
		}

		@Test
		void dynamicListCannotBeCompiled() {
			SpelExpression listExpression = parseExpression("{1, (5 - 3), 3, 4}");
			assertThat(((SpelNodeImpl) listExpression.getAST()).isCompilable()).isFalse();
			assertThat(SpelCompiler.compile(listExpression)).isFalse();
		}

		private InlineList parseList(String s) {
			SpelExpression expression = parseExpression(s);
			return (InlineList) expression.getAST();
		}

	}

	@Nested
	class InlineMapTests {

		@Test
		void mapIsCached() {
			InlineMap map = parseMap("{1 : 2, 3 : 4}");
			assertThat(map.isConstant()).isTrue();
			Map<Integer, Integer> expected = Map.of(1, 2, 3, 4);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void dynamicMapIsNotCached() {
			InlineMap map = parseMap("{-1 : 2, (-2 - 1) : -4}");
			assertThat(map.isConstant()).isFalse();
			Map<Integer, Integer> expected = Map.of(-1, 2, -3, -4);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithVariableIsNotCached() {
			StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
			ExpressionState expressionState = new ExpressionState(evaluationContext);

			InlineMap map = parseMap("{-1 : 2, -3 : -#num}");
			assertThat(map.isConstant()).isFalse();

			evaluationContext.setVariable("num", 4);
			assertThat(map.getValue(expressionState)).isEqualTo(Map.of(-1, 2, -3, -4));
		}

		@Test
		void mapWithPropertyAccessIsNotCached() {
			StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new NumberHolder());
			ExpressionState expressionState = new ExpressionState(evaluationContext);

			InlineMap map = parseMap("{-1 : 2, -3 : -num}");
			assertThat(map.isConstant()).isFalse();
			assertThat(map.getValue(expressionState)).isEqualTo(Map.of(-1, 2, -3, -99));

			parser.parseExpression("num = 4").getValue(evaluationContext);
			assertThat(map.getValue(expressionState)).isEqualTo(Map.of(-1, 2, -3, -4));
		}

		@Test
		void mapWithNegativeKeysIsCached() {
			InlineMap map = parseMap("{-1 : 2, -3 : 4}");
			assertThat(map.isConstant()).isTrue();
			Map<Integer, Integer> expected = Map.of(-1, 2, -3, 4);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithNegativeValuesIsCached() {
			InlineMap map = parseMap("{1 : -2, 3 : -4}");
			assertThat(map.isConstant()).isTrue();
			Map<Integer, Integer> expected = Map.of(1, -2, 3, -4);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithNegativeLongValuesIsCached() {
			InlineMap map = parseMap("{1L : -2L, 3L : -4L}");
			assertThat(map.isConstant()).isTrue();
			Map<Long, Long> expected = Map.of(1L, -2L, 3L, -4L);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithNegativeFloatValuesIsCached() {
			InlineMap map = parseMap("{-1.0f : -2.0f, -3.0f : -4.0f}");
			assertThat(map.isConstant()).isTrue();
			Map<Float, Float> expected = Map.of(-1.0f, -2.0f, -3.0f, -4.0f);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithNegativeRealValuesIsCached() {
			InlineMap map = parseMap("{-1.0 : -2.0, -3.0 : -4.0}");
			assertThat(map.isConstant()).isTrue();
			Map<Double, Double> expected = Map.of(-1.0, -2.0, -3.0, -4.0);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		@Test
		void mapWithNegativeKeysAndNegativeValuesIsCached() {
			InlineMap map = parseMap("{-1 : -2, -3 : -4}");
			assertThat(map.isConstant()).isTrue();
			Map<Integer, Integer> expected = Map.of(-1, -2, -3, -4);
			assertThat(map.getValue(null)).isEqualTo(expected);
		}

		private InlineMap parseMap(String s) {
			SpelExpression expression = parseExpression(s);
			return (InlineMap) expression.getAST();
		}

	}


	private SpelExpression parseExpression(String s) {
		return (SpelExpression) parser.parseExpression(s);
	}


	private static class NumberHolder {
		@SuppressWarnings("unused")
		public int num = 99;
	}

}
