package org.springframework.expression.spel.ast;

import java.math.BigDecimal;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.BigNumberConcern;

@BigNumberConcern
public class BigDecimalLiteral extends Literal {
	private final TypedValue typedValue;

	public BigDecimalLiteral(final String originalValue, final int startPos, final int endPos, final BigDecimal value) {
		super(originalValue, startPos, endPos);
		this.typedValue = new TypedValue(value);
	}

	@Override
	public TypedValue getLiteralValue() {
		return typedValue;
	}
}
