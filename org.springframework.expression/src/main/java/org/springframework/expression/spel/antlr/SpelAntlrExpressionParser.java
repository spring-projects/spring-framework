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
package org.springframework.expression.spel.antlr;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelExpression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.WrappedELException;
import org.springframework.expression.spel.SpelExpressionParser.SpelInternalParser;
import org.springframework.expression.spel.generated.SpringExpressionsLexer;
import org.springframework.expression.spel.generated.SpringExpressionsParser.expr_return;

/**
 * Wrap an Antlr lexer and parser.
 * 
 * @author Andy Clement
 */
public class SpelAntlrExpressionParser implements SpelInternalParser {

	private final SpringExpressionsLexer lexer;
	private final SpringExpressionsParserExtender parser;

	public SpelAntlrExpressionParser() {
		lexer = new SpringExpressionsLexerExtender();
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		parser = new SpringExpressionsParserExtender(tokens);
	} 

	/**
	 * Parse an expression string.
	 * 
	 * @param expressionString the expression to parse
	 * @param context the parser context in which to perform the parse
	 * @return a parsed expression object
	 * @throws ParseException if the expression is invalid
	 */
	public Expression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		try {
			lexer.setCharStream(new ANTLRStringStream(expressionString));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			parser.setTokenStream(tokens);
			expr_return exprReturn = parser.expr();
			SpelExpression newExpression = new SpelExpression(expressionString, (SpelNode) exprReturn.getTree());
			return newExpression;
		} catch (RecognitionException re) {
			ParseException exception = new ParseException(expressionString, "Recognition error at position: "
					+ re.charPositionInLine + ": " + re.getMessage(), re);
			throw exception;
		} catch (WrappedELException e) {
			SpelException wrappedException = e.getCause();
			throw new ParseException(expressionString, "Parsing problem: " + wrappedException.getMessage(),
					wrappedException);
		}
	}

}