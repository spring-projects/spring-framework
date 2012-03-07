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

import java.math.BigDecimal;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.util.NumberUtils;

/**
 * Implements the not-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpNE extends Operator {

	public OpNE(int pos, SpelNodeImpl... operands) {
		super("!=", pos, operands);
	}

	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (left instanceof Number && right instanceof Number) {
			Number op1 = (Number) left;
			Number op2 = (Number) right;
            if ( op1 instanceof BigDecimal || op2 instanceof BigDecimal ) {
                BigDecimal bd1 = NumberUtils.convertNumberToTargetClass(op1, BigDecimal.class);
                BigDecimal bd2 = NumberUtils.convertNumberToTargetClass(op2, BigDecimal.class);
                if ( bd1 == null || bd2 == null ) {
                    throw new SpelEvaluationException(SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
                }
                return BooleanTypedValue.forValue(bd1.compareTo(bd2) != 0);
            } else if (op1 instanceof Double || op2 instanceof Double) {
				return BooleanTypedValue.forValue(op1.doubleValue() != op2.doubleValue());
			} else if (op1 instanceof Long || op2 instanceof Long) {
				return BooleanTypedValue.forValue(op1.longValue() != op2.longValue());
			} else {
				return BooleanTypedValue.forValue(op1.intValue() != op2.intValue());
			}
		}

		if (left!=null && (left instanceof Comparable)) {
			return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) != 0);
		} else {
			return BooleanTypedValue.forValue(left!=right);
		}
	}

}
