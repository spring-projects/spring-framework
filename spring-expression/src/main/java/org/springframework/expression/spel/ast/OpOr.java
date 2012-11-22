/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents the boolean OR operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @since 3.0
 */
public class OpOr extends Operator {

	public OpOr(int pos, SpelNodeImpl... operands) {
		super("or", pos, operands);
	}

	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Boolean leftValue;
		Boolean rightValue;
		try {
			leftValue = getLeftOperand().getValue(state, Boolean.class);
			this.assertValueNotNull(leftValue);
		}
		catch (SpelEvaluationException see) {
			see.setPosition(getLeftOperand().getStartPosition());
			throw see;
		}

		if (leftValue) {
			return BooleanTypedValue.TRUE; // no need to evaluate right operand
		}

		try {
			rightValue = getRightOperand().getValue(state, Boolean.class);
			this.assertValueNotNull(rightValue);
		}
		catch (SpelEvaluationException see) {
			see.setPosition(getRightOperand().getStartPosition()); // TODO end positions here and in similar situations
			throw see;
		}

		return BooleanTypedValue.forValue(/* leftValue || */ rightValue);
	}

	private void assertValueNotNull(Boolean value) {
		if (value == null) {
			throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
	}

}
