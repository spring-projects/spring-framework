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
package org.springframework.binding.expression;

/**
 * Base class for exceptions thrown during expression parsing.
 * 
 * @author Keith Donald
 */
public class ParserException extends RuntimeException {

	/**
	 * The expression string that could not be parsed.
	 */
	private String expressionString;

	/**
	 * Creates a new expression parsing exception.
	 * @param expressionString the expression string that could not be parsed
	 * @param cause the underlying cause of this exception
	 */
	public ParserException(String expressionString, Throwable cause) {
		this(expressionString, "Unable to parse expression string '" + expressionString + "'", cause);
	}

	/**
	 * Creates a new expression parsing exception.
	 * @param expressionString the expression string that could not be parsed
	 * @param message a descriptive message
	 * @param cause the underlying cause of this exception
	 */
	public ParserException(String expressionString, String message, Throwable cause) {
		super(message, cause);
		this.expressionString = expressionString;
	}

	/**
	 * Returns the expression string that could not be parsed.
	 */
	public Object getExpressionString() {
		return expressionString;
	}
}