/*
 * Copyright 2004-2008 the original author or authors.
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
	}

	public void testLessThanOrEqual() {
		evaluate("3 <= 5", true, Boolean.class);
		evaluate("5 <= 3", false, Boolean.class);
		evaluate("6 <= 6", true, Boolean.class);
	}

	public void testEqual() {
		evaluate("3 == 5", false, Boolean.class);
		evaluate("5 == 3", false, Boolean.class);
		evaluate("6 == 6", true, Boolean.class);
	}

	public void testGreaterThanOrEqual() {
		evaluate("3 >= 5", false, Boolean.class);
		evaluate("5 >= 3", true, Boolean.class);
		evaluate("6 >= 6", true, Boolean.class);
	}

	public void testGreaterThan() {
		evaluate("3 > 5", false, Boolean.class);
		evaluate("5 > 3", true, Boolean.class);
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
		evaluate("3 / 1", 3, Integer.class);
		evaluate("3 % 2", 1, Integer.class);
	}

	public void testMathOperatorDivide_ConvertToDouble() {
		evaluateAndAskForReturnType("8/4", new Double(2.0), Double.class);
	}

	public void testMathOperatorDivide04_ConvertToFloat() {
		evaluateAndAskForReturnType("8/4", new Float(2.0), Float.class);
	}

	// public void testMathOperatorDivide04() {
	// evaluateAndAskForReturnType("8.4 / 4", "2", Integer.class);
	// }

	public void testDoubles() {
		evaluate("3.0d + 5.0d", 8.0d, Double.class);
		evaluate("3.0d - 5.0d", -2.0d, Double.class);
		evaluate("3.0d * 5.0d", 15.0d, Double.class);
		evaluate("3.0d / 5.0d", 0.6d, Double.class);
		evaluate("6.0d % 3.5d", 2.5d, Double.class);
	}

	public void testFloats() {
		evaluate("3.0f + 5.0f", 8.0d, Double.class);
		evaluate("3.0f - 5.0f", -2.0d, Double.class);
		evaluate("3.0f * 5.0f", 15.0d, Double.class);
		evaluate("3.0f / 5.0f", 0.6d, Double.class);
		evaluate("5.0f % 3.1f", 1.9d, Double.class);
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
	
}
