/*
 * Copyright 2002-2013 the original author or authors.
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

import java.math.BigDecimal;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.util.NumberUtils;

/**
 * Implements the less-than-or-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpLE extends Operator {

	public OpLE(int pos, SpelNodeImpl... operands) {
		super("<=", pos, operands);
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state)
			throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal bdLeft = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal bdRight = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return BooleanTypedValue.forValue(bdLeft.compareTo(bdRight) <= 0);
			}
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return BooleanTypedValue.forValue(leftNumber.doubleValue() <= rightNumber.doubleValue());
			}
			if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return BooleanTypedValue.forValue(leftNumber.floatValue() <= rightNumber.floatValue());
			}
			if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return BooleanTypedValue.forValue(leftNumber.longValue() <= rightNumber.longValue());
			}
			return BooleanTypedValue.forValue(leftNumber.intValue() <= rightNumber.intValue());
		}
		return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) <= 0);
	}

}
