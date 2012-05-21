/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * Tests the evaluation of expressions using relational operators.
 *
 * @author Andy Clement
 */
public class OperatorTests extends ExpressionTestCase {

	@Test
	public void testIntegerLiteral() {
		evaluate("3", 3, Integer.class);
	}

	@Test
	public void testRealLiteral() {
		evaluate("3.5", 3.5d, Double.class);
	}

	@Test
	public void testLessThan() {
		
		evaluate("5 < 5", false, Boolean.class);		
		evaluate("3 < 5", true, Boolean.class);
		evaluate("5 < 3", false, Boolean.class);
		evaluate("3L < 5L", true, Boolean.class);
		evaluate("5L < 3L", false, Boolean.class);
		evaluate("3.0d < 5.0d", true, Boolean.class);
		evaluate("5.0d < 3.0d", false, Boolean.class);
		evaluate("'abc' < 'def'",true,Boolean.class);
		evaluate("'def' < 'abc'",false,Boolean.class);
		evaluate("new java.math.BigDecimal('3') < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') < new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') < 5", true, Boolean.class);
		evaluate("3L < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L < new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d < new java.math.BigDecimal('3.0')", false, Boolean.class);

		evaluate("3 lt 5", true, Boolean.class);
		evaluate("5 lt 3", false, Boolean.class);
		evaluate("3L lt 5L", true, Boolean.class);
		evaluate("5L lt 3L", false, Boolean.class);
		evaluate("3.0d lT 5.0d", true, Boolean.class);
		evaluate("5.0d Lt 3.0d", false, Boolean.class);
		evaluate("'abc' LT 'def'",true,Boolean.class);
		evaluate("'def' lt 'abc'",false,Boolean.class);
		evaluate("new java.math.BigDecimal('3') lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('5') lt new java.math.BigDecimal('3')", false, Boolean.class);
		evaluate("3 lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("new java.math.BigDecimal('3') lt 5", true, Boolean.class);
		evaluate("3L lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('5')", true, Boolean.class);
		evaluate("3L lt new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('3.1')", true, Boolean.class);
		evaluate("3.0d lt new java.math.BigDecimal('3.0')", false, Boolean.class);
	}

	@Test
	public void testLessThanOrEqual() {
		evaluate("3 <= 5", true, Boolean.class);
		evaluate("5 <= 3", false, Boolean.class);
		evaluate("6 <= 6", true, Boolean.class);
		evaluate("3L <= 5L", true, Boolean.class);
		evaluate("5L <= 3L", false, Boolean.class);
		evaluate("5L <= 5L", true, Boolean.class);
		evaluate("3.0d <= 5.0d", true, Boolean.class);
		evaluate("5.0d <= 3.0d", false, Boolean.class);
		evaluate("5.0d <= 5.0d", true, Boolean.class);
		evaluate("'abc' <= 'def'",true,Boolean.class);
		evaluate("'def' <= 'abc'",false,Boolean.class);
		evaluate("'abc' <= 'abc'",true,Boolean.class);
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

		evaluate("3 le 5", true, Boolean.class);
		evaluate("5 le 3", false, Boolean.class);
		evaluate("6 Le 6", true, Boolean.class);
		evaluate("3L lE 5L", true, Boolean.class);
		evaluate("5L LE 3L", false, Boolean.class);
		evaluate("5L le 5L", true, Boolean.class);
		evaluate("3.0d LE 5.0d", true, Boolean.class);
		evaluate("5.0d lE 3.0d", false, Boolean.class);
		evaluate("5.0d Le 5.0d", true, Boolean.class);
		evaluate("'abc' Le 'def'",true,Boolean.class);
		evaluate("'def' LE 'abc'",false,Boolean.class);
		evaluate("'abc' le 'abc'",true,Boolean.class);
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
	}

	@Test
	public void testEqual() {
		evaluate("3 == 5", false, Boolean.class);
		evaluate("5 == 3", false, Boolean.class);
		evaluate("6 == 6", true, Boolean.class);
		evaluate("3.0f == 5.0f", false, Boolean.class);
		evaluate("3.0f == 3.0f", true, Boolean.class);
		evaluate("'abc' == null", false, Boolean.class);
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

		evaluate("3 eq 5", false, Boolean.class);
		evaluate("5 eQ 3", false, Boolean.class);
		evaluate("6 Eq 6", true, Boolean.class);
		evaluate("3.0f eq 5.0f", false, Boolean.class);
		evaluate("3.0f EQ 3.0f", true, Boolean.class);
		evaluate("'abc' EQ null", false, Boolean.class);
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
	}

	@Test
	public void testNotEqual() {
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
	}

	@Test
	public void testGreaterThanOrEqual() {
		evaluate("3 >= 5", false, Boolean.class);
		evaluate("5 >= 3", true, Boolean.class);
		evaluate("6 >= 6", true, Boolean.class);
		evaluate("3L >= 5L", false, Boolean.class);
		evaluate("5L >= 3L", true, Boolean.class);
		evaluate("5L >= 5L", true, Boolean.class);
		evaluate("3.0d >= 5.0d", false, Boolean.class);
		evaluate("5.0d >= 3.0d", true, Boolean.class);
		evaluate("5.0d <= 5.0d", true, Boolean.class);
		evaluate("'abc' >= 'def'",false,Boolean.class);
		evaluate("'def' >= 'abc'",true,Boolean.class);
		evaluate("'abc' >= 'abc'",true,Boolean.class);
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

		evaluate("3 GE 5", false, Boolean.class);
		evaluate("5 gE 3", true, Boolean.class);
		evaluate("6 Ge 6", true, Boolean.class);
		evaluate("3L ge 5L", false, Boolean.class);
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
	}

	@Test
	public void testGreaterThan() {
		evaluate("3 > 5", false, Boolean.class);
		evaluate("5 > 3", true, Boolean.class);
		evaluate("3L > 5L", false, Boolean.class);
		evaluate("5L > 3L", true, Boolean.class);
		evaluate("3.0d > 5.0d", false, Boolean.class);
		evaluate("5.0d > 3.0d", true, Boolean.class);
		evaluate("'abc' > 'def'",false,Boolean.class);
		evaluate("'def' > 'abc'",true,Boolean.class);
		evaluate("new java.math.BigDecimal('3') > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') > new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') > 5", false, Boolean.class);
		evaluate("3L > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L > new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d > new java.math.BigDecimal('3.0')", false, Boolean.class);

		evaluate("3.0d gt 5.0d", false, Boolean.class);
		evaluate("5.0d gT 3.0d", true, Boolean.class);
		evaluate("'abc' Gt 'def'",false,Boolean.class);
		evaluate("'def' GT 'abc'",true,Boolean.class);
		evaluate("new java.math.BigDecimal('3') gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('5') gt new java.math.BigDecimal('3')", true, Boolean.class);
		evaluate("3 gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("new java.math.BigDecimal('3') gt 5", false, Boolean.class);
		evaluate("3L gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('5')", false, Boolean.class);
		evaluate("3L gt new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('3.1')", false, Boolean.class);
		evaluate("3.0d gt new java.math.BigDecimal('3.0')", false, Boolean.class);
	}

	@Test
	public void testMultiplyStringInt() {
		evaluate("'a' * 5", "aaaaa", String.class);
	}

	@Test
	public void testMultiplyDoubleDoubleGivesDouble() {
		evaluate("3.0d * 5.0d", 15.0d, Double.class);
	}

	@Test
	public void testMixedOperandsBigDecimal() {
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
	public void testMathOperatorAdd02() {
		evaluate("'hello' + ' ' + 'world'", "hello world", String.class);
	}

	@Test
	public void testMathOperatorsInChains() {
		evaluate("1+2+3",6,Integer.class);
		evaluate("2*3*4",24,Integer.class);
		evaluate("12-1-2",9,Integer.class);
	}

	@Test
	public void testIntegerArithmetic() {
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
	public void testPlus() throws Exception {
		evaluate("7 + 2", "9", Integer.class);
		evaluate("3.0f + 5.0f", 8.0f, Float.class);
		evaluate("3.0d + 5.0d", 8.0d, Double.class);
		evaluate("3 + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);

		evaluate("'ab' + 2", "ab2", String.class);
		evaluate("2 + 'a'", "2a", String.class);
		evaluate("'ab' + null", "abnull", String.class);
		evaluate("null + 'ab'", "nullab", String.class);

		// AST:
		SpelExpression expr = (SpelExpression)parser.parseExpression("+3");
		assertEquals("+3",expr.toStringAST());
		expr = (SpelExpression)parser.parseExpression("2+3");
		assertEquals("(2 + 3)",expr.toStringAST());

		// use as a unary operator
		evaluate("+5d",5d,Double.class);
		evaluate("+5L",5L,Long.class);
		evaluate("+5",5,Integer.class);
		evaluate("+new java.math.BigDecimal('5')", new BigDecimal("5"),BigDecimal.class);
		evaluateAndCheckError("+'abc'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);

		// string concatenation
		evaluate("'abc'+'def'","abcdef",String.class);

		//
		evaluate("5 + new Integer('37')",42,Integer.class);
	}

	@Test
	public void testMinus() throws Exception {
		evaluate("'c' - 2", "a", String.class);
		evaluate("3.0f - 5.0f", -2.0f, Float.class);
		evaluateAndCheckError("'ab' - 2", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("2-'ab'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		SpelExpression expr = (SpelExpression)parser.parseExpression("-3");
		assertEquals("-3",expr.toStringAST());
		expr = (SpelExpression)parser.parseExpression("2-3");
		assertEquals("(2 - 3)",expr.toStringAST());

		evaluate("-5d",-5d,Double.class);
		evaluate("-5L",-5L,Long.class);
		evaluate("-5",-5,Integer.class);
		evaluate("-new java.math.BigDecimal('5')", new BigDecimal("-5"),BigDecimal.class);
		evaluateAndCheckError("-'abc'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	public void testModulus() {
		evaluate("3%2",1,Integer.class);
		evaluate("3L%2L",1L,Long.class);
		evaluate("3.0f%2.0f",1f,Float.class);
		evaluate("5.0d % 3.1d", 1.9d, Double.class);
		evaluate("new java.math.BigDecimal('5') % new java.math.BigDecimal('3')", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') % 3", new BigDecimal("2"), BigDecimal.class);
		evaluateAndCheckError("'abc'%'def'",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	public void testDivide() {
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
	public void testMathOperatorDivide_ConvertToDouble() {
		evaluateAndAskForReturnType("8/4", new Double(2.0), Double.class);
	}

	@Test
	public void testMathOperatorDivide04_ConvertToFloat() {
		evaluateAndAskForReturnType("8/4", new Float(2.0), Float.class);
	}

	@Test
	public void testDoubles() {
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
	public void testBigDecimals() {
		evaluate("3 + new java.math.BigDecimal('5')", new BigDecimal("8"), BigDecimal.class);
		evaluate("3 - new java.math.BigDecimal('5')", new BigDecimal("-2"), BigDecimal.class);
		evaluate("3 * new java.math.BigDecimal('5')", new BigDecimal("15"), BigDecimal.class);
		evaluate("3 / new java.math.BigDecimal('5')", new BigDecimal("1"), BigDecimal.class);
		evaluate("5 % new java.math.BigDecimal('3')", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') % 3", new BigDecimal("2"), BigDecimal.class);
		evaluate("new java.math.BigDecimal('5') ^ 3", new BigDecimal("125"), BigDecimal.class);
	}
	
	@Test
	public void testOperatorNames() throws Exception {
		Operator node = getOperatorNode((SpelExpression)parser.parseExpression("1==3"));
		assertEquals("==",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("1!=3"));
		assertEquals("!=",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3/3"));
		assertEquals("/",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3+3"));
		assertEquals("+",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3-3"));
		assertEquals("-",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3<4"));
		assertEquals("<",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3<=4"));
		assertEquals("<=",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3*4"));
		assertEquals("*",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3%4"));
		assertEquals("%",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3>=4"));
		assertEquals(">=",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3 between 4"));
		assertEquals("between",node.getOperatorName());

		node = getOperatorNode((SpelExpression)parser.parseExpression("3 ^ 4"));
		assertEquals("^",node.getOperatorName());
	}

	@Test
	public void testOperatorOverloading() {
		evaluateAndCheckError("'a' * '2'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("'a' ^ '2'", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	@Test
	public void testPower() {
		evaluate("3^2",9,Integer.class);
		evaluate("3.0d^2.0d",9.0d,Double.class);
		evaluate("3L^2L",9L,Long.class);
		evaluate("(2^32)^2",9223372036854775807L,Long.class);
		evaluate("new java.math.BigDecimal('5') ^ 3", new BigDecimal("125"), BigDecimal.class);
	}

	@Test
	public void testMixedOperands_FloatsAndDoubles() {
		evaluate("3.0d + 5.0f", 8.0d, Double.class);
		evaluate("3.0D - 5.0f", -2.0d, Double.class);
		evaluate("3.0f * 5.0d", 15.0d, Double.class);
		evaluate("3.0f / 5.0D", 0.6d, Double.class);
		evaluate("5.0D % 3f", 2.0d, Double.class);
	}

	@Test
	public void testMixedOperands_DoublesAndInts() {
		evaluate("3.0d + 5", 8.0d, Double.class);
		evaluate("3.0D - 5", -2.0d, Double.class);
		evaluate("3.0f * 5", 15.0f, Float.class);
		evaluate("6.0f / 2", 3.0f, Float.class);
		evaluate("6.0f / 4", 1.5f, Float.class);
		evaluate("5.0D % 3", 2.0d, Double.class);
		evaluate("5.5D % 3", 2.5, Double.class);
	}

	@Test
	public void testStrings() {
		evaluate("'abc' == 'abc'",true,Boolean.class);
		evaluate("'abc' == 'def'",false,Boolean.class);
		evaluate("'abc' != 'abc'",false,Boolean.class);
		evaluate("'abc' != 'def'",true,Boolean.class);
	}

	@Test
	public void testLongs() {
		evaluate("3L == 4L", false, Boolean.class);
		evaluate("3L == 3L", true, Boolean.class);
		evaluate("3L != 4L", true, Boolean.class);
		evaluate("3L != 3L", false, Boolean.class);
		evaluate("3L * 50L", 150L, Long.class);
		evaluate("3L + 50L", 53L, Long.class);
		evaluate("3L - 50L", -47L, Long.class);
	}

	// ---

	private Operator getOperatorNode(SpelExpression e) {
		SpelNode node = e.getAST();
		return (Operator)findNode(node,Operator.class);
	}

	private SpelNode findNode(SpelNode node, Class<Operator> clazz) {
		if (clazz.isAssignableFrom(node.getClass())) {
			return node;
		}
		int childCount = node.getChildCount();
		for (int i=0;i<childCount;i++) {
			SpelNode possible = findNode(node.getChild(i),clazz);
			if (possible!=null) {
				return possible;
			}
		}
		return null;
	}

}
