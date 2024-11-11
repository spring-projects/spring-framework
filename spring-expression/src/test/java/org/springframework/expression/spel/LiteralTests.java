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

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpression;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the evaluation of basic literals: boolean, string, integer, long,
 * hex integer, hex long, float, double, null.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class LiteralTests extends AbstractExpressionTests {

	@Test
	void booleans() {
		evaluate("false", false, Boolean.class);
		evaluate("true", true, Boolean.class);
	}

	@Test
	void strings() {
		evaluate("'hello'", "hello", String.class);
		evaluate("'joe bloggs'", "joe bloggs", String.class);
		evaluate("'Hello World'", "Hello World", String.class);
	}

	@Test
	void stringsContainingQuotes() {
		evaluate("'Tony''s Pizza'", "Tony's Pizza", String.class);
		evaluate("'Tony\\r''s Pizza'", "Tony\\r's Pizza", String.class);
		evaluate("\"Hello World\"", "Hello World", String.class);
		evaluate("\"Hello ' World\"", "Hello ' World", String.class);
	}

	@Test
	void integers() {
		evaluate("1", 1, Integer.class);
		evaluate("1415", 1415, Integer.class);
	}

	@Test
	void longs() {
		evaluate("1L", 1L, Long.class);
		evaluate("1415L", 1415L, Long.class);
	}

	@Test
	void signedIntegers() {
		evaluate("-1", -1, Integer.class);
		evaluate("-0xa", -10, Integer.class);
	}

	@Test
	void signedLongs() {
		evaluate("-1L", -1L, Long.class);
		evaluate("-0x20l", -32L, Long.class);
	}

	@Test
	void hexIntegers() {
		evaluate("0x7FFFF", 524287, Integer.class);
		evaluate("0X7FFFF", 524287, Integer.class);
	}

	@Test
	void hexLongs() {
		evaluate("0X7FFFFl", 524287L, Long.class);
		evaluate("0x7FFFFL", 524287L, Long.class);
		evaluate("0xCAFEBABEL", 3405691582L, Long.class);
	}

	@Test
	void hexLongAndIntInteractions() {
		evaluate("0x20 * 2L", 64L, Long.class);
		// ask for the result to be made into an Integer
		evaluateAndAskForReturnType("0x20 * 2L", 64, Integer.class);
		// ask for the result to be made into an Integer knowing that it will not fit
		evaluateAndCheckError("0x1220 * 0xffffffffL", Integer.class, SpelMessage.TYPE_CONVERSION_ERROR, 0);
	}

	@Test
	void floats() {
		// "f" or "F" must be explicitly specified.
		evaluate("1.25f", 1.25f, Float.class);
		evaluate("2.5f", 2.5f, Float.class);
		evaluate("-3.5f", -3.5f, Float.class);
		evaluate("1.25F", 1.25f, Float.class);
		evaluate("2.5F", 2.5f, Float.class);
		evaluate("-3.5F", -3.5f, Float.class);
	}

	@Test
	void doubles() {
		// Real numbers are Doubles by default
		evaluate("1.25", 1.25d, Double.class);
		evaluate("2.99", 2.99d, Double.class);
		evaluate("-3.141", -3.141d, Double.class);

		// But "d" or "D" can also be explicitly specified.
		evaluate("1.25d", 1.25d, Double.class);
		evaluate("2.99d", 2.99d, Double.class);
		evaluate("-3.141d", -3.141d, Double.class);
		evaluate("1.25D", 1.25d, Double.class);
		evaluate("2.99D", 2.99d, Double.class);
		evaluate("-3.141D", -3.141d, Double.class);
	}

	@Test
	void doublesUsingExponents() {
		evaluate("6.0221415E+23", "6.0221415E23", Double.class);
		evaluate("6.0221415e+23", "6.0221415E23", Double.class);
		evaluate("6.0221415E+23d", "6.0221415E23", Double.class);
		evaluate("6.0221415e+23D", "6.0221415E23", Double.class);
		evaluate("6E2f", 6E2f, Float.class);
	}

	@Test
	void doublesUsingExponentsWithInvalidInput() {
		parseAndCheckError("6.1e23e22", SpelMessage.MORE_INPUT, 6, "e22");
		parseAndCheckError("6.1f23e22", SpelMessage.MORE_INPUT, 4, "23e22");
	}

	@Test
	void nullLiteral() {
		evaluate("null", null, null);
	}

	@Test
	void conversions() {
		// getting the expression type to be what we want - either:
		evaluate("37.byteValue", (byte) 37, Byte.class); // calling byteValue() on Integer.class
		evaluateAndAskForReturnType("37", (byte) 37, Byte.class); // relying on registered type converters
	}

	@Test
	void notWritable() {
		SpelExpression expr = (SpelExpression) parser.parseExpression("37");
		assertThat(expr.isWritable(context)).isFalse();

		expr = (SpelExpression) parser.parseExpression("37L");
		assertThat(expr.isWritable(context)).isFalse();

		expr = (SpelExpression) parser.parseExpression("true");
		assertThat(expr.isWritable(context)).isFalse();
	}

}
