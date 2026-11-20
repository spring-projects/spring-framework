/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;

/**
 * Tests for custom {@link OperatorOverloader} support.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class OperatorOverloaderTests extends AbstractExpressionTests {

	@Test
	void simpleOperations() {
		// default behavior
		evaluate("'abc' + true", "abctrue", String.class);
		evaluate("'abc' + null", "abcnull", String.class);

		// no built-in support for <string> - <boolean>
		evaluateAndCheckError("'abc' - true", SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);

		super.context.setOperatorOverloader(new StringAndBooleanOperatorOverloader());

		// unaffected
		evaluate("'abc' + true", "abctrue", String.class);
		evaluate("'abc' + null", "abcnull", String.class);

		// <string> - <boolean> has been overloaded
		evaluate("'abc' - true", "abcTRUE", String.class);
	}


	private static class StringAndBooleanOperatorOverloader implements OperatorOverloader {

		@Override
		public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand) {
			return (leftOperand instanceof String && rightOperand instanceof Boolean);
		}

		@Override
		public Object operate(Operation operation, Object leftOperand, Object rightOperand) {
			if (operation == Operation.SUBTRACT) {
				return leftOperand + ((Boolean) rightOperand).toString().toUpperCase();
			}
			throw new UnsupportedOperationException(operation.name());
		}
	}

}
