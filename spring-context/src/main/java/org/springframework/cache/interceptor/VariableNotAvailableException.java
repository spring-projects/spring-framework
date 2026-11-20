/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import org.springframework.expression.EvaluationException;

/**
 * An internal {@link EvaluationException} which signals that a given variable
 * used in an expression is not available in the context.
 *
 * @author Stephane Nicoll
 * @since 4.0.6
 */
@SuppressWarnings("serial")
class VariableNotAvailableException extends EvaluationException {

	public VariableNotAvailableException(String name) {
		super("Variable '" + name + "' not available");
	}

}
