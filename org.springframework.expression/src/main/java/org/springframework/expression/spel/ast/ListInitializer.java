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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

public class ListInitializer extends SpelNode {

	public ListInitializer(Token payload) {
		super(payload);
	}

	/**
	 * Evaluate the list initializer, returning a List<Object>
	 */
	@Override
	public List<Object> getValue(ExpressionState state) throws EvaluationException {
		List<Object> result = new ArrayList<Object>();
		for (int i = 0; i < getChildCount(); i++) {
			Object element = getChild(i).getValue(state);
			result.add(element);
		}
		return result;
	}

	/**
	 * Return string form of this node {<child>,<child>,<child>}
	 */
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append("}");
		return sb.toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
