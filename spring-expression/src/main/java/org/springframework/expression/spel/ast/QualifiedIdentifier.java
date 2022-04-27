/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.lang.Nullable;

/**
 * Represents a dot separated sequence of strings that indicate a package qualified type
 * reference.
 *
 * <p>Example: "java.lang.String" as in the expression "new java.lang.String('hello')"
 *
 * @author Andy Clement
 * @since 3.0
 */
public class QualifiedIdentifier extends SpelNodeImpl {

	@Nullable
	private TypedValue value;


	public QualifiedIdentifier(int startPos, int endPos, SpelNodeImpl... operands) {
		super(startPos, endPos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		// Cache the concatenation of child identifiers
		if (this.value == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < getChildCount(); i++) {
				Object value = this.children[i].getValueInternal(state).getValue();
				if (i > 0 && (value == null || !value.toString().startsWith("$"))) {
					sb.append('.');
				}
				sb.append(value);
			}
			this.value = new TypedValue(sb.toString());
		}
		return this.value;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (this.value != null) {
			sb.append(this.value.getValue());
		}
		else {
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0) {
					sb.append('.');
				}
				sb.append(getChild(i).toStringAST());
			}
		}
		return sb.toString();
	}

}
