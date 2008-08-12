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
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.generated.SpringExpressionsLexer;

/**
 * Represent a object reference of the form '@(<contextName>:<objectName>)'
 * 
 */
public class Reference extends SpelNode {

	private boolean contextAndObjectDetermined = false;
	private SpelNode contextNode = null;
	private SpelNode objectNode = null;

	public Reference(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {

		ensureContextAndNameDetermined();
		Object contextName = (contextNode == null ? null : contextNode.getValue(state));
		Object objectName = (objectNode == null ? null : objectNode.getValue(state));

		Object referencedValue = state.lookupReference(contextName, objectName);

		return referencedValue;
	}

	/**
	 * Work out which represents the context and which the object. This would be trivial except for parser recovery
	 * situations where the expression was incomplete. We need to do our best here to recover so that we can offer
	 * suitable code completion suggestions.
	 */
	private void ensureContextAndNameDetermined() {
		if (contextAndObjectDetermined)
			return;
		contextAndObjectDetermined = true;
		int colon = -1;
		for (int i = 0; i < getChildCount(); i++) {
			if (getChild(i).getToken().getType() == SpringExpressionsLexer.COLON) {
				colon = i;
			}
		}
		if (colon != -1) {
			contextNode = getChild(colon - 1);
			objectNode = getChild(colon + 1);
		} else {
			objectNode = getChild(0);
		}
		if (objectNode.getToken().getType() != SpringExpressionsLexer.QUALIFIED_IDENTIFIER) {
			objectNode = null;
		}
	}

	@Override
	public String toStringAST() {
		ensureContextAndNameDetermined();
		StringBuilder sb = new StringBuilder();
		sb.append("@(");
		if (contextNode != null) {
			sb.append(contextNode.toStringAST()).append(":");
		}
		sb.append(objectNode.toStringAST());
		sb.append(")");
		return sb.toString();
	}

}
