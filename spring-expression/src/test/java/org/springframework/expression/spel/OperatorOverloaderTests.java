/*
 * Copyright 2002-2024 the original author or authors.
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
