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

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * SpelEvaluationException tests (SPR-16544).
 *
 * @author Juergen Hoeller
 * @author DJ Kulkarni
 */
class SpelExceptionTests {

	@Test
	void spelExpressionMapNullVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aMap.containsKey('one')");
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
				spelExpression::getValue);
	}

	@Test
	void spelExpressionMapIndexAccessNullVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aMap['one'] eq 1");
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
				spelExpression::getValue);
	}

	@Test
	@SuppressWarnings("serial")
	public void spelExpressionMapWithVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aMap['one'] eq 1");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariables(new HashMap<>() {
			{
				put("aMap", new HashMap<String, Integer>() {
					{
						put("one", 1);
						put("two", 2);
						put("three", 3);
					}
				});

			}
		});
		boolean result = spelExpression.getValue(ctx, Boolean.class);
		assertThat(result).isTrue();

	}

	@Test
	void spelExpressionListNullVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aList.contains('one')");
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
				spelExpression::getValue);
	}

	@Test
	void spelExpressionListIndexAccessNullVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aList[0] eq 'one'");
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
				spelExpression::getValue);
	}

	@Test
	@SuppressWarnings("serial")
	public void spelExpressionListWithVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aList.contains('one')");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariables(new HashMap<>() {
			{
				put("aList", new ArrayList<String>() {
					{
						add("one");
						add("two");
						add("three");
					}
				});

			}
		});
		boolean result = spelExpression.getValue(ctx, Boolean.class);
		assertThat(result).isTrue();
	}

	@Test
	@SuppressWarnings("serial")
	public void spelExpressionListIndexAccessWithVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#aList[0] eq 'one'");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariables(new HashMap<>() {
			{
				put("aList", new ArrayList<String>() {
					{
						add("one");
						add("two");
						add("three");
					}
				});

			}
		});
		boolean result = spelExpression.getValue(ctx, Boolean.class);
		assertThat(result).isTrue();
	}

	@Test
	void spelExpressionArrayIndexAccessNullVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#anArray[0] eq 1");
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
				spelExpression::getValue);
	}

	@Test
	@SuppressWarnings("serial")
	public void spelExpressionArrayWithVariables() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression spelExpression = parser.parseExpression("#anArray[0] eq 1");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariables(new HashMap<>() {
			{
				put("anArray", new int[] {1,2,3});
			}
		});
		boolean result = spelExpression.getValue(ctx, Boolean.class);
		assertThat(result).isTrue();
	}

}
