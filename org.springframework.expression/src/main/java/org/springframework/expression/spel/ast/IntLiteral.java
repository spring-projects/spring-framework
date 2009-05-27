package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;

/**
 * Expression language AST node that represents an integer literal.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class IntLiteral extends Literal {

	private final TypedValue value;

	IntLiteral(String payload, int pos, int value) {
		super(payload, pos); 
		this.value = new TypedValue(value, INTEGER_TYPE_DESCRIPTOR);
	}

	@Override
	public TypedValue getLiteralValue() {
		return this.value;
	}

}
