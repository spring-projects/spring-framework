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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.generated.SpringExpressionsParser;

/**
 * The common supertype of all AST nodes in a parsed Spring Expression Language format expression.
 * 
 * @author Andy Clement
 * 
 */
public abstract class SpelNode extends CommonTree implements Serializable {

	/**
	 * The Antlr parser uses this constructor to build SpelNodes.
	 * 
	 * @param payload the token for the node that has been parsed
	 */
	protected SpelNode(Token payload) {
		super(payload);
	}

	/**
	 * Evaluate the expression node in the context of the supplied expression state and return the value.
	 * 
	 * @param expressionState the current expression state (includes the context)
	 * @return the value of this node evaluated against the specified state
	 */
	public abstract Object getValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * Determine if this expression node will support a setValue() call.
	 * 
	 * @param expressionState the current expression state (includes the context)
	 * @return true if the expression node will allow setValue()
	 * @throws EvaluationException if something went wrong trying to determine if the node supports writing
	 */
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	/**
	 * Evaluate the expression to a node and then set the new value on that node. For example, if the expression
	 * evaluates to a property reference then the property will be set to the new value.
	 * 
	 * @param expressionState the current expression state (includes the context)
	 * @param newValue the new value
	 * @throws EvaluationException if any problem occurs evaluating the expression or setting the new value
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

	/**
	 * @return the string form of this AST node
	 */
	public abstract String toStringAST();

	/**
	 * Helper method that returns a SpelNode rather than an Antlr Tree node.
	 * 
	 * @return the child node cast to a SpelNode
	 */
	@Override
	public SpelNode getChild(int index) {
		return (SpelNode) super.getChild(index);
	}

	/**
	 * Determine the class of the object passed in, unless it is already a class object.
	 * @param o the object that the caller wants the class of
	 * @return the class of the object if it is not already a class object, or null if the object is null
	 */
	public Class<?> getObjectClass(Object o) {
		if (o == null)
			return null;
		return (o instanceof Class) ? ((Class<?>) o) : o.getClass();
	}

	protected final Object getValue(ExpressionState state, Class<?> desiredReturnType) throws EvaluationException {
		Object result = getValue(state);
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
}
