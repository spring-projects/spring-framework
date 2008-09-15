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

import org.springframework.expression.Expression;
import org.springframework.expression.common.DefaultTemplateParserContext;

/**
 * Test parsing of template expressions
 * 
 * @author Andy Clement
 */
public class TemplateExpressionParsingTests extends ExpressionTestCase {

	public void testParsingSimpleTemplateExpression01() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'world'}", DefaultTemplateParserContext.INSTANCE);
		Object o = expr.getValue();
		System.out.println(o);
		assertEquals("hello world", o.toString());
	}

	public void testParsingSimpleTemplateExpression02() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'to'} you", DefaultTemplateParserContext.INSTANCE);
		Object o = expr.getValue();
		System.out.println(o);
		assertEquals("hello to you", o.toString());
	}

	public void testParsingSimpleTemplateExpression03() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
				DefaultTemplateParserContext.INSTANCE);
		Object o = expr.getValue();
		System.out.println(o);
		assertEquals("The quick brown fox jumped over the lazy dog", o.toString());
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
