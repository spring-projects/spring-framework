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

package org.springframework.expression;


/**
 * Super class for exceptions that can occur whilst processing expressions
 *
 * @author Andy Clement
 * @since 3.0
 */
public class ExpressionException extends RuntimeException {

	protected String expressionString;
	protected int position; // -1 if not known - but should be known in all reasonable cases

	/**
	 * Creates a new expression exception.
	 * @param expressionString the expression string
	 * @param message a descriptive message
	 */
	public ExpressionException(String expressionString, String message) {
		super(message);
		this.position = -1;
		this.expressionString = expressionString;
	}

	/**
	 * Creates a new expression exception.
	 * @param expressionString the expression string
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 */
	public ExpressionException(String expressionString, int position, String message) {
		super(message);
		this.position = position;
		this.expressionString = expressionString;
	}

	/**
	 * Creates a new expression exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 */
	public ExpressionException(int position, String message) {
		super(message);
		this.position = position;
	}

	/**
	 * Creates a new expression exception.
	 * @param position the position in the expression string where the problem occurred
	 * @param message a descriptive message
	 * @param cause the underlying cause of this exception
	 */
	public ExpressionException(int position, String message, Throwable cause) {
		super(message,cause);
		this.position = position;
	}

	/**
	 * Creates a new expression exception.
	 * @param message a descriptive message
	 */
	public ExpressionException(String message) {
		super(message);
	}

	public ExpressionException(String message, Throwable cause) {
		super(message,cause);
	}

	public String toDetailedString() {
		StringBuilder output = new StringBuilder();
		if (expressionString!=null) {
			output.append("Expression '");
			output.append(expressionString);
			output.append("'");
			if (position!=-1) {
				output.append(" @ ");
				output.append(position);
			}
			output.append(": ");
		}
		output.append(getMessage());
		return output.toString();
	}

	public final String getExpressionString() {
		return this.expressionString;
	}

	public final int getPosition() {
		return position;
	}

}
