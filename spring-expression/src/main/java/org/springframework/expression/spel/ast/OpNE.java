/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Implements the not-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpNE extends Operator {

	public OpNE(int pos, SpelNodeImpl... operands) {
		super("!=", pos, operands);
		this.exitTypeDescriptor = "Z";
	}

	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		leftActualDescriptor = CodeFlow.toDescriptorFromObject(left);
		rightActualDescriptor = CodeFlow.toDescriptorFromObject(right);
		return BooleanTypedValue.forValue(!equalityCheck(state, left, right));
	}

	// This check is different to the one in the other numeric operators (OpLt/etc)
	// because we allow simple object comparison
	@Override
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right= getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}

		String leftDesc = left.exitTypeDescriptor;
		String rightDesc = right.exitTypeDescriptor;
		DescriptorComparison dc =  DescriptorComparison.checkNumericCompatibility(leftDesc, rightDesc, leftActualDescriptor, rightActualDescriptor);
		return (!dc.areNumbers || dc.areCompatible);
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		String rightDesc = getRightOperand().exitTypeDescriptor;
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		boolean leftPrim = CodeFlow.isPrimitive(leftDesc);
		boolean rightPrim = CodeFlow.isPrimitive(rightDesc);

		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(leftDesc, rightDesc, leftActualDescriptor, rightActualDescriptor);
		
		if (dc.areNumbers && dc.areCompatible) {
			char targetType = dc.compatibleType;
			
			getLeftOperand().generateCode(mv, cf);
			if (!leftPrim) {
				CodeFlow.insertUnboxInsns(mv, targetType, leftDesc);
			}
		
			cf.enterCompilationScope();
			getRightOperand().generateCode(mv, cf);
			cf.exitCompilationScope();
			if (!rightPrim) {
				CodeFlow.insertUnboxInsns(mv, targetType, rightDesc);
			}
			// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
			if (targetType == 'D') {
				mv.visitInsn(DCMPL);
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType == 'F') {
				mv.visitInsn(FCMPL);		
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType == 'J') {
				mv.visitInsn(LCMP);		
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType == 'I' || targetType == 'Z') {
				mv.visitJumpInsn(IF_ICMPEQ, elseTarget);		
			}
			else {
				throw new IllegalStateException("Unexpected descriptor "+leftDesc);
			}
		}
		else {
			getLeftOperand().generateCode(mv, cf);
			getRightOperand().generateCode(mv, cf);
			mv.visitJumpInsn(IF_ACMPEQ, elseTarget);
		}
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		cf.pushDescriptor("Z");
	}

}
