/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * Superclass for exceptions that can occur whilst processing expressions.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

	protected final @Nullable String expressionString;

	protected int position;  // -1 if not known; should be known in all reasonable cases


	/**
	 * Construct a new expression exception.
	 * @param message a descriptive message
	 */
	public ExpressionException(String message) {
		super(message);
		this.expressionString = null;
		this.position = 0;
	}

	/**
	 * Construct a new expression exception.
	 * @param message a descriptive message
	 * @param cause the underlying cause of this exception
	 */
	public ExpressionException(String message, @Nullable Throwable cause) {
		super(message, cause);
		this.expressionString = null;
		this.position = 0;
	}

	/**
	 * Construct a new expression exception.
	 * @param expressionString the expression string
	 * @param message a descriptive message
	 */
	public ExpressionException(@Nullable String expressionString, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = -1;
	}

	/**
	 * Construct a new expression exception.
	 * @param expressionString the expression string
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 */
	public ExpressionException(@Nullable String expressionString, int position, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = position;
	}

	/**
	 * Construct a new expression exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 */
	public ExpressionException(int position, String message) {
		super(message);
		this.expressionString = null;
		this.position = position;
	}

	/**
	 * Construct a new expression exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 * @param cause the underlying cause of this exception
	 */
	public ExpressionException(int position, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.expressionString = null;
		this.position = position;
	}


	/**
	 * Return the expression string.
	 */
	public final @Nullable String getExpressionString() {
		return this.expressionString;
	}

	/**
	 * Return the position in the expression string where the problem occurred.
	 */
	public final int getPosition() {
		return this.position;
	}

	/**
	 * Return the exception message.
	 * <p>This method returns the same result as {@link #toDetailedString()}.
	 * @see #getSimpleMessage()
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return toDetailedString();
	}

	/**
	 * Return a detailed description of this exception, including the expression
	 * String and position (if available) as well as the actual exception message.
	 */
	public String toDetailedString() {
		if (this.expressionString != null) {
			StringBuilder output = new StringBuilder();
			output.append("Expression [");
			output.append(this.expressionString);
			output.append(']');
			if (this.position >= 0) {
				output.append(" @");
				output.append(this.position);
			}
			output.append(": ");
			output.append(getSimpleMessage());
			return output.toString();
		}
		else {
			return getSimpleMessage();
		}
	}

	/**
	 * Return the exception simple message without including the expression
	 * that caused the failure.
	 * @since 4.0
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public String getSimpleMessage() {
		return super.getMessage();
	}

}
