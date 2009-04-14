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

import org.springframework.expression.Expression;
import org.springframework.expression.spel.antlr.SpelAntlrExpressionParser;

/**
 * Tests based on Jiras up to the release of Spring 3.0.0
 * 
 * @author Andy Clement
 */
public class SpringEL300Tests extends ExpressionTestCase {

	public void testNPE_5661() {
		evaluate("joinThreeStrings('a',null,'c')", "anullc", String.class);
	}
	
	public void testNPE_5673() throws Exception {
		SpelAntlrExpressionParser parser = new SpelAntlrExpressionParser();
		Expression ex = parser.parseExpression("#{'Unable to render embedded object: File ({#this == 2\\}'}", TemplateExpressionParsingTests.HASH_DELIMITED_PARSER_CONTEXT);
		assertEquals("Unable to render embedded object: File ({#this == 2}",ex.getValue());
//		ex = parser.parseExpression("Unable to render embedded object: File (#{#this}) not found", TemplateExpressionParsingTests.HASH_DELIMITED_PARSER_CONTEXT);
//		assertEquals()
//		System.out.println(ex.getValue(new StandardEvaluationContext(new File("C:/temp"))));
	}

}
