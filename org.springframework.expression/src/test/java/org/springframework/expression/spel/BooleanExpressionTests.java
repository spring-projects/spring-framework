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
 * Tests the evaluation of real boolean expressions, these use AND, OR, NOT, TRUE, FALSE
 * 
 * @author Andy Clement
 */
public class BooleanExpressionTests extends ExpressionTestCase {

	public void testBooleanTrue() {
		evaluate("true", Boolean.TRUE, Boolean.class);
	}

	public void testBooleanFalse() {
		evaluate("false", Boolean.FALSE, Boolean.class);
	}

	public void testOr() {
		evaluate("false or false", Boolean.FALSE, Boolean.class);
		evaluate("false or true", Boolean.TRUE, Boolean.class);
		evaluate("true or false", Boolean.TRUE, Boolean.class);
		evaluate("true or true", Boolean.TRUE, Boolean.class);
	}

	public void testAnd() {
		evaluate("false and false", Boolean.FALSE, Boolean.class);
		evaluate("false and true", Boolean.FALSE, Boolean.class);
		evaluate("true and false", Boolean.FALSE, Boolean.class);
		evaluate("true and true", Boolean.TRUE, Boolean.class);
	}

	public void testNot() {
		evaluate("!false", Boolean.TRUE, Boolean.class);
		evaluate("!true", Boolean.FALSE, Boolean.class);
	}

	public void testCombinations01() {
		evaluate("false and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or false", Boolean.FALSE, Boolean.class);
	}

	public void testWritability() {
		evaluate("true and true", Boolean.TRUE, Boolean.class, false);
		evaluate("true or true", Boolean.TRUE, Boolean.class, false);
		evaluate("!false", Boolean.TRUE, Boolean.class, false);
	}

	public void testBooleanErrors01() {
		evaluateAndCheckError("1.0 or false", SpelMessages.TYPE_CONVERSION_ERROR, 0);
		evaluateAndCheckError("false or 39.4", SpelMessages.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError("true and 'hello'", SpelMessages.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError(" 'hello' and 'goodbye'", SpelMessages.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("!35.2", SpelMessages.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("! 'foob'", SpelMessages.TYPE_CONVERSION_ERROR, 2);
	}
}
