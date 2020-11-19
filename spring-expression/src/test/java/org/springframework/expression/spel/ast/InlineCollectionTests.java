/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Semyon Danilov
 */
public class InlineCollectionTests {

	@Test
	public void testListCached() {
		InlineList list = parseList("{1, -2, 3, 4}");
		assertThat(list.isConstant()).isTrue();
		assertThat(list.getConstantValue()).isEqualTo(Arrays.asList(1, -2, 3, 4));
	}

	@Test
	public void testDynamicListNotCached() {
		InlineList list = parseList("{1, 5-2, 3, 4}");
		assertThat(list.isConstant()).isFalse();
		assertThat(list.getValue(null)).isEqualTo(Arrays.asList(1, 3, 3, 4));
	}

	@Test
	public void testListWithVariableNotCached() {
		InlineList list = parseList("{1, -a, 3, 4}");
		assertThat(list.isConstant()).isFalse();
		final StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext(new AHolder());
		standardEvaluationContext.setVariable("a", 2);
		assertThat(list.getValue(new ExpressionState(standardEvaluationContext))).isEqualTo(Arrays.asList(1, -2, 3, 4));
	}

	@Test
	public void testListCanBeCompiled() {
		SpelExpression listExpression = parseExpression("{1, -2, 3, 4}");
		assertThat(((SpelNodeImpl) listExpression.getAST()).isCompilable()).isTrue();
		assertThat(SpelCompiler.compile(listExpression)).isTrue();
	}

	@Test
	public void testDynamicListCantBeCompiled() {
		SpelExpression listExpression = parseExpression("{1, 5-2, 3, 4}");
		assertThat(((SpelNodeImpl) listExpression.getAST()).isCompilable()).isFalse();
		assertThat(SpelCompiler.compile(listExpression)).isFalse();
	}

	@Test
	public void testMapCached() {
		InlineMap map = parseMap("{1 : 2, 3 : 4}");
		assertThat(map.isConstant()).isTrue();
		final Map<Integer, Integer> expected = new HashMap<>();
		expected.put(1, 2);
		expected.put(3, 4);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeKeyCached() {
		InlineMap map = parseMap("{-1 : 2, -3 : 4}");
		assertThat(map.isConstant()).isTrue();
		final Map<Integer, Integer> expected = new HashMap<>();
		expected.put(-1, 2);
		expected.put(-3, 4);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeValueCached() {
		InlineMap map = parseMap("{1 : -2, 3 : -4}");
		assertThat(map.isConstant()).isTrue();
		final Map<Integer, Integer> expected = new HashMap<>();
		expected.put(1, -2);
		expected.put(3, -4);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeLongTypesCached() {
		InlineMap map = parseMap("{1L : -2L, 3L : -4L}");
		assertThat(map.isConstant()).isTrue();
		final Map<Long, Long> expected = new HashMap<>();
		expected.put(1L, -2L);
		expected.put(3L, -4L);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeFloatTypesCached() {
		InlineMap map = parseMap("{-1.0f : -2.0f, -3.0f : -4.0f}");
		assertThat(map.isConstant()).isTrue();
		final Map<Float, Float> expected = new HashMap<>();
		expected.put(-1.0f, -2.0f);
		expected.put(-3.0f, -4.0f);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeRealTypesCached() {
		InlineMap map = parseMap("{-1.0 : -2.0, -3.0 : -4.0}");
		assertThat(map.isConstant()).isTrue();
		final Map<Double, Double> expected = new HashMap<>();
		expected.put(-1.0, -2.0);
		expected.put(-3.0, -4.0);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithNegativeKeyAndValueCached() {
		InlineMap map = parseMap("{-1 : -2, -3 : -4}");
		assertThat(map.isConstant()).isTrue();
		final Map<Integer, Integer> expected = new HashMap<>();
		expected.put(-1, -2);
		expected.put(-3, -4);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	@Test
	public void testMapWithDynamicNotCached() {
		InlineMap map = parseMap("{-1 : 2, -3+1 : -4}");
		assertThat(map.isConstant()).isFalse();
		final Map<Integer, Integer> expected = new HashMap<>();
		expected.put(-1, 2);
		expected.put(-2, -4);
		assertThat(map.getValue(null)).isEqualTo(expected);
	}

	private InlineMap parseMap(String s) {
		SpelExpression expression = parseExpression(s);
		return (InlineMap) expression.getAST();
	}

	private InlineList parseList(String s) {
		SpelExpression expression = parseExpression(s);
		return (InlineList) expression.getAST();
	}

	private SpelExpression parseExpression(final String s) {
		ExpressionParser parser = new SpelExpressionParser();
		return (SpelExpression) parser.parseExpression(s);
	}

	private static class AHolder {
		public int a = 2;
	}

}
