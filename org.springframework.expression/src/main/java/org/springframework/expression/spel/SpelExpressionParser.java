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
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.DefaultNonTemplateParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.antlr.SpelAntlrExpressionParser;

/**
 * Instances of this parser class can process Spring Expression Language format expressions. The result of parsing an
 * expression is a SpelExpression instance that can be repeatedly evaluated (possibly against different evaluation
 * contexts) or serialized for later evaluation.
 * 
 * @author Andy Clement
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private final SpelInternalParser expressionParser;

	public SpelExpressionParser() {
		// Use an Antlr based expression parser
		expressionParser = new SpelAntlrExpressionParser();
	}

	/**
	 * Parse an expression string.
	 * 
	 * @param expressionString the expression to parse
	 * @param context the parser context in which to perform the parse
	 * @return a parsed expression object
	 * @throws ParseException if the expression is invalid
	 */
	@Override
	protected Expression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		return expressionParser.doParseExpression(expressionString,context);
	}

	/**
	 * Simple override with covariance to return a nicer type
	 */
	@Override
	public SpelExpression parseExpression(String expressionString) throws ParseException {
		return (SpelExpression) super.parseExpression(expressionString, DefaultNonTemplateParserContext.INSTANCE);
	}

	public interface SpelInternalParser {
		Expression doParseExpression(String expressionString, ParserContext context) throws ParseException;
	}
}