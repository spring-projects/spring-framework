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

import java.util.Collections;
import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represent a Lambda expression, eg. "{|x,y| $x > $y ? $x : $y }". It is possible for an expression to have zero
 * arguments in which case this expression node only has one child.
 * 
 * @author Andy Clement
 */
public class Lambda extends SpelNode {

	public Lambda(Token payload) {
		super(payload);
		// payload.setText("LambdaExpression");
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		return this;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (getChildCount() == 1) { // there are no arguments
			sb.append("{|| ");
			sb.append(getChild(0).toStringAST());
			sb.append(" }");
		} else {
			sb.append("{|");
			sb.append(getChild(0).toStringAST());
			sb.append("| ");
			sb.append(getChild(1).toStringAST());
			sb.append(" }");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return toStringAST();
	}

	public List<String> getArguments() {
		// Only one child means there are no arguments
		if (getChildCount() < 2) {
			return Collections.emptyList();
		}
		ArgList args = (ArgList) getChild(0);
		return args.getArgumentNames();
	}

	public Object getExpression() {
		return (getChildCount() > 1 ? getChild(1) : getChild(0));
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
