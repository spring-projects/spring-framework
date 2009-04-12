/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.Serializable;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.generated.SpringExpressionsParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * The common supertype of all AST nodes in a parsed Spring Expression Language format expression.
 *
 * @author Andy Clement
 * @since 3.0
 */
public abstract class SpelNodeImpl extends CommonTree implements SpelNode, Serializable, CommonTypeDescriptors {
	
	/**
	 * The Antlr parser uses this constructor to build SpelNodes.
	 * @param payload the token for the node that has been parsed
	 */
	protected SpelNodeImpl(Token payload) {
		super(payload);
	}

	public final Object getValue(ExpressionState expressionState) throws EvaluationException {
		if (expressionState != null) {
			return getValueInternal(expressionState).getValue();
		} else {
			return getValue(new ExpressionState(new StandardEvaluationContext()));
		}
	}

	// by default Ast nodes are not writable
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	public void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException {
		throw new SpelException(
				getCharPositionInLine(), SpelMessages.SETVALUE_NOT_SUPPORTED, getClass(), getTokenName());
	}

	protected String getTokenName() {
		if (getToken() == null) {
			return "UNKNOWN";
		}
		return SpringExpressionsParser.tokenNames[getToken().getType()];
	}

	@Override
	public SpelNodeImpl getChild(int index) {
		return (SpelNodeImpl) super.getChild(index);
	}

	public Class<?> getObjectClass(Object obj) {
		if (obj == null) {
			return null;
		}
		return (obj instanceof Class ? ((Class<?>) obj) : obj.getClass());
	}

	@SuppressWarnings("unchecked")
	protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
		Object result = getValueInternal(state).getValue();
		if (result != null && desiredReturnType != null) {
			Class<?> resultType = result.getClass();
			if (desiredReturnType.isAssignableFrom(resultType)) {
				return (T) result;
			}
			// Attempt conversion to the requested type, may throw an exception
			return ExpressionUtils.convert(state.getEvaluationContext(), result, desiredReturnType);
		}
		return (T) result;
	}

	public int getStartPosition() {
		return getCharPositionInLine();
	}


	public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;

	public abstract String toStringAST();

}
