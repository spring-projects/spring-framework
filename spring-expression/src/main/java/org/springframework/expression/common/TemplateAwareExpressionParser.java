/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;

/**
 * An expression parser that understands templates. It can be subclassed by expression
 * parsers that do not offer first class support for templating.
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	/**
	 * Default ParserContext instance for non-template expressions.
	 */
	private static final ParserContext NON_TEMPLATE_PARSER_CONTEXT = new ParserContext() {

		@Override
		public String getExpressionPrefix() {
			return null;
		}

		@Override
		public String getExpressionSuffix() {
			return null;
		}

		@Override
		public boolean isTemplate() {
			return false;
		}
	};

	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, NON_TEMPLATE_PARSER_CONTEXT);
	}

	@Override
	public Expression parseExpression(String expressionString, ParserContext context)
			throws ParseException {
		if (context == null) {
			context = NON_TEMPLATE_PARSER_CONTEXT;
		}

		if (context.isTemplate()) {
			return parseTemplate(expressionString, context);
		}
		else {
			return doParseExpression(expressionString, context);
		}
	}

	private Expression parseTemplate(String expressionString, ParserContext context)
			throws ParseException {
		if (expressionString.length() == 0) {
			return new LiteralExpression("");
		}
		Expression[] expressions = parseExpressions(expressionString, context);
		if (expressions.length == 1) {
			return expressions[0];
		}
		else {
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * Helper that parses given expression string using the configured parser. The
	 * expression string can contain any number of expressions all contained in "${...}"
	 * markers. For instance: "foo${expr0}bar${expr1}". The static pieces of text will
	 * also be returned as Expressions that just return that static piece of text. As a
	 * result, evaluating all returned expressions and concatenating the results produces
	 * the complete evaluated string. Unwrapping is only done of the outermost delimiters
	 * found, so the string 'hello ${foo${abc}}' would break into the pieces 'hello ' and
	 * 'foo${abc}'. This means that expression languages that used ${..} as part of their
	 * functionality are supported without any problem. The parsing is aware of the
	 * structure of an embedded expression. It assumes that parentheses '(', square
	 * brackets '[' and curly brackets '}' must be in pairs within the expression unless
	 * they are within a string literal and a string literal starts and terminates with a
	 * single quote '.
	 * @param expressionString the expression string
	 * @return the parsed expressions
	 * @throws ParseException when the expressions cannot be parsed
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context)
			throws ParseException {
		List<Expression> expressions = new LinkedList<Expression>();
		String prefix = context.getExpressionPrefix();
		String suffix = context.getExpressionSuffix();
		int startIdx = 0;
		while (startIdx < expressionString.length()) {
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				// an inner expression was found - this is a composite
				if (prefixIndex > startIdx) {
					expressions.add(createLiteralExpression(context,
							expressionString.substring(startIdx, prefixIndex)));
				}
				int afterPrefixIndex = prefixIndex + prefix.length();
				int suffixIndex = skipToCorrectEndSuffix(prefix, suffix,
						expressionString, afterPrefixIndex);

				if (suffixIndex == -1) {
					throw new ParseException(expressionString, prefixIndex,
							"No ending suffix '" + suffix
									+ "' for expression starting at character "
									+ prefixIndex + ": "
									+ expressionString.substring(prefixIndex));
				}

				if (suffixIndex == afterPrefixIndex) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix
									+ "' at character " + prefixIndex);
				}

				String expr = expressionString.substring(prefixIndex + prefix.length(),
						suffixIndex);
				expr = expr.trim();

				if (expr.length() == 0) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix
									+ "' at character " + prefixIndex);
				}

				expressions.add(doParseExpression(expr, context));
				startIdx = suffixIndex + suffix.length();
			}
			else {
				// no more ${expressions} found in string, add rest as static text
				expressions.add(createLiteralExpression(context,
						expressionString.substring(startIdx)));
				startIdx = expressionString.length();
			}
		}
		return expressions.toArray(new Expression[expressions.size()]);
	}

	private Expression createLiteralExpression(ParserContext context, String text) {
		return new LiteralExpression(text);
	}

	/**
	 * Return true if the specified suffix can be found at the supplied position in the
	 * supplied expression string.
	 * @param expressionString the expression string which may contain the suffix
	 * @param pos the start position at which to check for the suffix
	 * @param suffix the suffix string
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			// the expressionString ran out before the suffix could entirely be found
			return false;
		}
		return true;
	}

	/**
	 * Copes with nesting, for example '${...${...}}' where the correct end for the first
	 * ${ is the final }.
	 * @param prefix the prefix
	 * @param suffix the suffix
	 * @param expressionString the expression string
	 * @param afterPrefixIndex the most recently found prefix location for which the
	 *        matching end suffix is being sought
	 * @return the position of the correct matching nextSuffix or -1 if none can be found
	 */
	private int skipToCorrectEndSuffix(String prefix, String suffix,
			String expressionString, int afterPrefixIndex) throws ParseException {
		// Chew on the expression text - relying on the rules:
		// brackets must be in pairs: () [] {}
		// string literals are "..." or '...' and these may contain unmatched brackets
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // the suffix is missing
		}
		Stack<Bracket> stack = new Stack<Bracket>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				break;
			}
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{':
				case '[':
				case '(':
					stack.push(new Bracket(ch, pos));
					break;
				case '}':
				case ']':
				case ')':
					if (stack.isEmpty()) {
						throw new ParseException(expressionString, pos, "Found closing '"
								+ ch + "' at position " + pos + " without an opening '"
								+ Bracket.theOpenBracketFor(ch) + "'");
					}
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '"
								+ ch + "' at position " + pos
								+ " but most recent opening is '" + p.bracket
								+ "' at position " + p.pos);
					}
					break;
				case '\'':
				case '"':
					// jump to the end of the literal
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position "
										+ pos);
					}
					pos = endLiteral;
					break;
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '"
					+ Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket
					+ "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			return -1;
		}
		return pos;
	}


	/**
	 * This captures a type of bracket and the position in which it occurs in the
	 * expression. The positional information is used if an error has to be reported
	 * because the related end bracket cannot be found. Bracket is used to describe:
	 * square brackets [] round brackets () and curly brackets {}
	 */
	private static class Bracket {

		char bracket;

		int pos;

		Bracket(char bracket, int pos) {
			this.bracket = bracket;
			this.pos = pos;
		}

		boolean compatibleWithCloseBracket(char closeBracket) {
			if (this.bracket == '{') {
				return closeBracket == '}';
			}
			else if (this.bracket == '[') {
				return closeBracket == ']';
			}
			return closeBracket == ')';
		}

		static char theOpenBracketFor(char closeBracket) {
			if (closeBracket == '}') {
				return '{';
			}
			else if (closeBracket == ']') {
				return '[';
			}
			return '(';
		}

		static char theCloseBracketFor(char openBracket) {
			if (openBracket == '{') {
				return '}';
			}
			else if (openBracket == '[') {
				return ']';
			}
			return ')';
		}
	}

	/**
	 * Actually parse the expression string and return an Expression object.
	 * @param expressionString the raw expression string to parse
	 * @param context a context for influencing this expression parsing routine (optional)
	 * @return an evaluator for the parsed expression
	 * @throws ParseException an exception occurred during parsing
	 */
	protected abstract Expression doParseExpression(String expressionString,
			ParserContext context) throws ParseException;

}
