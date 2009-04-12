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

package org.springframework.expression.spel.antlr;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelExpression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.WrappedSpelException;
import org.springframework.expression.spel.generated.SpringExpressionsLexer;
import org.springframework.expression.spel.generated.SpringExpressionsParser.expr_return;

/**
 * Default {@link org.springframework.expression.ExpressionParser} implementation,
 * wrapping an Antlr lexer and parser that implements standard Spring EL syntax.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SpelAntlrExpressionParser extends TemplateAwareExpressionParser {

	private final SpringExpressionsLexer lexer;

	private final SpringExpressionsParserExtender parser;


	public SpelAntlrExpressionParser() {
		this.lexer = new SpringExpressionsLexerExtender();
		CommonTokenStream tokens = new CommonTokenStream(this.lexer);
		this.parser = new SpringExpressionsParserExtender(tokens);
	} 


	/**
	 * Parse an expression string.
	 * @param expressionString the expression to parse
	 * @param context the parser context in which to perform the parse
	 * @return a parsed expression object
	 * @throws ParseException if the expression is invalid
	 */
	protected Expression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		try {
			this.lexer.setCharStream(new ANTLRStringStream(expressionString));
			CommonTokenStream tokens = new CommonTokenStream(this.lexer);
			this.parser.setTokenStream(tokens);
			expr_return exprReturn = this.parser.expr();
			return new SpelExpression(expressionString, (SpelNode) exprReturn.getTree());
		} catch (RecognitionException re) {
			throw new ParseException(expressionString,
					"Recognition error at position: " + re.charPositionInLine + ": " + re.getMessage(), re);
		} catch (WrappedSpelException ex) {
			SpelException wrappedException = ex.getCause();
			throw new ParseException(expressionString,
					"Parsing problem: " + wrappedException.getMessage(), wrappedException);
		}
	}

}
