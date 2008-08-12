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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * Common supertype for operators that operate on either one or two operands. In the case of multiply or divide there
 * would be two operands, but for unary plus or minus, there is only one.
 * 
 * @author Andy Clement
 */
public abstract class Operator extends SpelNode {

	public Operator(Token payload) {
		super(payload);
	}

	/**
	 * Operator expressions can never be written to
	 */
	@Override
	public final boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

	public SpelNode getLeftOperand() {
		return getChild(0);
	}

	public SpelNode getRightOperand() {
		return getChild(1);
	}

	public abstract String getOperatorName();

	/**
	 * String format for all operators is the same '(' [operand] [operator] [operand] ')'
	 */
	@Override
	public String toStringAST() {
		StringBuffer sb = new StringBuffer();
		if (getChildCount() > 0)
			sb.append("(");
		sb.append(getChild(0).toStringAST());
		for (int i = 1; i < getChildCount(); i++) {
			sb.append(" ").append(getOperatorName()).append(" ");
			sb.append(getChild(i).toStringAST());
		}
		if (getChildCount() > 0)
			sb.append(")");
		return sb.toString();
	}

}
