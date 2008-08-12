/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents a DOT separated expression sequence, such as 'property1.property2.methodOne()'
 * 
 * @author Andy Clement
 * 
 */
public class CompoundExpression extends SpelNode {

	public CompoundExpression(Token payload) {
		super(payload);
	}

	/**
	 * Evalutes a compound expression. This involves evaluating each piece in turn and the return value from each piece
	 * is the active context object for the subsequent piece.
	 * 
	 * @param state the state in which the expression is being evaluated
	 * @return the final value from the last piece of the compound expression
	 */
	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object result = null;
		SpelNode nextNode = null;
		try {
			nextNode = getChild(0);
			result = nextNode.getValue(state);
			for (int i = 1; i < getChildCount(); i++) {
				try {
					state.pushActiveContextObject(result);
					nextNode = getChild(i);
					result = nextNode.getValue(state);
				} finally {
					state.popActiveContextObject();
				}
			}
		} catch (SpelException ee) {
			// Correct the position for the error before rethrowing
			ee.setPosition(nextNode.getCharPositionInLine());
			throw ee;
		}
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws EvaluationException {
		if (getChildCount() == 1) {
			getChild(0).setValue(state, value);
			return;
		}
		Object ctx = getChild(0).getValue(state);
		for (int i = 1; i < getChildCount() - 1; i++) {
			try {
				state.pushActiveContextObject(ctx);
				ctx = getChild(i).getValue(state);
			} finally {
				state.popActiveContextObject();
			}
		}
		try {
			state.pushActiveContextObject(ctx);
			getChild(getChildCount() - 1).setValue(state, value);
		} finally {
			state.popActiveContextObject();
		}
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		if (getChildCount() == 1) {
			return getChild(0).isWritable(state);
		}
		Object ctx = getChild(0).getValue(state);
		for (int i = 1; i < getChildCount() - 1; i++) {
			try {
				state.pushActiveContextObject(ctx);
				ctx = getChild(i).getValue(state);
			} finally {
				state.popActiveContextObject();
			}
		}
		try {
			state.pushActiveContextObject(ctx);
			return getChild(getChildCount() - 1).isWritable(state);
		} finally {
			state.popActiveContextObject();
		}
	}

	@Override
	public String toStringAST() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < getChildCount(); i++) {
			sb.append(getChild(i).toStringAST());
		}
		return sb.toString();
	}

}
