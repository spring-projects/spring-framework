/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.util.StringUtils;

/**
 * Expression language AST node that represents a string literal.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class StringLiteral extends Literal {

	private final TypedValue value;


	public StringLiteral(String payload, int startPos, int endPos, String value) {
		super(payload, startPos, endPos);

		// The original enclosing quote character for the string literal: ' or ".
		char quoteCharacter = value.charAt(0);

		// Remove enclosing quotes
		String valueWithinQuotes = value.substring(1, value.length() - 1);

		// Replace escaped internal quote characters
		if (quoteCharacter == '\'') {
			valueWithinQuotes = StringUtils.replace(valueWithinQuotes, "''", "'");
		}
		else {
			valueWithinQuotes = StringUtils.replace(valueWithinQuotes, "\"\"", "\"");
		}

		this.value = new TypedValue(valueWithinQuotes);
		this.exitTypeDescriptor = "Ljava/lang/String";
	}


	@Override
	public TypedValue getLiteralValue() {
		return this.value;
	}

	@Override
	public String toString() {
		String ast = String.valueOf(getLiteralValue().getValue());
		ast = StringUtils.replace(ast, "'", "''");
		return "'" + ast + "'";
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
