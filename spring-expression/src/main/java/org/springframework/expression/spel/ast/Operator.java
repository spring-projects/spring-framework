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


/**
 * Common supertype for operators that operate on either one or two operands. In the case of multiply or divide there
 * would be two operands, but for unary plus or minus, there is only one.
 *
 * @author Andy Clement
 * @since 3.0
 */
public abstract class Operator extends SpelNodeImpl {

	String operatorName;

	public Operator(String payload,int pos,SpelNodeImpl... operands) {
		super(pos, operands);
		this.operatorName = payload;
	}

	public SpelNodeImpl getLeftOperand() {
		return children[0];
	}

	public SpelNodeImpl getRightOperand() {
		return children[1];
	}

	public final String getOperatorName() {
		return operatorName;
	}

	/**
	 * String format for all operators is the same '(' [operand] [operator] [operand] ')'
	 */
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(getChild(0).toStringAST());
		for (int i = 1; i < getChildCount(); i++) {
			sb.append(" ").append(getOperatorName()).append(" ");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

}
