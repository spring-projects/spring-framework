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
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.NumberUtils;

/**
 * The power operator.
 * 
 * @author Andy Clement
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
	
		Object operandOne = leftOp.getValueInternal(state).getValue();
		Object operandTwo = rightOp.getValueInternal(state).getValue();
		if (operandOne instanceof Number && operandTwo instanceof Number) {
			Number op1 = (Number) operandOne;
			Number op2 = (Number) operandTwo;
            if ( op1 instanceof BigDecimal ) {
            	// BigDecimal.pow has a limit in the range.
            	// is it correct to use the power function this way?
                BigDecimal bd1 = NumberUtils.convertNumberToTargetClass(op1, BigDecimal.class);
				return new TypedValue(bd1.pow(op2.intValue()));
            } else if (op1 instanceof Double || op2 instanceof Double) {
				return new TypedValue(Math.pow(op1.doubleValue(),op2.doubleValue()));
			} else if (op1 instanceof Long || op2 instanceof Long) {
				double d= Math.pow(op1.longValue(), op2.longValue());
				return new TypedValue((long)d);
			} else {
				double d= Math.pow(op1.longValue(), op2.longValue());
				if (d > Integer.MAX_VALUE) {
					return new TypedValue((long)d);
				} else {
					return new TypedValue((int)d);
				}
			}
		}
		return state.operate(Operation.POWER, operandOne, operandTwo);
	}

}
