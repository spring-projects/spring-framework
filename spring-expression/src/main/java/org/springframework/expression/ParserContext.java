/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

/**
 * Input provided to an expression parser that can influence an expression
 * parsing/compilation routine.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ParserContext {

	/**
	 * Whether the expression being parsed is a template.
	 * <p>A template consists of literal text that can be mixed with expressions.
	 * Some examples:
	 * <pre class="code">
	 *     Some literal text
	 *     Hello #{name.firstName}!
	 *     #{3 + 4}
	 * </pre>
	 * @return true if the expression is a template, false otherwise
	 */
	boolean isTemplate();

	/**
	 * For template expressions, returns the prefix that identifies the start of an
	 * expression block within a string. For example: "${"
	 * @return the prefix that identifies the start of an expression
	 */
	String getExpressionPrefix();

	/**
	 * For template expressions, returns the prefix that identifies the end of an
	 * expression block within a string. For example: "}"
	 * @return the suffix that identifies the end of an expression
	 */
	String getExpressionSuffix();


	/**
	 * The default {@link ParserContext} implementation that enables template
	 * expression parsing.
	 * <p>The expression prefix is "#{", and the expression suffix is "}".
	 * @see #isTemplate()
	 */
	ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

		@Override
		public boolean isTemplate() {
			return true;
		}

		@Override
		public String getExpressionPrefix() {
			return "#{";
		}

		@Override
		public String getExpressionSuffix() {
			return "}";
		}
	};

}
