/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.spel.support.StandardTypeConverter;

/**
 * Tests the evaluation of real boolean expressions, these use AND, OR, NOT, TRUE, FALSE
 *
 * @author Andy Clement
 * @author Oliver Becker
 */
class BooleanExpressionTests extends AbstractExpressionTests {

	@Test
	void booleanTrue() {
		evaluate("true", Boolean.TRUE, Boolean.class);
	}

	@Test
	void booleanFalse() {
		evaluate("false", Boolean.FALSE, Boolean.class);
	}

	@Test
	void or() {
		evaluate("false or false", Boolean.FALSE, Boolean.class);
		evaluate("false or true", Boolean.TRUE, Boolean.class);
		evaluate("true or false", Boolean.TRUE, Boolean.class);
		evaluate("true or true", Boolean.TRUE, Boolean.class);
	}

	@Test
	void and() {
		evaluate("false and false", Boolean.FALSE, Boolean.class);
		evaluate("false and true", Boolean.FALSE, Boolean.class);
		evaluate("true and false", Boolean.FALSE, Boolean.class);
		evaluate("true and true", Boolean.TRUE, Boolean.class);
	}

	@Test
	void not() {
		evaluate("!false", Boolean.TRUE, Boolean.class);
		evaluate("!true", Boolean.FALSE, Boolean.class);

		evaluate("not false", Boolean.TRUE, Boolean.class);
		evaluate("NoT true", Boolean.FALSE, Boolean.class);
	}

	@Test
	void combinations01() {
		evaluate("false and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or true", Boolean.TRUE, Boolean.class);
		evaluate("true and false or false", Boolean.FALSE, Boolean.class);
	}

	@Test
	void writability() {
		evaluate("true and true", Boolean.TRUE, Boolean.class, false);
		evaluate("true or true", Boolean.TRUE, Boolean.class, false);
		evaluate("!false", Boolean.TRUE, Boolean.class, false);
	}

	@Test
	void booleanErrors01() {
		evaluateAndCheckError("1.0 or false", SpelMessage.TYPE_CONVERSION_ERROR, 0);
		evaluateAndCheckError("false or 39.4", SpelMessage.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError("true and 'hello'", SpelMessage.TYPE_CONVERSION_ERROR, 9);
		evaluateAndCheckError(" 'hello' and 'goodbye'", SpelMessage.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("!35.2", SpelMessage.TYPE_CONVERSION_ERROR, 1);
		evaluateAndCheckError("! 'foob'", SpelMessage.TYPE_CONVERSION_ERROR, 2);
	}

	@Test
	void convertAndHandleNull() { // SPR-9445
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
