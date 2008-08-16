package org.springframework.expression;

/**
 * By default the mathematical operators {@link Operation} support simple types like numbers. By providing an
 * implementation of OperatorOverloader, a user of the expression language can support these operations on other types.
 * 
 * @author Andy Clement
 */
public interface OperatorOverloader {

	// TODO does type OperatorOverloader need a better name?
	// TODO Operator overloading needs some testing!

	/**
	 * Return true if the operator overloader supports the specified operation between the two operands and so should be
	 * invoked to handle it.
	 * 
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return true if the OperatorOverloader supports the specified operation between the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException;

	/**
	 * Execute the specified operation on two operands, returning a result. See {@link Operation} for supported
	 * operations.
	 * 
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return the result of performing the operation on the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	Object operate(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException;
}
