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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.internal.TypeCode;

/**
 * Represents a reference to a type, for example "T(String)" or "T(com.somewhere.Foo)"
 * 
 * @author Andy Clement
 * 
 */
public class TypeReference extends SpelNode {

	public TypeReference(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		// TODO possible optimization here if we cache the discovered type reference, but can we do that?
		String typename = (String) getChild(0).getValue(state);
		if (typename.indexOf(".") == -1 && Character.isLowerCase(typename.charAt(0))) {
			TypeCode tc = TypeCode.forName(typename);
			if (tc != TypeCode.OBJECT) {
				// it is a primitive type
				return tc.getType();
			}
		}
		return state.findType(typename);
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("T(");
		sb.append(getChild(0).toStringAST());
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
