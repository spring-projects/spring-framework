/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;

/**
 * Expression language AST node that represents null.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class NullLiteral extends Literal {

	public NullLiteral(int startPos, int endPos) {
		super(null, startPos, endPos);
		this.exitTypeDescriptor = "Ljava/lang/Object";
	}


	@Override
	public TypedValue getLiteralValue() {
		return TypedValue.NULL;
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean isCompilable() {
		return true;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		mv.visitInsn(ACONST_NULL);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
