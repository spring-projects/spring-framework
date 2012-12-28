/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * Test providing operator support
 *
 * @author Andy Clement
 */
public class OperatorOverloaderTests extends ExpressionTestCase {

	static class StringAndBooleanAddition implements OperatorOverloader {

		@Override
		public Object operate(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
			if (operation==Operation.ADD) {
				return ((String)leftOperand)+((Boolean)rightOperand).toString();
			} else {
				return leftOperand;
			}
		}

		@Override
		public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand)
				throws EvaluationException {
			if (leftOperand instanceof String && rightOperand instanceof Boolean) {
				return true;
			}
			return false;

		}

	}

	@Test
	public void testSimpleOperations() throws Exception {
		// no built in support for this:
		evaluateAndCheckError("'abc'-true",SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES);

		StandardEvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();
		eContext.setOperatorOverloader(new StringAndBooleanAddition());

		SpelExpression expr = (SpelExpression)parser.parseExpression("'abc'+true");
		Assert.assertEquals("abctrue",expr.getValue(eContext));

		expr = (SpelExpression)parser.parseExpression("'abc'-true");
		Assert.assertEquals("abc",expr.getValue(eContext));

		expr = (SpelExpression)parser.parseExpression("'abc'+null");
		Assert.assertEquals("abcnull",expr.getValue(eContext));
	}
}
