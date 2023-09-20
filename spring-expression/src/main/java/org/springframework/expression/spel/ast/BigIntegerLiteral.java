package org.springframework.expression.spel.ast;

import java.math.BigInteger;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.BigNumberConcern;

@BigNumberConcern
public class BigIntegerLiteral extends Literal {
	private final TypedValue typedValue;

	public BigIntegerLiteral(final String originalValue, final int startPos, final int endPos, final BigInteger value) {
		super(originalValue, startPos, endPos);
		this.typedValue = new TypedValue(value);
	}

	@Override
	public TypedValue getLiteralValue() {
		return typedValue;
	}
}
