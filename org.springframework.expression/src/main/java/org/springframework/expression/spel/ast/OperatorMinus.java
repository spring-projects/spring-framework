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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Implements the minus operator. If there is only one operand it is a unary minus.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorMinus extends Operator {

	public OperatorMinus(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "-";
	}

	@Override
	public String toStringAST() {
		if (getRightOperand() == null) { // unary minus
			return new StringBuilder().append("-").append(getLeftOperand()).toString();
		}
		return super.toStringAST();
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();
		if (rightOp == null) {// If only one operand, then this is unary minus
			Object left = leftOp.getValueInternal(state).getValue();
			if (left instanceof Number) {
				Number n = (Number) left;
				if (left instanceof Double) {
					return new TypedValue(0 - n.doubleValue(),DOUBLE_TYPE_DESCRIPTOR);
				}
				else if (left instanceof Float) {
					return new TypedValue(0 - n.floatValue(),FLOAT_TYPE_DESCRIPTOR);
				}
				else if (left instanceof Long) {
					return new TypedValue(0 - n.longValue(),LONG_TYPE_DESCRIPTOR);
				}
				else {
					return new TypedValue(0 - n.intValue(),INTEGER_TYPE_DESCRIPTOR);
				}
			}
			throw new SpelException(SpelMessages.CANNOT_NEGATE_TYPE, left.getClass().getName());
		}
		else {
			Object left = leftOp.getValueInternal(state).getValue();
			Object right = rightOp.getValueInternal(state).getValue();
			if (left instanceof Number && right instanceof Number) {
				Number op1 = (Number) left;
				Number op2 = (Number) right;
				if (op1 instanceof Double || op2 instanceof Double) {
					return new TypedValue(op1.doubleValue() - op2.doubleValue(),DOUBLE_TYPE_DESCRIPTOR);
				}
				else if (op1 instanceof Float || op2 instanceof Float) {
					return new TypedValue(op1.floatValue() - op2.floatValue(),FLOAT_TYPE_DESCRIPTOR);
				}
				else if (op1 instanceof Long || op2 instanceof Long) {
					return new TypedValue(op1.longValue() - op2.longValue(),LONG_TYPE_DESCRIPTOR);
				}
				else {
					return new TypedValue(op1.intValue() - op2.intValue(),INTEGER_TYPE_DESCRIPTOR);
				}
			}
			return state.operate(Operation.SUBTRACT, left, right);
		}
	}

}
