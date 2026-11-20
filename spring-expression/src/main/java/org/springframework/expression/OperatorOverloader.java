/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import org.jspecify.annotations.Nullable;

/**
 * By default, the mathematical operators defined in {@link Operation} support simple
 * types like numbers. By providing an implementation of {@code OperatorOverloader},
 * a user of the expression language can support these operations on other types.
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface OperatorOverloader {

	/**
	 * Return {@code true} if this operator overloader supports the specified
	 * operation on the two operands and should be invoked to handle it.
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return true if this {@code OperatorOverloader} supports the specified
	 * operation between the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

	/**
	 * Perform the specified operation on the two operands, returning a result.
	 * <p>See {@link Operation} for supported operations.
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return the result of performing the operation on the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

}
