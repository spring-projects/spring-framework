/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void spelExpressionMapWithVariables() {
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
	void spelExpressionListWithVariables() {
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
	void spelExpressionListIndexAccessWithVariables() {
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
	void spelExpressionArrayWithVariables() {
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
