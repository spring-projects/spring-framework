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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.expression.spel.SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED;
import static org.springframework.expression.spel.SpelMessage.MAX_REPEATED_TEXT_SIZE_EXCEEDED;
import static org.springframework.expression.spel.SpelMessage.NEGATIVE_REPEATED_TEXT_COUNT;

/**
 * Tests the evaluation of expressions using various operators.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 */
class OperatorTests extends AbstractExpressionTests {

	@Test
	void equal() {
		evaluate("3 == 5", false, Boolean.class);
		evaluate("5 == 3", false, Boolean.class);
		evaluate("6 == 6", true, Boolean.class);
		evaluate("3.0f == 5.0f", false, Boolean.class);
		evaluate("3.0f == 3.0f", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') == new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') == new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') == new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 == new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') == 5", false, Boolean.class);
		evaluate("3L == new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d == new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L == new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d == new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d == new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("3.0f == 3.0d", true, Boolean.class);
		evaluate("10 == '10'", false, Boolean.class);
		evaluate("'abc' == 'abc'", true, Boolean.class);
		evaluate("'abc' == new java.lang.StringBuilder('abc')", true, Boolean.class);
		evaluate("'abc' == 'def'", false, Boolean.class);
		evaluate("'abc' == null", false, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable(0) == new org.springframework.expression.spel.OperatorTests$OtherSubComparable(0)", true, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable(1) < new org.springframework.expression.spel.OperatorTests$OtherSubComparable(2)", true, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable(2) > new org.springframework.expression.spel.OperatorTests$OtherSubComparable(1)", true, Boolean.class);

		evaluate("3 eq 5", false, Boolean.class);
		evaluate("5 eQ 3", false, Boolean.class);
		evaluate("6 Eq 6", true, Boolean.class);
		evaluate("3.0f eq 5.0f", false, Boolean.class);
		evaluate("3.0f EQ 3.0f", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') eq new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') eq new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') eq new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 eq new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') eq 5", false, Boolean.class);
		evaluate("3L eq new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d eq new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L eq new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d eq new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d eq new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("3.0f eq 3.0d", true, Boolean.class);
		evaluate("10 eq '10'", false, Boolean.class);
		evaluate("'abc' eq 'abc'", true, Boolean.class);
		evaluate("'abc' eq new java.lang.StringBuilder('abc')", true, Boolean.class);
		evaluate("'abc' eq 'def'", false, Boolean.class);
		evaluate("'abc' eq null", false, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable() eq new org.springframework.expression.spel.OperatorTests$OtherSubComparable()", true, Boolean.class);
	}

	@Test
	void notEqual() {
		evaluate("3 != 5", true, Boolean.class);
		evaluate("5 != 3", true, Boolean.class);
		evaluate("6 != 6", false, Boolean.class);
		evaluate("3.0f != 5.0f", true, Boolean.class);
		evaluate("3.0f != 3.0f", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') != new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') != new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') != new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 != new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') != 5", true, Boolean.class);
		evaluate("3L != new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d != new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L != new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d != new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d != new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("3.0f != 3.0d", false, Boolean.class);
		evaluate("10 != '10'", true, Boolean.class);
		evaluate("'abc' != 'abc'", false, Boolean.class);
		evaluate("'abc' != new java.lang.StringBuilder('abc')", false, Boolean.class);
		evaluate("'abc' != 'def'", true, Boolean.class);
		evaluate("'abc' != null", true, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable() != new org.springframework.expression.spel.OperatorTests$OtherSubComparable()", false, Boolean.class);

		evaluate("3 ne 5", true, Boolean.class);
		evaluate("5 nE 3", true, Boolean.class);
		evaluate("6 Ne 6", false, Boolean.class);
		evaluate("3.0f NE 5.0f", true, Boolean.class);
		evaluate("3.0f ne 3.0f", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') ne new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') ne new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') ne new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 ne new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') ne 5", true, Boolean.class);
		evaluate("3L ne new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d ne new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L ne new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d ne new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d ne new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("3.0f ne 3.0d", false, Boolean.class);
		evaluate("10 ne '10'", true, Boolean.class);
		evaluate("'abc' ne 'abc'", false, Boolean.class);
		evaluate("'abc' ne new java.lang.StringBuilder('abc')", false, Boolean.class);
		evaluate("'abc' ne 'def'", true, Boolean.class);
		evaluate("'abc' ne null", true, Boolean.class);
		evaluate("new org.springframework.expression.spel.OperatorTests$SubComparable() ne new org.springframework.expression.spel.OperatorTests$OtherSubComparable()", false, Boolean.class);
	}

	@Test
	void lessThan() {
		evaluate("5 < 5", false, Boolean.class);
		evaluate("3 < 5", true, Boolean.class);
		evaluate("5 < 3", false, Boolean.class);
		evaluate("3L < 5L", true, Boolean.class);
		evaluate("5L < 3L", false, Boolean.class);
		evaluate("3.0d < 5.0d", true, Boolean.class);
		evaluate("5.0d < 3.0d", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') < new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') < 5", true, Boolean.class);
		evaluate("3L < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L < new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("'abc' < 'def'", true, Boolean.class);
		evaluate("'abc' < new java.lang.StringBuilder('def')", true, Boolean.class);
		evaluate("'def' < 'abc'", false, Boolean.class);

		evaluate("3 lt 5", true, Boolean.class);
		evaluate("5 lt 3", false, Boolean.class);
		evaluate("3L lt 5L", true, Boolean.class);
		evaluate("5L lt 3L", false, Boolean.class);
		evaluate("3.0d lT 5.0d", true, Boolean.class);
		evaluate("5.0d Lt 3.0d", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') lt new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') lt 5", true, Boolean.class);
		evaluate("3L lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L lt new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("'abc' LT 'def'", true, Boolean.class);
		evaluate("'abc' lt new java.lang.StringBuilder('def')", true, Boolean.class);
		evaluate("'def' lt 'abc'", false, Boolean.class);
	}

	@Test
	void lessThanOrEqual() {
		evaluate("3 <= 5", true, Boolean.class);
		evaluate("5 <= 3", false, Boolean.class);
		evaluate("6 <= 6", true, Boolean.class);
		evaluate("3L <= 5L", true, Boolean.class);
		evaluate("5L <= 3L", false, Boolean.class);
		evaluate("5L <= 5L", true, Boolean.class);
		evaluate("3.0d <= 5.0d", true, Boolean.class);
		evaluate("5.0d <= 3.0d", false, Boolean.class);
		evaluate("5.0d <= 5.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') <= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') <= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') <= new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 <= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') <= 5", true, Boolean.class);
		evaluate("3L <= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d <= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L <= new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d <= new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d <= new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("'abc' <= 'def'", true, Boolean.class);
		evaluate("'def' <= 'abc'", false, Boolean.class);
		evaluate("'abc' <= 'abc'", true, Boolean.class);

		evaluate("3 le 5", true, Boolean.class);
		evaluate("5 le 3", false, Boolean.class);
		evaluate("6 Le 6", true, Boolean.class);
		evaluate("3L lE 5L", true, Boolean.class);
		evaluate("5L LE 3L", false, Boolean.class);
		evaluate("5L le 5L", true, Boolean.class);
		evaluate("3.0d LE 5.0d", true, Boolean.class);
		evaluate("5.0d lE 3.0d", false, Boolean.class);
		evaluate("5.0d Le 5.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') le new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') le new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') le new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 le new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') le 5", true, Boolean.class);
		evaluate("3L le new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d le new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L le new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d le new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d le new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("'abc' Le 'def'", true, Boolean.class);
		evaluate("'def' LE 'abc'", false, Boolean.class);
		evaluate("'abc' le 'abc'", true, Boolean.class);
	}

	@Test
	void greaterThan() {
		evaluate("3 > 5", false, Boolean.class);
		evaluate("5 > 3", true, Boolean.class);
		evaluate("3L > 5L", false, Boolean.class);
		evaluate("5L > 3L", true, Boolean.class);
		evaluate("3.0d > 5.0d", false, Boolean.class);
		evaluate("5.0d > 3.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') > new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') > 5", false, Boolean.class);
		evaluate("3L > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L > new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("'abc' > 'def'", false, Boolean.class);
		evaluate("'abc' > new java.lang.StringBuilder('def')", false, Boolean.class);
		evaluate("'def' > 'abc'", true, Boolean.class);

		evaluate("3 gt 5", false, Boolean.class);
		evaluate("5 gt 3", true, Boolean.class);
		evaluate("3L gt 5L", false, Boolean.class);
		evaluate("5L gt 3L", true, Boolean.class);
		evaluate("3.0d gt 5.0d", false, Boolean.class);
		evaluate("5.0d gT 3.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') gt new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') gt 5", false, Boolean.class);
		evaluate("3L gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L gt new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('3.0')", false, Boolean.class);
		evaluate("'abc' Gt 'def'", false, Boolean.class);
		evaluate("'abc' gt new java.lang.StringBuilder('def')", false, Boolean.class);
		evaluate("'def' GT 'abc'", true, Boolean.class);
	}

	@Test
	void greaterThanOrEqual() {
		evaluate("3 >= 5", false, Boolean.class);
		evaluate("5 >= 3", true, Boolean.class);
		evaluate("6 >= 6", true, Boolean.class);
		evaluate("3L >= 5L", false, Boolean.class);
		evaluate("5L >= 3L", true, Boolean.class);
		evaluate("5L >= 5L", true, Boolean.class);
		evaluate("3.0d >= 5.0d", false, Boolean.class);
		evaluate("5.0d >= 3.0d", true, Boolean.class);
		evaluate("5.0d >= 5.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') >= new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') >= new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') >= new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 >= new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') >= 5", false, Boolean.class);
		evaluate("3L >= new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d >= new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L >= new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d >= new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d >= new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("'abc' >= 'def'", false, Boolean.class);
		evaluate("'def' >= 'abc'", true, Boolean.class);
		evaluate("'abc' >= 'abc'", true, Boolean.class);

		evaluate("3 GE 5", false, Boolean.class);
		evaluate("5 gE 3", true, Boolean.class);
		evaluate("6 Ge 6", true, Boolean.class);
		evaluate("3L ge 5L", false, Boolean.class);
		evaluate("5L ge 3L", true, Boolean.class);
		evaluate("5L ge 5L", true, Boolean.class);
		evaluate("3.0d ge 5.0d", false, Boolean.class);
		evaluate("5.0d ge 3.0d", true, Boolean.class);
		evaluate("5.0d ge 5.0d", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') ge new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') ge new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') ge new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 ge new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') ge 5", false, Boolean.class);
		evaluate("3L ge new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d ge new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L ge new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d ge new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d ge new java.math.BigDecimal('3.0')", true, Boolean.class);
		evaluate("'abc' ge 'def'", false, Boolean.class);
		evaluate("'def' ge 'abc'", true, Boolean.class);
		evaluate("'abc' ge 'abc'", true, Boolean.class);
	}

	@Test
	void integerLiteral() {
		evaluate("3", 3, Integer.class);
	}

	@Test
	void realLiteral() {
		evaluate("3.5", 3.5d, Double.class);
	}

	@Test
	void multiplyDoubleDoubleGivesDouble() {
		evaluate("3.0d * 5.0d", 15.0d, Double.class);
	}

	@Test
	void mixedOperandsBigDecimal() {
		evaluate("3 * new java.math.BigDecimal('5')", new BigDecimal("15"), BigDecimal.class);
		evaluate("3L * new java.math.BigDecimal('5')", new BigDecimal("15"), BigDecimal.class);
		evaluate("3.0d * new java.math.BigDecimal('5')", new BigDecimal("15.0"), BigDecimal.class);

		evaluate("3 + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);
		evaluate("3L + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);
		evaluate("3.0d + new java.math.BigDecimal('5')", new BigDecimal("8.0"), BigDecimal.class);

		evaluate("3 - new java.math.BigDecimal('5')", new BigDecimal("-2"), BigDecimal.class);
		evaluate("3L - new java.math.BigDecimal('5')", new BigDecimal("-2"), BigDecimal.class);
		evaluate("3.0d - new java.math.BigDecimal('5')", new BigDecimal("-2.0"), BigDecimal.class);

		evaluate("3 / new java.math.BigDecimal('5')", new BigDecimal("1"), BigDecimal.class);
		evaluate("3 / new java.math.BigDecimal('5.0')", new BigDecimal("0.6"), BigDecimal.class);
		evaluate("3 / new java.math.BigDecimal('5.00')", new BigDecimal("0.60"), BigDecimal.class);
		evaluate("3L / new java.math.BigDecimal('5.0')", new BigDecimal("0.6"), BigDecimal.class);
		evaluate("3.0d / new java.math.BigDecimal('5.0')", new BigDecimal("0.6"), BigDecimal.class);

		evaluate("5 % new java.math.BigDecimal('3')", new BigDecimal("2"), BigDecimal.class);
		evaluate("3 % new java.math.BigDecimal('5')", new BigDecimal("3"), BigDecimal.class);
		evaluate("3L % new java.math.BigDecimal('5')", new BigDecimal("3"), BigDecimal.class);
		evaluate("3.0d % new java.math.BigDecimal('5')", new BigDecimal("3.0"), BigDecimal.class);
	}

	@Test
	void mathOperatorAdd02() {
		evaluate("'hello' + ' ' + 'world'", "hello world", String.class);
	}

	@Test
	void mathOperatorsInChains() {
		evaluate("1+2+3",6,Integer.class);
		evaluate("2*3*4",24,Integer.class);
		evaluate("12-1-2",9,Integer.class);
	}

	@Test
	void integerArithmetic() {
		evaluate("2 + 4", "6", Integer.class);
		evaluate("5 - 4", "1", Integer.class);
		evaluate("3 * 5", 15, Integer.class);
		evaluate("3.2d * 5", 16.0d, Double.class);
		evaluate("3 * 5f", 15f, Float.class);
		evaluate("3 / 1", 3, Integer.class);
		evaluate("3 % 2", 1, Integer.class);
		evaluate("3 mod 2", 1, Integer.class);
		evaluate("3 mOd 2", 1, Integer.class);
		evaluate("3 Mod 2", 1, Integer.class);
		evaluate("3 MOD 2", 1, Integer.class);
	}

	@Test
	void plus() {
		evaluate("7 + 2", "9", Integer.class);
		evaluate("3.0f + 5.0f", 8.0f, Float.class);
		evaluate("3.0d + 5.0d", 8.0d, Double.class);
		evaluate("3 + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);
		evaluate("5 + new Integer('37')", 42, Integer.class);

		// AST:
		SpelExpression expr = (SpelExpression) parser.parseExpression("+3");
		assertThat(expr.toStringAST()).isEqualTo("+3");
		expr = (SpelExpression) parser.parseExpression("2+3");
		assertThat(expr.toStringAST()).isEqualTo("(2 + 3)");

		// use as a unary operator
		evaluate("+5d", 5d, Double.class);
		evaluate("+5L", 5L, Long.class);
		evaluate("+5", 5, Integer.class);
		evaluate("+new java.math.BigDecimal('5')", new BigDecimal("5"), BigDecimal.class);
		evaluateAndCheckError("+'abc'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	void minus() {
		evaluate("'c' - 2", "a", String.class);
		evaluate("3.0f - 5.0f", -2.0f, Float.class);
		evaluateAndCheckError("'ab' - 2", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("2-'ab'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		SpelExpression expr = (SpelExpression) parser.parseExpression("-3");
		assertThat(expr.toStringAST()).isEqualTo("-3");
		expr = (SpelExpression) parser.parseExpression("2-3");
		assertThat(expr.toStringAST()).isEqualTo("(2 - 3)");

		evaluate("-5d", -5d, Double.class);
		evaluate("-5L", -5L, Long.class);
		evaluate("-5", -5, Integer.class);
		evaluate("-new java.math.BigDecimal('5')", new BigDecimal("-5"), BigDecimal.class);
		evaluateAndCheckError("-'abc'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	void modulus() {
		evaluate("3%2",1,Integer.class);
		evaluate("3L%2L",1L,Long.class);
		evaluate("3.0f%2.0f",1f,Float.class);
		evaluate("5.0d % 3.1d", 1.9d, Double.class);
		evaluate("new java.math.BigDecimal('5') % new java.math.BigDecimal('3')", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') % 3", new BigDecimal("2"), BigDecimal.class);
		evaluateAndCheckError("'abc'%'def'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	void divide() {
		evaluate("3.0f / 5.0f", 0.6f, Float.class);
		evaluate("4L/2L",2L,Long.class);
		evaluate("3.0f div 5.0f", 0.6f, Float.class);
		evaluate("4L DIV 2L",2L,Long.class);
		evaluate("new java.math.BigDecimal('3') / 5", new BigDecimal("1"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('3.0') / 5", new BigDecimal("0.6"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('3.00') / 5", new BigDecimal("0.60"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('3.00') / new java.math.BigDecimal('5.0000')", new BigDecimal("0.6000"), BigDecimal.class);
		evaluateAndCheckError("'abc'/'def'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	void mathOperatorDivide_ConvertToDouble() {
		evaluateAndAskForReturnType("8/4", 2.0, Double.class);
	}

	@Test
	void mathOperatorDivide04_ConvertToFloat() {
		evaluateAndAskForReturnType("8/4", 2.0F, Float.class);
	}

	@Test
	void doubles() {
		evaluate("3.0d == 5.0d", false, Boolean.class);
		evaluate("3.0d == 3.0d", true, Boolean.class);
		evaluate("3.0d != 5.0d", true, Boolean.class);
		evaluate("3.0d != 3.0d", false, Boolean.class);
		evaluate("3.0d + 5.0d", 8.0d, Double.class);
		evaluate("3.0d - 5.0d", -2.0d, Double.class);
		evaluate("3.0d * 5.0d", 15.0d, Double.class);
		evaluate("3.0d / 5.0d", 0.6d, Double.class);
		evaluate("6.0d % 3.5d", 2.5d, Double.class);
	}

	@Test
	void bigDecimals() {
		evaluate("3 + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);
		evaluate("3 - new java.math.BigDecimal('5')", new BigDecimal("-2"), BigDecimal.class);
		evaluate("3 * new java.math.BigDecimal('5')", new BigDecimal("15"), BigDecimal.class);
		evaluate("3 / new java.math.BigDecimal('5')", new BigDecimal("1"), BigDecimal.class);
		evaluate("5 % new java.math.BigDecimal('3')", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') % 3", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') ^ 3", new BigDecimal("125"), BigDecimal.class);
	}

	@Test
	void operatorNames() {
		Operator node = getOperatorNode((SpelExpression)parser.parseExpression("1==3"));
		assertThat(node.getOperatorName()).isEqualTo("==");

		node = getOperatorNode((SpelExpression)parser.parseExpression("1!=3"));
		assertThat(node.getOperatorName()).isEqualTo("!=");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3/3"));
		assertThat(node.getOperatorName()).isEqualTo("/");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3+3"));
		assertThat(node.getOperatorName()).isEqualTo("+");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3-3"));
		assertThat(node.getOperatorName()).isEqualTo("-");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3<4"));
		assertThat(node.getOperatorName()).isEqualTo("<");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3<=4"));
		assertThat(node.getOperatorName()).isEqualTo("<=");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3*4"));
		assertThat(node.getOperatorName()).isEqualTo("*");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3%4"));
		assertThat(node.getOperatorName()).isEqualTo("%");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3>=4"));
		assertThat(node.getOperatorName()).isEqualTo(">=");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3 between 4"));
		assertThat(node.getOperatorName()).isEqualTo("between");

		node = getOperatorNode((SpelExpression)parser.parseExpression("3 ^ 4"));
		assertThat(node.getOperatorName()).isEqualTo("^");
	}

	@Test
	void operatorOverloading() {
		evaluateAndCheckError("'a' * '2'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("'a' ^ '2'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	void power() {
		evaluate("3^2", 9, Integer.class);
		evaluate("3.0d^2.0d", 9.0d, Double.class);
		evaluate("3L^2L", 9L, Long.class);
		evaluate("(2^32)^2", 9223372036854775807L, Long.class);
		evaluate("new java.math.BigDecimal('5') ^ 3", new BigDecimal("125"), BigDecimal.class);
	}

	@Test
	void mixedOperands_FloatsAndDoubles() {
		evaluate("3.0d + 5.0f", 8.0d, Double.class);
		evaluate("3.0D - 5.0f", -2.0d, Double.class);
		evaluate("3.0f * 5.0d", 15.0d, Double.class);
		evaluate("3.0f / 5.0D", 0.6d, Double.class);
		evaluate("5.0D % 3f", 2.0d, Double.class);
	}

	@Test
	void mixedOperands_DoublesAndInts() {
		evaluate("3.0d + 5", 8.0d, Double.class);
		evaluate("3.0D - 5", -2.0d, Double.class);
		evaluate("3.0f * 5", 15.0f, Float.class);
		evaluate("6.0f / 2", 3.0f, Float.class);
		evaluate("6.0f / 4", 1.5f, Float.class);
		evaluate("5.0D % 3", 2.0d, Double.class);
		evaluate("5.5D % 3", 2.5, Double.class);
	}

	@Test
	void strings() {
		evaluate("'abc' == 'abc'", true, Boolean.class);
		evaluate("'abc' == 'def'", false, Boolean.class);
		evaluate("'abc' != 'abc'", false, Boolean.class);
		evaluate("'abc' != 'def'", true, Boolean.class);
	}

	@Test
	void stringRepeat() {
		evaluate("'abc' * 0", "", String.class);
		evaluate("'abc' * 1", "abc", String.class);
		evaluate("'abc' * 2", "abcabc", String.class);

		Expression expr = parser.parseExpression("'a' * 256");
		assertThat(expr.getValue(context, String.class)).hasSize(256);

		// 4 is the position of the '*' (repeat operator)
		evaluateAndCheckError("'a' * 257", String.class, MAX_REPEATED_TEXT_SIZE_EXCEEDED, 4);

		// Integer overflow: 2 * ((Integer.MAX_VALUE / 2) + 1) --> integer overflow
		int repeatCount = (Integer.MAX_VALUE / 2) + 1;
		assertThat(2 * repeatCount).isNegative();
		// 5 is the position of the '*' (repeat operator)
		evaluateAndCheckError("'ab' * " + repeatCount, String.class, MAX_REPEATED_TEXT_SIZE_EXCEEDED, 5);
	}

	@Test
	void stringRepeatWithNegativeRepeatCount() {
		// 4 is the position of the '*' (repeat operator)
		// -1 is the negative repeat count
		evaluateAndCheckError("'a' * -1", String.class, NEGATIVE_REPEATED_TEXT_COUNT, 4, -1);
	}

	@Test
	void stringConcatenation() {
		evaluate("'' + ''", "", String.class);
		evaluate("'' + null", "null", String.class);
		evaluate("null + ''", "null", String.class);
		evaluate("'ab' + null", "abnull", String.class);
		evaluate("null + 'ab'", "nullab", String.class);
		evaluate("'ab' + 2", "ab2", String.class);
		evaluate("2 + 'ab'", "2ab", String.class);
		evaluate("'abc' + 'def'", "abcdef", String.class);

		// Text is big but not too big
		final int maxSize = 100_000;
		context.setVariable("text1", createString(maxSize));
		Expression expr = parser.parseExpression("#text1 + ''");
		assertThat(expr.getValue(context, String.class)).hasSize(maxSize);

		expr = parser.parseExpression("'' + #text1");
		assertThat(expr.getValue(context, String.class)).hasSize(maxSize);

		context.setVariable("text1", createString(maxSize / 2));
		expr = parser.parseExpression("#text1 + #text1");
		assertThat(expr.getValue(context, String.class)).hasSize(maxSize);

		// Text is too big
		context.setVariable("text1", createString(maxSize + 1));
		evaluateAndCheckError("#text1 + ''", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("#text1 + true", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("'' + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 3);
		evaluateAndCheckError("true + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 5);

		context.setVariable("text1", createString(maxSize / 2));
		context.setVariable("text2", createString((maxSize / 2) + 1));
		evaluateAndCheckError("#text1 + #text2", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("#text1 + #text2 + true", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("#text1 + true + #text2", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 14);
		evaluateAndCheckError("true + #text1 + #text2", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 14);

		evaluateAndCheckError("#text2 + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("#text2 + #text1 + true", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
		evaluateAndCheckError("#text2 + true + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 14);
		evaluateAndCheckError("true + #text2 + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 14);

		context.setVariable("text1", createString((maxSize / 3) + 1));
		evaluateAndCheckError("#text1 + #text1 + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 16);
		evaluateAndCheckError("(#text1 + #text1) + #text1", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 18);
		evaluateAndCheckError("#text1 + (#text1 + #text1)", String.class, MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, 7);
	}

	private static String createString(int size) {
		return new String(new char[size]);
	}

	@Test
	void longs() {
		evaluate("3L == 4L", false, Boolean.class);
		evaluate("3L == 3L", true, Boolean.class);
		evaluate("3L != 4L", true, Boolean.class);
		evaluate("3L != 3L", false, Boolean.class);
		evaluate("3L * 50L", 150L, Long.class);
		evaluate("3L + 50L", 53L, Long.class);
		evaluate("3L - 50L", -47L, Long.class);
	}

	@Test
	void bigIntegers() {
		evaluate("3 + new java.math.BigInteger('5')", new BigInteger("8"), BigInteger.class);
		evaluate("3 - new java.math.BigInteger('5')", new BigInteger("-2"), BigInteger.class);
		evaluate("3 * new java.math.BigInteger('5')", new BigInteger("15"), BigInteger.class);
		evaluate("3 / new java.math.BigInteger('5')", new BigInteger("0"), BigInteger.class);
		evaluate("5 % new java.math.BigInteger('3')", new BigInteger("2"), BigInteger.class);
		evaluate("new java.math.BigInteger('5') % 3", new BigInteger("2"), BigInteger.class);
		evaluate("new java.math.BigInteger('5') ^ 3", new BigInteger("125"), BigInteger.class);
	}

	@Test
	void bigIntFunction() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Method method = BigInteger.class.getMethod("valueOf", long.class);
		context.registerFunction("bigInt", method);

		Expression expression = parser.parseExpression("3 + #bigInt(5)");
		BigInteger result = expression.getValue(context, BigInteger.class);
		assertThat(result).isEqualTo(BigInteger.valueOf(8));
	}


	private Operator getOperatorNode(SpelExpression expr) {
		SpelNode node = expr.getAST();
		return findOperator(node);
	}

	private Operator findOperator(SpelNode node) {
		if (node instanceof Operator operator) {
			return operator;
		}
		int childCount = node.getChildCount();
		for (int i = 0; i < childCount; i++) {
			Operator possible = findOperator(node.getChild(i));
			if (possible != null) {
				return possible;
			}
		}
		return null;
	}


	static class BaseComparable implements Comparable<BaseComparable> {

		private int id;

		public BaseComparable() {
			this.id = 0;
		}

		public BaseComparable(int id) {
			this.id = id;
		}

		@Override
		public int compareTo(BaseComparable other) {
			return this.id - other.id;
		}
	}


	static class SubComparable extends BaseComparable {
		public SubComparable() {
		}

		public SubComparable(int id) {
			super(id);
		}
	}


	static class OtherSubComparable extends BaseComparable {
		public OtherSubComparable() {
		}

		public OtherSubComparable(int id) {
			super(id);
		}
	}

}
