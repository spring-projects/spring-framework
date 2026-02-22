package org.springframework.docs.core.expressions.languageref.expressionsoperatorsoverloaded

import org.springframework.expression.Operation
import org.springframework.expression.OperatorOverloader

class ListConcatenation: OperatorOverloader {

	override fun overridesOperation(operation: Operation, left: Any?, right: Any?): Boolean {
		return operation == Operation.ADD && left is List<*> && right is List<*>
	}

	override fun operate(operation: Operation, left: Any?, right: Any?): Any {
		if (operation == Operation.ADD && left is List<*> && right is List<*>) {
			return left + right
		}

		throw UnsupportedOperationException("No overload for operation $operation and operands [$left] and [$right]")
	}
}