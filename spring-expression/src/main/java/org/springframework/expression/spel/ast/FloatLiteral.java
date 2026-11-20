/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;

/**
 * Expression language AST node that represents a float literal.
 *
 * @author Satyapal Reddy
 * @author Andy Clement
 * @since 3.2
 */
public class FloatLiteral extends Literal {

	private final TypedValue value;


	public FloatLiteral(String payload, int startPos, int endPos, float value) {
		super(payload, startPos, endPos);
		this.value = new TypedValue(value);
		this.exitTypeDescriptor = "F";
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
		mv.visitLdcInsn(this.value.getValue());
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
