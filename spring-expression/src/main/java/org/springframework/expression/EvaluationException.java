/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Represent an exception that occurs during expression evaluation.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class EvaluationException extends ExpressionException {

	/**
	 * Create a new expression evaluation exception.
	 * @param message description of the problem that occurred
	 */
	public EvaluationException(String message) {
		super(message);
	}

	/**
	 * Create a new expression evaluation exception.
	 * @param message description of the problem that occurred
	 * @param cause the underlying cause of this exception
	 */
	public EvaluationException(String message, @Nullable Throwable cause) {
		super(message,cause);
	}

	/**
	 * Create a new expression evaluation exception.
	 * @param position the position in the expression where the problem occurred
	 * @param message description of the problem that occurred
	 */
	public EvaluationException(int position, String message) {
		super(position, message);
	}

	/**
	 * Create a new expression evaluation exception.
	 * @param expressionString the expression that could not be evaluated
	 * @param message description of the problem that occurred
	 */
	public EvaluationException(String expressionString, String message) {
		super(expressionString, message);
	}

	/**
	 * Create a new expression evaluation exception.
	 * @param position the position in the expression where the problem occurred
	 * @param message description of the problem that occurred
	 * @param cause the underlying cause of this exception
	 */
	public EvaluationException(int position, String message, @Nullable Throwable cause) {
		super(position, message, cause);
	}

}
