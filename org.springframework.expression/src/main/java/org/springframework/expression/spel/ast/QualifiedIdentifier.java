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

/**
 * Represents a dot separated sequence of strings that indicate a package qualified type reference.
 * <p>
 * Example: "java.lang.String" as in the expression "new java.lang.String('hello')"
 * 
 * @author Andy Clement
 * 
 */
public class QualifiedIdentifier extends SpelNode {

	private String value;

	public QualifiedIdentifier(Token payload) {
		super(payload);
		// value = payload.getText();
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		// Cache the concatenation of child identifiers
		if (value == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0)
					sb.append(".");
				sb.append(getChild(i).getValue(state));
			}
			value = sb.toString();
		}
		return value;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (value != null) {
			sb.append(value);
		} else {
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0)
					sb.append(".");
				sb.append(getChild(i).toStringAST());
			}
		}
		return sb.toString();
	}

}
