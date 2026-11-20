/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * This exception wraps (as cause) a checked exception thrown by some method that SpEL
 * invokes. It differs from a SpelEvaluationException because this indicates the
 * occurrence of a checked exception that the invoked method was defined to throw.
 * SpelEvaluationExceptions are for handling (and wrapping) unexpected exceptions.
 *
 * @author Andy Clement
 * @since 3.0.3
 */
@SuppressWarnings("serial")
public class ExpressionInvocationTargetException extends EvaluationException {

	public ExpressionInvocationTargetException(int position, String message, @Nullable Throwable cause) {
		super(position, message, cause);
	}

	public ExpressionInvocationTargetException(int position, String message) {
		super(position, message);
	}

	public ExpressionInvocationTargetException(String expressionString, String message) {
		super(expressionString, message);
	}

	public ExpressionInvocationTargetException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	public ExpressionInvocationTargetException(String message) {
		super(message);
	}

}
