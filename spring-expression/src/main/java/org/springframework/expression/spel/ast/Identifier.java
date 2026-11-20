/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;

/**
 * An 'identifier' {@link SpelNode}.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class Identifier extends SpelNodeImpl {

	private final TypedValue id;


	public Identifier(String payload, int startPos, int endPos) {
		super(startPos, endPos);
		this.id = new TypedValue(payload);
	}


	@Override
	public String toStringAST() {
		return String.valueOf(this.id.getValue());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) {
		return this.id;
	}

}
