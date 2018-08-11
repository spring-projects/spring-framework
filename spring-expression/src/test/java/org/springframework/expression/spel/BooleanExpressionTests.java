/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.spel.support.StandardTypeConverter;

/**
 * Tests the evaluation of real boolean expressions, these use AND, OR, NOT, TRUE, FALSE
 *
 * @author Andy Clement
 * @author Oliver Becker
 */
public class BooleanExpressionTests extends AbstractExpressionTests {

	@Test
	public void testBooleanTrue() {
		evaluate("true", Boolean.TRUE, Boolean.class);
	}

	@Test
	public void testBooleanFalse() {
		evaluate("false", Boolean.FALSE, Boolean.class);
	}

	@Test
	public void testOr() {
		evaluate("false or false", Boolean.FALSE, Boolean.class);
		evaluate("false or true", Boolean.TRUE, Boolean.class);
		evaluate("true or false", Boolean.TRUE, Boolean.class);
		evaluate("true or true", Boolean.TRUE, Boolean.class);
	}

	@Test
	public void testAnd() {
		evaluate("false and false", Boolean.FALSE, Boolean.class);
		evaluate("false and true", Boolean.FALSE, Boolean.class);
		evaluate("true and false", Boolean.FALSE, Boolean.class);
		evaluate("true and true", Boolean.TRUE, Boolean.class);
	}

	@Test
	public void testNot() {
		evaluate("!false", Boolean.TRUE, Boolean.class);
		evaluate("!true", Boolean.FALSE, Boolean.class);

		evaluate("not false", Boolean.TRUE, Boolean.class);
		evaluate("NoT true", Boolean.FALSE, Boolean.class);
	}

	@Test
	public void testCombinations01() {
		evaluate("false and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or false", Boolean.FALSE, Boolean.class);
	}

	@Test
	public void testWritability() {
		evaluate("true and true", Boolean.TRUE, Boolean.class, false);
		evaluate("true or true", Boolean.TRUE, Boolean.class, false);
		evaluate("!false", Boolean.TRUE, Boolean.class, false);
	}

	@Test
	public void testBooleanErrors01() {
		evaluateAndCheckError("1.0 or false", SpelMessage.TYPE_CONVERSION_ERROR, 0);
		evaluateAndCheckError("false or 39.4", SpelMessage.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError("true and 'hello'", SpelMessage.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError(" 'hello' and 'goodbye'", SpelMessage.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("!35.2", SpelMessage.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("! 'foob'", SpelMessage.TYPE_CONVERSION_ERROR, 2);
	}

	@Test
	public void testConvertAndHandleNull() { // SPR-9445
		// without null conversion
		evaluateAndCheckError("null or true", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");
		evaluateAndCheckError("null and true", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");
		evaluateAndCheckError("!null", SpelMessage.TYPE_CONVERSION_ERROR, 1, "null", "boolean");
		evaluateAndCheckError("null ? 'foo' : 'bar'", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");

		// with null conversion (null -> false)
		GenericConversionService conversionService = new GenericConversionService() {
			@Override
			protected Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
				return targetType.getType() == Boolean.class ? false : null;
			}
		};
		context.setTypeConverter(new StandardTypeConverter(conversionService));

		evaluate("null or true", Boolean.TRUE, Boolean.class, false);
		evaluate("null and true", Boolean.FALSE, Boolean.class, false);
		evaluate("!null", Boolean.TRUE, Boolean.class, false);
		evaluate("null ? 'foo' : 'bar'", "bar", String.class, false);
	}

}
