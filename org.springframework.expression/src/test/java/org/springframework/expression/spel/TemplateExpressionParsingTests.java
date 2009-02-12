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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.antlr.SpelAntlrExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class TemplateExpressionParsingTests extends ExpressionTestCase {

	public static final ParserContext DEFAULT_TEMPLATE_PARSER_CONTEXT = new ParserContext() {
		public String getExpressionPrefix() {
			return "${";
		}
		public String getExpressionSuffix() {
			return "}";
		}
		public boolean isTemplate() {
			return true;
		}
	};


	public void testParsingSimpleTemplateExpression01() throws Exception {
		SpelAntlrExpressionParser parser = new SpelAntlrExpressionParser();
		Expression expr = parser.parseExpression("hello ${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("hello world", o.toString());
	}

	public void testParsingSimpleTemplateExpression02() throws Exception {
		SpelAntlrExpressionParser parser = new SpelAntlrExpressionParser();
		Expression expr = parser.parseExpression("hello ${'to'} you", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("hello to you", o.toString());
	}

	public void testParsingSimpleTemplateExpression03() throws Exception {
		SpelAntlrExpressionParser parser = new SpelAntlrExpressionParser();
		Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
				DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("The quick brown fox jumped over the lazy dog", o.toString());
	}

	public void testCompositeStringExpression() throws Exception {
		SpelAntlrExpressionParser parser = new SpelAntlrExpressionParser();
		Expression ex = parser.parseExpression("hello ${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		checkString("hello world", ex.getValue());
		checkString("hello world", ex.getValue(String.class));
		EvaluationContext ctx = new StandardEvaluationContext();
		checkString("hello world", ex.getValue(ctx));
		checkString("hello world", ex.getValue(ctx, String.class));
		assertEquals("hello ${'world'}", ex.getExpressionString());
		assertFalse(ex.isWritable(new StandardEvaluationContext()));
	}

	private void checkString(String expectedString, Object value) {
		if (!(value instanceof String)) {
			fail("Result was not a string, it was of type " + value.getClass() + "  (value=" + value + ")");
		}
		if (!value.equals(expectedString)) {
			fail("Did not get expected result.  Should have been '" + expectedString + "' but was '" + value + "'");
		}
	}

	// TODO need to support this case but what is the neatest way? Escape the clashing delimiters in the expression
	// string?
	// public void testParsingTemplateExpressionThatEmbedsTheDelimiters() throws Exception {
	// SpelExpressionParser parser = new SpelExpressionParser();
	// Expression expr = parser.parseExpression("The quick ${{'green','brown'}.${true}} fox jumped over the ${'lazy'}
	// dog",DefaultTemplateParserContext.INSTANCE);
	// Object o = expr.getValue();
	// System.out.println(o);
	// assertEquals("The quick brown fox jumped over the lazy dog",o.toString());
	// }

}
