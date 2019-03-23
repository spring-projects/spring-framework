/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.util.NumberUtils;

/**
 * Implements greater-than-or-equal operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpGE extends Operator {

	public OpGE(int pos, SpelNodeImpl... operands) {
		super(">=", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();

		this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(left);
		this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(right);

		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return BooleanTypedValue.forValue(leftBigDecimal.compareTo(rightBigDecimal) >= 0);
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return BooleanTypedValue.forValue(leftNumber.doubleValue() >= rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return BooleanTypedValue.forValue(leftNumber.floatValue() >= rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return BooleanTypedValue.forValue(leftBigInteger.compareTo(rightBigInteger) >= 0);
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return BooleanTypedValue.forValue(leftNumber.longValue() >= rightNumber.longValue());
			}
			else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
				return BooleanTypedValue.forValue(leftNumber.intValue() >= rightNumber.intValue());
			}
			else if (leftNumber instanceof Short || rightNumber instanceof Short) {
				return BooleanTypedValue.forValue(leftNumber.shortValue() >= rightNumber.shortValue());
			}
			else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
				return BooleanTypedValue.forValue(leftNumber.byteValue() >= rightNumber.byteValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double comparison
				return BooleanTypedValue.forValue(leftNumber.doubleValue() >= rightNumber.doubleValue());
			}
		}

		return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) >= 0);
	}

	@Override
	public boolean isCompilable() {
		return isCompilableOperatorUsingNumerics();
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		generateComparisonCode(mv, cf, IFLT, IF_ICMPLT);
	}

}
