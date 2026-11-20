/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents a NOT operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Oliver Becker
 * @since 3.0
 */
public class OperatorNot extends SpelNodeImpl {  // Not is a unary operator so does not extend BinaryOperator

	public OperatorNot(int startPos, int endPos, SpelNodeImpl operand) {
		super(startPos, endPos, operand);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		try {
			Boolean value = this.children[0].getValue(state, Boolean.class);
			if (value == null) {
				throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
			}
			state.trackOperation();
			return BooleanTypedValue.forValue(!value);
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getChild(0).getStartPosition());
			throw ex;
		}
	}

	@Override
	public String toStringAST() {
		return "!" + getChild(0).toStringAST();
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl child = this.children[0];
		return (child.isCompilable() && CodeFlow.isBooleanCompatible(child.exitTypeDescriptor));
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		this.children[0].generateCode(mv, cf);
		cf.unboxBooleanIfNecessary(mv);
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		mv.visitJumpInsn(IFNE,elseTarget);
		mv.visitInsn(ICONST_1); // TRUE
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0); // FALSE
		mv.visitLabel(endOfIf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
