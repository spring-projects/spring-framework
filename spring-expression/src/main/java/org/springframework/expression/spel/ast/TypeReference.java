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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents a reference to a type, for example "T(String)" or "T(com.somewhere.Foo)"
 * 
 * @author Andy Clement
 */
public class TypeReference extends SpelNodeImpl {

	public TypeReference(int pos,SpelNodeImpl qualifiedId) {
		super(pos,qualifiedId);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		// TODO possible optimization here if we cache the discovered type reference, but can we do that?
		String typename = (String) children[0].getValueInternal(state).getValue();
		if (typename.indexOf(".") == -1 && Character.isLowerCase(typename.charAt(0))) {
			TypeCode tc = TypeCode.valueOf(typename.toUpperCase());
			if (tc != TypeCode.OBJECT) {
				// it is a primitive type
				return new TypedValue(tc.getType());
			}
		}
		return new TypedValue(state.findType(typename));
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("T(");
		sb.append(getChild(0).toStringAST());
		sb.append(")");
		return sb.toString();
	}
	
}
