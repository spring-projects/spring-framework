/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * Superclass for exceptions that can occur whilst processing expressions.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

	@Nullable
	protected final String expressionString;

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
	@Nullable
	public final String getExpressionString() {
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
	 * As of Spring 4.0, this method returns the same result as {@link #toDetailedString()}.
	 * @see #getSimpleMessage()
	 * @see java.lang.Throwable#getMessage()
	 */
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
	@SuppressWarnings("NullAway")
	public String getSimpleMessage() {
		return super.getMessage();
	}

}
