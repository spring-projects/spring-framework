/*
 * Copyright 2002-2015 the original author or authors.
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
import java.math.RoundingMode;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.NumberUtils;

/**
 * Implements division operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpDivide extends Operator {

	public OpDivide(int pos, SpelNodeImpl... operands) {
		super("/", pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
		Object rightOperand = getRightOperand().getValueInternal(state).getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				int scale = Math.max(leftBigDecimal.scale(), rightBigDecimal.scale());
				return new TypedValue(leftBigDecimal.divide(rightBigDecimal, scale, RoundingMode.HALF_EVEN));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() / rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() / rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.divide(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() / rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() / rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double division
				return new TypedValue(leftNumber.doubleValue() / rightNumber.doubleValue());
			}
		}

		return state.operate(Operation.DIVIDE, leftOperand, rightOperand);
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
		CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, this.exitTypeDescriptor.charAt(0));
		if (this.children.length > 1) {
			cf.enterCompilationScope();
			getRightOperand().generateCode(mv, cf);
			String rightDesc = getRightOperand().exitTypeDescriptor;
			cf.exitCompilationScope();
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, this.exitTypeDescriptor.charAt(0));
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(IDIV);
					break;
				case 'J':
					mv.visitInsn(LDIV);
					break;
				case 'F': 
					mv.visitInsn(FDIV);
					break;
				case 'D':
					mv.visitInsn(DDIV);
					break;				
				default:
					throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
