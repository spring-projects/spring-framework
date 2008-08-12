/*
 * Copyright 2004-2008 the original author or authors.
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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Implements the minus operator. If there is only one operand it is a unary minus.
 * 
 * @author Andy Clement
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
	public Object getValue(ExpressionState state) throws EvaluationException {
		SpelNode leftOp = getLeftOperand();
		SpelNode rightOp = getRightOperand();
		if (rightOp == null) {// If only one operand, then this is unary minus
			Object left = leftOp.getValue(state);
			if (left instanceof Number) {
				Number n = (Number) left;
				if (left instanceof Double) {
					Double result = 0 - n.doubleValue();
					return result;
				} else if (left instanceof Float) {
					Float result = 0 - n.floatValue();
					return result;
				} else if (left instanceof Long) {
					Long result = 0 - n.longValue();
					return result;
				} else {
					Integer result = 0 - n.intValue();
					return result;
				}
			}
			throw new SpelException(SpelMessages.CANNOT_NEGATE_TYPE, left.getClass().getName());
		} else {
			Object left = leftOp.getValue(state);
			Object right = rightOp.getValue(state);
			if (left instanceof Number && right instanceof Number) {
				Number op1 = (Number) left;
				Number op2 = (Number) right;
				if (op1 instanceof Double || op2 instanceof Double) {
					Double result = op1.doubleValue() - op2.doubleValue();
					return result;
				} else if (op1 instanceof Float || op2 instanceof Float) {
					Float result = op1.floatValue() - op2.floatValue();
					return result;
				} else if (op1 instanceof Long || op2 instanceof Long) {
					Long result = op1.longValue() - op2.longValue();
					return result;
				} else {
					Integer result = op1.intValue() - op2.intValue();
					return result;
				}
			}
			return state.operate(Operation.SUBTRACT, left, right);
		}
	}
}
