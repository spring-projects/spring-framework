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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test providing operator support
 *
 * @author Andy Clement
 */
class OperatorOverloaderTests extends AbstractExpressionTests {

	@Test
	void testSimpleOperations() {
		// no built-in support for this:
		evaluateAndCheckError("'abc'-true",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);

		StandardEvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();
		eContext.setOperatorOverloader(new StringAndBooleanAddition());

		SpelExpression expr = (SpelExpression)parser.parseExpression("'abc'+true");
		assertThat(expr.getValue(eContext)).isEqualTo("abctrue");

		expr = (SpelExpression)parser.parseExpression("'abc'-true");
		assertThat(expr.getValue(eContext)).isEqualTo("abc");

		expr = (SpelExpression)parser.parseExpression("'abc'+null");
		assertThat(expr.getValue(eContext)).isEqualTo("abcnull");
	}


	static class StringAndBooleanAddition implements OperatorOverloader {

		@Override
		public Object operate(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
			if (operation==Operation.ADD) {
				return leftOperand + ((Boolean) rightOperand).toString();
			}
			else {
				return leftOperand;
			}
		}

		@Override
		public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
			return leftOperand instanceof String && rightOperand instanceof Boolean;

		}
	}

}
