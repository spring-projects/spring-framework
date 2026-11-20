/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.support;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;

/**
 * Standard implementation of {@link OperatorOverloader}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardOperatorOverloader implements OperatorOverloader {

	static final StandardOperatorOverloader INSTANCE = new StandardOperatorOverloader();

	@Override
	public boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException {

		return false;
	}

	@Override
	public Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException {

		throw new EvaluationException("No operation overloaded by default");
	}

}
