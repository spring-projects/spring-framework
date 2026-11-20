/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
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

	public OpNE(int startPos, int endPos, SpelNodeImpl... operands) {
		super("!=", startPos, endPos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object leftValue = getLeftOperand().getValueInternal(state).getValue();
		Object rightValue = getRightOperand().getValueInternal(state).getValue();
		this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(leftValue);
		this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(rightValue);
		state.trackOperation();
		return BooleanTypedValue.forValue(!equalityCheck(state.getEvaluationContext(), leftValue, rightValue));
	}

	// This check is different to the one in the other numeric operators (OpLt/etc)
	// because we allow simple object comparison
	@Override
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right = getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}

		String leftDesc = left.exitTypeDescriptor;
		String rightDesc = right.exitTypeDescriptor;
		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(leftDesc,
				rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
		return (!dc.areNumbers || dc.areCompatible);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		cf.loadEvaluationContext(mv);
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		String rightDesc = getRightOperand().exitTypeDescriptor;

		cf.enterCompilationScope();
		getLeftOperand().generateCode(mv, cf);
		cf.exitCompilationScope();
		if (CodeFlow.isPrimitive(leftDesc)) {
			CodeFlow.insertBoxIfNecessary(mv, leftDesc.charAt(0));
		}
		cf.enterCompilationScope();
		getRightOperand().generateCode(mv, cf);
		cf.exitCompilationScope();
		if (CodeFlow.isPrimitive(rightDesc)) {
			CodeFlow.insertBoxIfNecessary(mv, rightDesc.charAt(0));
		}

		String operatorClassName = Operator.class.getName().replace('.', '/');
		String evaluationContextClassName = EvaluationContext.class.getName().replace('.', '/');
		mv.visitMethodInsn(INVOKESTATIC, operatorClassName, "equalityCheck",
				"(L" + evaluationContextClassName + ";Ljava/lang/Object;Ljava/lang/Object;)Z", false);

		// Invert the boolean
		Label notZero = new Label();
		Label end = new Label();
		mv.visitJumpInsn(IFNE, notZero);
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO, end);
		mv.visitLabel(notZero);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(end);

		cf.pushDescriptor("Z");
	}

}
