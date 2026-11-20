/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * Represents assignment. An alternative to calling {@code setValue}
 * for an expression which indicates an assign statement.
 *
 * <p>Example: 'someNumberProperty=42'
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public class Assign extends SpelNodeImpl {

	public Assign(int startPos, int endPos, SpelNodeImpl... operands) {
		super(startPos, endPos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (!state.getEvaluationContext().isAssignmentEnabled()) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.NOT_ASSIGNABLE, toStringAST());
		}
		state.trackOperation();
		return this.children[0].setValueInternal(state, () -> this.children[1].getValueInternal(state));
	}

	@Override
	public String toStringAST() {
		return getChild(0).toStringAST() + "=" + getChild(1).toStringAST();
	}

}
