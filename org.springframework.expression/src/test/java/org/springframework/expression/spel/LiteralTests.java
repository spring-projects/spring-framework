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
 * Tests the evaluation of basic literals: boolean, integer, hex integer, long, real, null, date
 * 
 * @author Andy Clement
 */
public class LiteralTests extends ExpressionTestCase {

	public void testLiteralBoolean01() {
		evaluate("false", "false", Boolean.class);
	}

	public void testLiteralBoolean02() {
		evaluate("true", "true", Boolean.class);
	}

	public void testLiteralInteger01() {
		evaluate("1", "1", Integer.class);
	}

	public void testLiteralInteger02() {
		evaluate("1415", "1415", Integer.class);
	}

	public void testLiteralString01() {
		evaluate("'Hello World'", "Hello World", String.class);
	}

	public void testLiteralString02() {
		evaluate("'joe bloggs'", "joe bloggs", String.class);
	}

	public void testLiteralString03() {
		evaluate("'hello'", "hello", String.class);
	}

	public void testLiteralString04() {
		evaluate("'Tony''s Pizza'", "Tony's Pizza", String.class);
	}

	public void testLiteralString05() {
		evaluate("\"Hello World\"", "Hello World", String.class);
	}

	public void testLiteralString06() {
		evaluate("\"Hello ' World\"", "Hello ' World", String.class);
	}

	public void testHexIntLiteral01() {
		evaluate("0x7FFFF", "524287", Integer.class);
		evaluate("0x7FFFFL", 524287L, Long.class);
		evaluate("0X7FFFF", "524287", Integer.class);
		evaluate("0X7FFFFl", 524287L, Long.class);
	}

	public void testLongIntLiteral01() {
		evaluate("0xCAFEBABEL", 3405691582L, Long.class);
	}

	public void testLongIntInteractions01() {
		evaluate("0x20 * 2L", 64L, Long.class);
		// ask for the result to be made into an Integer
		evaluateAndAskForReturnType("0x20 * 2L", 64, Integer.class);
		// ask for the result to be made into an Integer knowing that it will not fit
		evaluateAndCheckError("0x1220 * 0xffffffffL", Integer.class, SpelMessages.PROBLEM_DURING_TYPE_CONVERSION, -1,
				"long value '19928648248800' cannot be represented as an int");
	}

	public void testSignedIntLiterals() {
		evaluate("-1", -1, Integer.class);
		evaluate("-0xa", -10, Integer.class);
		evaluate("-1L", -1L, Long.class);
		evaluate("-0x20l", -32L, Long.class);
	}

	public void testLiteralReal01() {
		evaluate("6.0221415E+23", "6.0221415E23", Double.class);
	}

	public void testLiteralNull01() {
		evaluate("null", null, null);
	}

	// TODO 3 'default' format for date varies too much, we need to standardize on a format for EL
	// public void testLiteralDate01() {
	// eval("date('3-Feb-2008 4:50:20 PM').getTime()>0", "true", Boolean.class);
	// }

	public void testLiteralDate02() {
		evaluate("date('19740824131030','yyyyMMddHHmmss').getHours()", "13", Integer.class);
	}

}
