/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Implements the less-than operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpLT extends Operator {

	public OpLT(int startPos, int endPos, SpelNodeImpl... operands) {
		super("<", startPos, endPos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		state.trackOperation();

		this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(left);
		this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(right);

		if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return BooleanTypedValue.forValue(leftBigDecimal.compareTo(rightBigDecimal) < 0);
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return BooleanTypedValue.forValue(leftNumber.doubleValue() < rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return BooleanTypedValue.forValue(leftNumber.floatValue() < rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return BooleanTypedValue.forValue(leftBigInteger.compareTo(rightBigInteger) < 0);
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return BooleanTypedValue.forValue(leftNumber.longValue() < rightNumber.longValue());
			}
			else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
				return BooleanTypedValue.forValue(leftNumber.intValue() < rightNumber.intValue());
			}
			else if (leftNumber instanceof Short || rightNumber instanceof Short) {
				return BooleanTypedValue.forValue(leftNumber.shortValue() < rightNumber.shortValue());
			}
			else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
				return BooleanTypedValue.forValue(leftNumber.byteValue() < rightNumber.byteValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double comparison
				return BooleanTypedValue.forValue(leftNumber.doubleValue() < rightNumber.doubleValue());
			}
		}

		if (left instanceof CharSequence && right instanceof CharSequence) {
			left = left.toString();
			right = right.toString();
		}

		return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) < 0);
	}

	@Override
	public boolean isCompilable() {
		return isCompilableOperatorUsingNumerics();
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		generateComparisonCode(mv, cf, IFGE, IF_ICMPGE);
	}

}
