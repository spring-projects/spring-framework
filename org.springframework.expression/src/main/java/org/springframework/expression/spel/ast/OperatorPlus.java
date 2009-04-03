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

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorPlus extends Operator {

	public OperatorPlus(Token payload) {
		super(payload);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();
		if (rightOp == null) { // If only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				return new TypedValue(((Number) operandOne).intValue(),INTEGER_TYPE_DESCRIPTOR);
			}
			return state.operate(Operation.ADD, operandOne, null);
		}
		else {
			Object operandOne = leftOp.getValueInternal(state).getValue();
			Object operandTwo = rightOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number && operandTwo instanceof Number) {
				Number op1 = (Number) operandOne;
				Number op2 = (Number) operandTwo;
				if (op1 instanceof Double || op2 instanceof Double) {
					return new TypedValue(op1.doubleValue() + op2.doubleValue(),DOUBLE_TYPE_DESCRIPTOR);
				}
				else if (op1 instanceof Float || op2 instanceof Float) {
					return new TypedValue(op1.floatValue() + op2.floatValue(),FLOAT_TYPE_DESCRIPTOR);
				}
				else if (op1 instanceof Long || op2 instanceof Long) {
					return new TypedValue(op1.longValue() + op2.longValue(),LONG_TYPE_DESCRIPTOR);
				}
				else { // TODO what about overflow?
					return new TypedValue(op1.intValue() + op2.intValue(),INTEGER_TYPE_DESCRIPTOR);
				}
			}
			else if (operandOne instanceof String && operandTwo instanceof String) {
				return new TypedValue(new StringBuilder((String) operandOne).append((String) operandTwo).toString(),STRING_TYPE_DESCRIPTOR);
			}
			else if (operandOne instanceof String && operandTwo instanceof Integer) {
				String l = (String) operandOne;
				Integer i = (Integer) operandTwo;
				// implements character + int (ie. a + 1 = b)
				if (l.length() == 1) {
					return new TypedValue(Character.toString((char) (l.charAt(0) + i)),STRING_TYPE_DESCRIPTOR);
				}
				return  new TypedValue(new StringBuilder(l).append(i).toString(),STRING_TYPE_DESCRIPTOR);
			}
			return state.operate(Operation.ADD, operandOne, operandTwo);
		}
	}

	@Override
	public String getOperatorName() {
		return "+";
	}

	@Override
	public String toStringAST() {
		if (getRightOperand() == null) {  // unary plus
			return new StringBuilder().append("+").append(getLeftOperand()).toString();
		}
		return super.toStringAST();
	}

}
