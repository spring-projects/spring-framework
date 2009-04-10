/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.expression.spel.ast.Operator;

/**
 * Tests the evaluation of expressions using relational operators.
 * 
 * @author Andy Clement
 */
public class OperatorTests extends ExpressionTestCase {

	public void testIntegerLiteral() {
		evaluate("3", 3, Integer.class);
	}

	public void testRealLiteral() {
		evaluate("3.5", 3.5d, Double.class);
	}

	public void testLessThan() {
		evaluate("3 < 5", true, Boolean.class);
		evaluate("5 < 3", false, Boolean.class);
		evaluate("3L < 5L", true, Boolean.class);
		evaluate("5L < 3L", false, Boolean.class);
		evaluate("3.0d < 5.0d", true, Boolean.class);
		evaluate("5.0d < 3.0d", false, Boolean.class);
		evaluate("'abc' < 'def'",true,Boolean.class);
		evaluate("'def' < 'abc'",false,Boolean.class);
	}

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
	}

	public void testEqual() {
		evaluate("3 == 5", false, Boolean.class);
		evaluate("5 == 3", false, Boolean.class);
		evaluate("6 == 6", true, Boolean.class);
		evaluate("3.0f == 5.0f", false, Boolean.class);
		evaluate("3.0f == 3.0f", true, Boolean.class);
		evaluate("'abc' == null", false, Boolean.class);
	}

	public void testNotEqual() {
		evaluate("3 != 5", true, Boolean.class);
		evaluate("5 != 3", true, Boolean.class);
		evaluate("6 != 6", false, Boolean.class);
		evaluate("3.0f != 5.0f", true, Boolean.class);
		evaluate("3.0f != 3.0f", false, Boolean.class);
	}

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

	}

	public void testGreaterThan() {
		evaluate("3 > 5", false, Boolean.class);
		evaluate("5 > 3", true, Boolean.class);		
		evaluate("3L > 5L", false, Boolean.class);
		evaluate("5L > 3L", true, Boolean.class);
		evaluate("3.0d > 5.0d", false, Boolean.class);
		evaluate("5.0d > 3.0d", true, Boolean.class);
		evaluate("'abc' > 'def'",false,Boolean.class);
		evaluate("'def' > 'abc'",true,Boolean.class);
	}

	public void testMultiplyStringInt() {
		evaluate("'a' * 5", "aaaaa", String.class);
	}

	public void testMultiplyDoubleDoubleGivesDouble() {
		evaluate("3.0d * 5.0d", 15.0d, Double.class);
	}

	public void testMathOperatorAdd02() {
		evaluate("'hello' + ' ' + 'world'", "hello world", String.class);
	}

	public void testIntegerArithmetic() {
		evaluate("2 + 4", "6", Integer.class);
		evaluate("5 - 4", "1", Integer.class);
		evaluate("3 * 5", 15, Integer.class);
		evaluate("3.2d * 5", 16.0d, Double.class);
		evaluate("3 * 5f", 15d, Double.class);
		evaluate("3 / 1", 3, Integer.class);
		evaluate("3 % 2", 1, Integer.class);
	}
	
	public void testPlus() throws Exception {
		evaluate("7 + 2", "9", Integer.class);
		evaluate("3.0f + 5.0f", 8.0d, Double.class);
		evaluate("3.0d + 5.0d", 8.0d, Double.class);

		evaluateAndCheckError("'ab' + 2", SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("2+'a' ", SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("2+'ab'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		
		// AST:
		SpelExpression expr = (SpelExpression)parser.parseExpression("+3");
		assertEquals("+3",expr.toStringAST());
		expr = (SpelExpression)parser.parseExpression("2+3");
		assertEquals("(2 + 3)",expr.toStringAST());
		
		// use as a unary operator
		evaluate("+5d",5d,Double.class);
		evaluate("+5L",5L,Long.class);
		evaluate("+5",5,Integer.class);
		evaluateAndCheckError("+'abc'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		
		// string concatenation
		evaluate("'abc'+'def'","abcdef",String.class);
		
		// 
		evaluate("5 + new Integer('37')",42,Integer.class);
	}
	
	public void testMinus() throws Exception {
		evaluate("'c' - 2", "a", String.class);
		evaluate("3.0f - 5.0f", -2.0d, Double.class);
		evaluateAndCheckError("'ab' - 2", SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("2-'ab'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		SpelExpression expr = (SpelExpression)parser.parseExpression("-3");
		assertEquals("-3",expr.toStringAST());
		expr = (SpelExpression)parser.parseExpression("2-3");
		assertEquals("(2 - 3)",expr.toStringAST());
		
		evaluate("-5d",-5d,Double.class);
		evaluate("-5L",-5L,Long.class);
		evaluate("-5",-5,Integer.class);
		evaluateAndCheckError("-'abc'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}
	
	public void testModulus() {
		evaluate("3%2",1,Integer.class);
		evaluate("3L%2L",1L,Long.class);
		evaluate("3.0d%2.0d",1d,Double.class);
		evaluate("5.0f % 3.1f", 1.9d, Double.class);
		evaluateAndCheckError("'abc'%'def'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}

	public void testDivide() {
		evaluate("3.0f / 5.0f", 0.6d, Double.class);
		evaluate("4L/2L",2L,Long.class);
		evaluateAndCheckError("'abc'/'def'",SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}
	
	public void testMathOperatorDivide_ConvertToDouble() {
		evaluateAndAskForReturnType("8/4", new Double(2.0), Double.class);
	}

	public void testMathOperatorDivide04_ConvertToFloat() {
		evaluateAndAskForReturnType("8/4", new Float(2.0), Float.class);
	}

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
	
	public void testOperatorOverloading() {
		evaluateAndCheckError("'a' * '2'", SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
		evaluateAndCheckError("'a' ^ '2'", SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);
	}
	
	public void testPower() {
		evaluate("3^2",9,Integer.class);
		evaluate("3.0d^2.0d",9.0d,Double.class);
		evaluate("3L^2L",9L,Long.class);
		evaluate("(2^32)^2",9223372036854775807L,Long.class);
	}
	
	public void testMixedOperands_FloatsAndDoubles() {
		evaluate("3.0d + 5.0f", 8.0d, Double.class);
		evaluate("3.0D - 5.0f", -2.0d, Double.class);
		evaluate("3.0f * 5.0d", 15.0d, Double.class);
		evaluate("3.0f / 5.0D", 0.6d, Double.class);
		evaluate("5.0D % 3.1f", 1.9d, Double.class);		
	}
	
	public void testMixedOperands_DoublesAndInts() {
		evaluate("3.0d + 5", 8.0d, Double.class);
		evaluate("3.0D - 5", -2.0d, Double.class);
		evaluate("3.0f * 5", 15.0d, Double.class);
		evaluate("6.0f / 2", 3.0, Double.class);
		evaluate("6.0f / 4", 1.5d, Double.class);
		evaluate("5.0D % 3", 2.0d, Double.class);		
		evaluate("5.5D % 3", 2.5, Double.class);		
	}
	
	public void testStrings() {
		evaluate("'abc' == 'abc'",true,Boolean.class);
		evaluate("'abc' == 'def'",false,Boolean.class);
		evaluate("'abc' != 'abc'",false,Boolean.class);
		evaluate("'abc' != 'def'",true,Boolean.class);
	}
	
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
