/*
 * Copyright 2002-present the original author or authors.
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
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * Implements the {@code multiply} operator.
 *
 * <p>Conversions and promotions are handled as defined in
 * <a href="https://java.sun.com/docs/books/jls/third_edition/html/conversions.html">Section 5.6.2 of the
 * Java Language Specification</a>, with the addiction of {@code BigDecimal}/{@code BigInteger} management:
 *
 * <p>If any of the operands is of a reference type, unboxing conversion (Section 5.1.8)
 * is performed. Then:<br>
 * If either operand is of type {@code BigDecimal}, the other is converted to {@code BigDecimal}.<br>
 * If either operand is of type double, the other is converted to double.<br>
 * Otherwise, if either operand is of type float, the other is converted to float.<br>
 * If either operand is of type {@code BigInteger}, the other is converted to {@code BigInteger}.<br>
 * Otherwise, if either operand is of type long, the other is converted to long.<br>
 * Otherwise, both operands are converted to type int.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpMultiply extends Operator {

	/**
	 * Maximum number of characters permitted in repeated text.
	 * @since 5.2.23
	 */
	private static final int MAX_REPEATED_TEXT_SIZE = 256;


	public OpMultiply(int startPos, int endPos, SpelNodeImpl... operands) {
		super("*", startPos, endPos, operands);
	}


	/**
	 * Implements the {@code multiply} operator directly here for certain types
	 * of supported operands and otherwise delegates to any registered overloader
	 * for types not supported here.
	 * <p>Supported operand types:
	 * <ul>
	 * <li>numbers
	 * <li>String and int ('abc' * 2 == 'abcabc')
	 * </ul>
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
		Object rightOperand = getRightOperand().getValueInternal(state).getValue();

		if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.multiply(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.multiply(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double multiplication
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String text && rightOperand instanceof Integer count) {
			checkRepeatedTextSize(text, count);
			return new TypedValue(text.repeat(count));
		}

		return state.operate(Operation.MULTIPLY, leftOperand, rightOperand);
	}

	private void checkRepeatedTextSize(String text, int count) {
		if (count < 0) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.NEGATIVE_REPEATED_TEXT_COUNT, count);
		}
		int result = text.length() * count;
		if (result < 0 || result > MAX_REPEATED_TEXT_SIZE) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_REPEATED_TEXT_SIZE_EXCEEDED, MAX_REPEATED_TEXT_SIZE);
		}
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length > 1) {
			if (!getRightOperand().isCompilable()) {
				return false;
			}
		}
		return (this.exitTypeDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		String exitDesc = this.exitTypeDescriptor;
		Assert.state(exitDesc != null, "No exit type descriptor");
		char targetDesc = exitDesc.charAt(0);
		CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
		if (this.children.length > 1) {
			cf.enterCompilationScope();
			getRightOperand().generateCode(mv, cf);
			String rightDesc = getRightOperand().exitTypeDescriptor;
			cf.exitCompilationScope();
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
			switch (targetDesc) {
				case 'I' -> mv.visitInsn(IMUL);
				case 'J' -> mv.visitInsn(LMUL);
				case 'F' -> mv.visitInsn(FMUL);
				case 'D' -> mv.visitInsn(DMUL);
				default -> throw new IllegalStateException(
						"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
