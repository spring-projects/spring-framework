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
package org.springframework.expression.common;

import junit.framework.TestCase;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelExpressionParser;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * Test LiteralExpression
 * 
 * @author Andy Clement
 */
public class CompositeStringExpressionTests extends TestCase {

	public void testGetValue() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression ex = parser.parseExpression("hello ${'world'}", DefaultTemplateParserContext.INSTANCE);
		checkString("hello world", ex.getValue());
		checkString("hello world", ex.getValue(String.class));
		EvaluationContext ctx = new StandardEvaluationContext();
		checkString("hello world", ex.getValue(ctx));
		checkString("hello world", ex.getValue(ctx, String.class));
		assertEquals("hello ${'world'}", ex.getExpressionString());
		assertFalse(ex.isWritable(new StandardEvaluationContext()));
	}

	// public void testSetValue() {
	// try {
	// LiteralExpression lEx = new LiteralExpression("somevalue");
	// lEx.setValue(new StandardEvaluationContext(), "flibble");
	// fail("Should have got an exception that the value cannot be set");
	// } catch (EvaluationException ee) {
	// // success, not allowed - whilst here, check the expression value in the exception
	// assertEquals(ee.getExpressionString(), "somevalue");
	// }
	// }
	//
	// public void testGetValueType() throws Exception {
	// LiteralExpression lEx = new LiteralExpression("somevalue");
	// assertEquals(String.class, lEx.getValueType());
	// assertEquals(String.class, lEx.getValueType(new StandardEvaluationContext()));
	// }

	private void checkString(String expectedString, Object value) {
		if (!(value instanceof String)) {
			fail("Result was not a string, it was of type " + value.getClass() + "  (value=" + value + ")");
		}
		if (!((String) value).equals(expectedString)) {
			fail("Did not get expected result.  Should have been '" + expectedString + "' but was '" + value + "'");
		}
	}
}
