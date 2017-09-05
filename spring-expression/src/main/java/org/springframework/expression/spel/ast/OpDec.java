/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;

/**
 * Decrement operator.  Can be used in a prefix or postfix form. This will throw
 * appropriate exceptions if the operand in question does not support decrement.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 3.2
 */
public class OpDec extends Operator {

	private final boolean postfix;  // false means prefix


	public OpDec(int pos, boolean postfix, SpelNodeImpl... operands) {
		super("--", pos, operands);
		this.postfix = postfix;
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl operand = getLeftOperand();

		// The operand is going to be read and then assigned to, we don't want to evaluate it twice.
		ValueRef lvalue = operand.getValueRef(state);

		TypedValue operandTypedValue = lvalue.getValue();  //operand.getValueInternal(state);
		Object operandValue = operandTypedValue.getValue();
		TypedValue returnValue = operandTypedValue;
		TypedValue newValue = null;

		if (operandValue instanceof Number) {
			Number op1 = (Number) operandValue;
			if (op1 instanceof BigDecimal) {
				newValue = new TypedValue(((BigDecimal) op1).subtract(BigDecimal.ONE), operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Double) {
				newValue = new TypedValue(op1.doubleValue() - 1.0d, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Float) {
				newValue = new TypedValue(op1.floatValue() - 1.0f, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof BigInteger) {
				newValue = new TypedValue(((BigInteger) op1).subtract(BigInteger.ONE), operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Long) {
				newValue = new TypedValue(op1.longValue() - 1L, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Integer) {
				newValue = new TypedValue(op1.intValue() - 1, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Short) {
				newValue = new TypedValue(op1.shortValue() - (short) 1, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Byte) {
				newValue = new TypedValue(op1.byteValue() - (byte) 1, operandTypedValue.getTypeDescriptor());
			}
			else {
				// Unknown Number subtype -> best guess is double decrement
				newValue = new TypedValue(op1.doubleValue() - 1.0d, operandTypedValue.getTypeDescriptor());
			}
		}

		if (newValue == null) {
			try {
				newValue = state.operate(Operation.SUBTRACT, returnValue.getValue(), 1);
			}
			catch (SpelEvaluationException ex) {
				if (ex.getMessageCode() == SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES) {
					// This means the operand is not decrementable
					throw new SpelEvaluationException(operand.getStartPosition(),
							SpelMessage.OPERAND_NOT_DECREMENTABLE, operand.toStringAST());
				}
				else {
					throw ex;
				}
			}
		}

		// set the new value
		try {
			lvalue.setValue(newValue.getValue());
		}
		catch (SpelEvaluationException see) {
			// if unable to set the value the operand is not writable (e.g. 1-- )
			if (see.getMessageCode() == SpelMessage.SETVALUE_NOT_SUPPORTED) {
				throw new SpelEvaluationException(operand.getStartPosition(),
						SpelMessage.OPERAND_NOT_DECREMENTABLE);
			}
			else {
				throw see;
			}
		}

		if (!this.postfix) {
			// the return value is the new value, not the original value
			returnValue = newValue;
		}

		return returnValue;
	}

	@Override
	public String toStringAST() {
		return getLeftOperand().toStringAST() + "--";
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		throw new IllegalStateException("No right operand");
	}

}
