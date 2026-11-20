/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * Represent an exception that occurs during expression parsing.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ParseException extends ExpressionException {

	/**
	 * Create a new expression parsing exception.
	 * @param expressionString the expression string that could not be parsed
	 * @param position the position in the expression string where the problem occurred
	 * @param message description of the problem that occurred
	 */
	public ParseException(@Nullable String expressionString, int position, String message) {
		super(expressionString, position, message);
	}

	/**
	 * Create a new expression parsing exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message description of the problem that occurred
	 * @param cause the underlying cause of this exception
	 */
	public ParseException(int position, String message, Throwable cause) {
		super(position, message, cause);
	}

	/**
	 * Create a new expression parsing exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message description of the problem that occurred
	 */
	public ParseException(int position, String message) {
		super(position, message);
	}

}
