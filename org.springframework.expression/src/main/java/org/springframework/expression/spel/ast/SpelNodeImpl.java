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

import java.io.Serializable;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.generated.SpringExpressionsParser;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * The common supertype of all AST nodes in a parsed Spring Expression Language format expression.
 * 
 * @author Andy Clement
 * 
 */
public abstract class SpelNodeImpl extends CommonTree implements Serializable, SpelNode {

	/**
	 * The Antlr parser uses this constructor to build SpelNodes.
	 * 
	 * @param payload the token for the node that has been parsed
	 */
	protected SpelNodeImpl(Token payload) {
		super(payload);
	}

	public final Object getValue(ExpressionState expressionState) throws EvaluationException {
		if (expressionState==null) {
			return getValue(new ExpressionState(new StandardEvaluationContext()));
		} else {
			return getValueInternal(expressionState);
		}
	}


	
	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#getValue(org.springframework.expression.spel.ExpressionState)
	 */
	public abstract Object getValueInternal(ExpressionState expressionState) throws EvaluationException;

	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#isWritable(org.springframework.expression.spel.ExpressionState)
	 */
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#setValue(org.springframework.expression.spel.ExpressionState, java.lang.Object)
	 */
	public void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException {
		throw new SpelException(getCharPositionInLine(), SpelMessages.SETVALUE_NOT_SUPPORTED, getClass(),
				getTokenName());
	}

	/**
	 * @return return the token this node represents
	 */
	protected String getTokenName() {
		if (getToken() == null) {
			return "UNKNOWN";
		}
		return SpringExpressionsParser.tokenNames[getToken().getType()];
	}

	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#toStringAST()
	 */
	public abstract String toStringAST();

	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#getChild(int)
	 */
	@Override
	public SpelNodeImpl getChild(int index) {
		return (SpelNodeImpl) super.getChild(index);
	}

	/* (non-Javadoc)
	 * @see org.springframework.expression.spel.ast.ISpelNode#getObjectClass(java.lang.Object)
	 */
	public Class<?> getObjectClass(Object o) {
		if (o == null)
			return null;
		return (o instanceof Class) ? ((Class<?>) o) : o.getClass();
	}

	protected final Object getValue(ExpressionState state, Class<?> desiredReturnType) throws EvaluationException {
		Object result = getValueInternal(state);
		if (result != null && desiredReturnType != null) {
			Class<?> resultType = result.getClass();
			if (desiredReturnType.isAssignableFrom(resultType)) {
				return result;
			}
			// Attempt conversion to the requested type, may throw an exception
			return ExpressionUtils.convert(state.getEvaluationContext(), result, desiredReturnType);
		}
		return result;
	}

	public int getStartPosition() {
		return getCharPositionInLine();
	}
}
