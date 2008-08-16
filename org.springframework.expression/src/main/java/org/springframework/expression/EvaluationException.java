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
package org.springframework.expression;

/**
 * Base class for exceptions occurring during expression parsing and evaluation.
 * 
 * @author Andy Clement
 */
public class EvaluationException extends Exception {

	/**
	 * The expression string.
	 */
	private String expressionString;

	/**
	 * Creates a new expression exception. The expressionString field should be set by a later call to
	 * setExpressionString().
	 * 
	 * @param cause the underlying cause of this exception
	 */
	public EvaluationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new expression parsing exception.
	 * 
	 * @param expressionString the expression string that could not be parsed
	 * @param cause the underlying cause of this exception
	 */
	public EvaluationException(String expressionString, Throwable cause) {
		this(expressionString, "Exception occurred whilst handling '" + expressionString + "'", cause);
	}

	/**
	 * Creates a new expression exception.
	 * 
	 * @param expressionString the expression string
	 * @param message a descriptive message
	 * @param cause the underlying cause of this exception
	 */
	public EvaluationException(String expressionString, String message, Throwable cause) {
		super(message, cause);
		this.expressionString = expressionString;
	}

	/**
	 * Creates a new expression exception.
	 * 
	 * @param expressionString the expression string
	 * @param message a descriptive message
	 */
	public EvaluationException(String expressionString, String message) {
		super(message);
		this.expressionString = expressionString;
	}

	/**
	 * Creates a new expression exception. The expressionString field should be set by a later call to
	 * setExpressionString().
	 * 
	 * @param message a descriptive message
	 */
	public EvaluationException(String message) {
		super(message);
	}

	/**
	 * Set the expression string, called on exceptions where the expressionString is not known at the time of exception
	 * creation.
	 * 
	 * @param expressionString the expression string
	 */
	protected final void setExpressionString(String expressionString) {
		this.expressionString = expressionString;
	}

	/**
	 * @return the expression string
	 */
	public final String getExpressionString() {
		return expressionString;
	}
}