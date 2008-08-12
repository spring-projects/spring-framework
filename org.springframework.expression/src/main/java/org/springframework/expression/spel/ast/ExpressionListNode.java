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
 * Represents a list of expressions of the form "(expression1;expression2;expression3)". The expressions are always
 * evaluated from left to right, due to possible side effects that earlier expressions may have that influence the
 * evaluation of later expressions (defining functions, setting variables, etc).
 * 
 * @author Andy Clement
 * 
 */
public class ExpressionListNode extends SpelNode {

	public ExpressionListNode(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object result = null;
		for (int i = 0; i < getChildCount(); i++) {
			result = getChild(i).getValue(state);
		}
		return result;
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		boolean isWritable = false;
		if (getChildCount() > 0) {
			// Evaluate all but the last one
			for (int i = 0; i < getChildCount() - 1; i++) {
				getChild(i).getValue(state);
			}
			isWritable = getChild(getChildCount() - 1).isWritable(state);
		}
		return isWritable;
	}

	@Override
	public String toStringAST() {
		StringBuffer sb = new StringBuffer();
		if (getChildCount() == 1) {
			sb.append(getChild(0).toStringAST());
		} else {
			sb.append("(");
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0)
					sb.append(";");
				sb.append(getChild(i).toStringAST());
			}
			sb.append(")");
		}
		return sb.toString();
	}

}
