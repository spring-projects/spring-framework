/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents the {@code between} operator.
 *
 * <p>The left operand must be a single value, and the right operand must be a
 * 2-element list which defines a range from a lower bound to an upper bound.
 *
 * <p>This operator returns {@code true} if the left operand is greater than or
 * equal to the lower bound and less than or equal to the upper bound. Consequently,
 * {@code 1 between {1, 5}} evaluates to {@code true}, while {@code 1 between {5, 1}}
 * evaluates to {@code false}.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public class OperatorBetween extends Operator {

	public OperatorBetween(int startPos, int endPos, SpelNodeImpl... operands) {
		super("between", startPos, endPos, operands);
	}


	/**
	 * Returns a boolean based on whether a value is in the range expressed. The first
	 * operand is any value whilst the second is a list of two values - those two values
	 * being the lower and upper bounds allowed for the first operand (inclusive).
	 * @param state the expression state
	 * @return true if the left operand is in the range specified, false otherwise
	 * @throws EvaluationException if there is a problem evaluating the expression
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (!(right instanceof List<?> list) || list.size() != 2) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
		}

		Object low = list.get(0);
		Object high = list.get(1);
		TypeComparator comp = state.getTypeComparator();
		try {
			return BooleanTypedValue.forValue(comp.compare(left, low) >= 0 && comp.compare(left, high) <= 0);
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

}
