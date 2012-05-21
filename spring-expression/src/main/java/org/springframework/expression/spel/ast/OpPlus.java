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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * The plus operator will:
 * <ul>
 * <li>add {@code BigDecimal}
 * <li>add doubles (floats are represented as doubles)
 * <li>add longs
 * <li>add integers
 * <li>concatenate strings
 * </ul>
 * It can be used as a unary operator for numbers ({@code BigDecimal}/double/long/int).
 * The standard promotions are performed when the operand types vary (double+int=double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpPlus extends Operator {


	public OpPlus(int pos, SpelNodeImpl... operands) {
		super("+", pos, operands);
		Assert.notEmpty(operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		if (rightOp == null) { // If only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				if (operandOne instanceof Double || operandOne instanceof Long || operandOne instanceof BigDecimal) {
					return new TypedValue(operandOne);
				}
				if (operandOne instanceof Float) {
					return new TypedValue(((Number) operandOne).floatValue());
				}
				return new TypedValue(((Number) operandOne).intValue());
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		final TypedValue operandOneValue = leftOp.getValueInternal(state);
		final Object leftOperand = operandOneValue.getValue();

		final TypedValue operandTwoValue = rightOp.getValueInternal(state);
		final Object rightOperand = operandTwoValue.getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.add(rightBigDecimal));
			}

			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}

			if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
			}

			if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
			}

			// TODO what about overflow?
			return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
		}

		if (leftOperand instanceof String && rightOperand instanceof String) {
			return new TypedValue(new StringBuilder((String) leftOperand).append(
					(String) rightOperand).toString());
		}

		if (leftOperand instanceof String) {
			StringBuilder result = new StringBuilder((String) leftOperand);
			result.append((rightOperand == null ? "null" : convertTypedValueToString(
					operandTwoValue, state)));
			return new TypedValue(result.toString());
		}

		if (rightOperand instanceof String) {
			StringBuilder result = new StringBuilder((leftOperand == null ? "null"
					: convertTypedValueToString(operandOneValue, state)));
			result.append((String) rightOperand);
			return new TypedValue(result.toString());
		}

		return state.operate(Operation.ADD, leftOperand, rightOperand);
	}

	@Override
	public String toStringAST() {
		if (this.children.length<2) {  // unary plus
			return new StringBuilder().append("+").append(getLeftOperand().toStringAST()).toString();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			return null;
		}
		return this.children[1];
	}

	/**
	 * Convert operand value to string using registered converter or using
	 * {@code toString} method.
	 *
	 * @param value typed value to be converted
	 * @param state expression state
	 * @return {@code TypedValue} instance converted to {@code String}
	 */
	private static String convertTypedValueToString(TypedValue value, ExpressionState state) {
		final TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		final TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);

		if (typeConverter.canConvert(value.getTypeDescriptor(), typeDescriptor)) {
			final Object obj = typeConverter.convertValue(value.getValue(),
					value.getTypeDescriptor(), typeDescriptor);
			return String.valueOf(obj);
		}

		return String.valueOf(value.getValue());
	}

}
