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
 * The plus operator will:
 * <ul>
 * <li>add doubles (floats are represented as doubles)
 * <li>add longs
 * <li>add integers
 * <li>concatenate strings
 * </ul>
 * It can be used as a unary operator for numbers (double/long/int).  The standard promotions are performed
 * when the operand types vary (double+int=double). For other options it defers to the registered overloader.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class OpPlus extends Operator {

	public OpPlus(int pos, SpelNodeImpl... operands) {
		super("+", pos, operands);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();
		if (rightOp == null) { // If only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
                if ( operandOne instanceof BigDecimal ) {
                    return new TypedValue((BigDecimal) operandOne);
                }
				if (operandOne instanceof Double) {
					return new TypedValue(((Double) operandOne).doubleValue());
				} else if (operandOne instanceof Long) {
					return new TypedValue(((Long) operandOne).longValue());
				} else {
					return new TypedValue(((Integer) operandOne).intValue());
				}
			}
			return state.operate(Operation.ADD, operandOne, null);
		}
		else {
			Object operandOne = leftOp.getValueInternal(state).getValue();
			Object operandTwo = rightOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number && operandTwo instanceof Number) {
				Number op1 = (Number) operandOne;
				Number op2 = (Number) operandTwo;
				if ( op1 instanceof BigDecimal || op2 instanceof BigDecimal ) {
                    BigDecimal bd1 = NumberUtils.convertNumberToTargetClass(op1, BigDecimal.class);
                    BigDecimal bd2 = NumberUtils.convertNumberToTargetClass(op2, BigDecimal.class);
					return new TypedValue(bd1.add(bd2));
                } else if (op1 instanceof Double || op2 instanceof Double) {
					return new TypedValue(op1.doubleValue() + op2.doubleValue());
				} else if (op1 instanceof Long || op2 instanceof Long) {
					return new TypedValue(op1.longValue() + op2.longValue());
				} else { // TODO what about overflow?
					return new TypedValue(op1.intValue() + op2.intValue());
				}
			} else if (operandOne instanceof String && operandTwo instanceof String) {
				return new TypedValue(new StringBuilder((String) operandOne).append((String) operandTwo).toString());
			} else if (operandOne instanceof String) {
				StringBuilder result = new StringBuilder((String)operandOne);
				result.append((operandTwo==null?"null":operandTwo.toString()));
				return new TypedValue(result.toString());				
			} else if (operandTwo instanceof String) {
				StringBuilder result = new StringBuilder((operandOne==null?"null":operandOne.toString()));
				result.append((String)operandTwo);
				return new TypedValue(result.toString());								
			}
			return state.operate(Operation.ADD, operandOne, operandTwo);
		}
	}

	@Override
	public String toStringAST() {
		if (children.length<2) {  // unary plus
			return new StringBuilder().append("+").append(getLeftOperand().toStringAST()).toString();
		}
		return super.toStringAST();
	}

	public SpelNodeImpl getRightOperand() {
		if (children.length<2) {return null;}
		return children[1];
	}


}
