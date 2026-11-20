/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.util.Assert;

/**
 * Expression language AST node that represents an integer literal.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class IntLiteral extends Literal {

	private final TypedValue value;


	public IntLiteral(String payload, int startPos, int endPos, int value) {
		super(payload, startPos, endPos);
		this.value = new TypedValue(value);
		this.exitTypeDescriptor = "I";
	}


	@Override
	public TypedValue getLiteralValue() {
		return this.value;
	}

	@Override
	public boolean isCompilable() {
		return true;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		Integer intValue = (Integer) this.value.getValue();
		Assert.state(intValue != null, "No int value");
		if (intValue == -1) {
			// Not sure we can get here because -1 is OpMinus
			mv.visitInsn(ICONST_M1);
		}
		else if (intValue >= 0 && intValue < 6) {
			mv.visitInsn(ICONST_0 + intValue);
		}
		else {
			mv.visitLdcInsn(intValue);
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
