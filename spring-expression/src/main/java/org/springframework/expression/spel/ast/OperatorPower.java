/*
 * Copyright 2002-2014 the original author or authors.
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
import java.math.BigInteger;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.NumberUtils;

/**
 * The power operator.
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OperatorPower extends Operator {

	public OperatorPower(int pos, SpelNodeImpl... operands) {
		super("^", pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		Object leftOperand = leftOp.getValueInternal(state).getValue();
		Object rightOperand = rightOp.getValueInternal(state).getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.pow(rightNumber.intValue()));
			}
			else if (leftNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.pow(rightNumber.intValue()));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return new TypedValue(Math.pow(leftNumber.doubleValue(), rightNumber.doubleValue()));
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return new TypedValue(Math.pow(leftNumber.floatValue(), rightNumber.floatValue()));
			}

			double d = Math.pow(leftNumber.doubleValue(), rightNumber.doubleValue());
			if (d > Integer.MAX_VALUE || leftNumber instanceof Long || rightNumber instanceof Long) {
				return new TypedValue((long) d);
			}
			else {
				return new TypedValue((int) d);
			}
		}

		return state.operate(Operation.POWER, leftOperand, rightOperand);
	}

}
