/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

/**
 * Parses expression strings into compiled expressions that can be evaluated.
 *
 * <p>Supports parsing template expressions as well as standard expression strings.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ExpressionParser {

	/**
	 * Parse the expression string and return an {@link Expression} object that
	 * can be used for repeated evaluation.
	 * <p>Examples:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 * @param expressionString the raw expression string to parse
	 * @return an {@code Expression} for the parsed expression
	 * @throws ParseException if an exception occurred during parsing
	 */
	Expression parseExpression(String expressionString) throws ParseException;

	/**
	 * Parse the expression string and return an {@link Expression} object that
	 * can be used for repeated evaluation.
	 * <p>Examples:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 * @param expressionString the raw expression string to parse
	 * @param context a context for influencing the expression parsing routine
	 * @return an {@code Expression} for the parsed expression
	 * @throws ParseException if an exception occurred during parsing
	 */
	Expression parseExpression(String expressionString, ParserContext context) throws ParseException;

}
