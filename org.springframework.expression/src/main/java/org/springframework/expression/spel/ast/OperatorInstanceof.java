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
import org.springframework.expression.spel.SpelMessages;

/**
 * The operator 'instanceof' checks if an object is of the class specified in the right hand operand, in the same way
 * that instanceof does in Java.
 * 
 * @author Andy Clement
 */
public class OperatorInstanceof extends Operator {

	public OperatorInstanceof(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "instanceof";
	}

	/**
	 * Compare the left operand to see it is an instance of the type specified as the right operand. The right operand
	 * must be a class.
	 * 
	 * @param state the expression state
	 * @return true if the left operand is an instanceof of the right operand, otherwise false
	 * @throws EvaluationException if there is a problem evaluating the expression
	 */
	@Override
	public Boolean getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		if (left == null) {
			return false; // null is not an instanceof anything
		}
		if (right == null || !(right instanceof Class<?>)) {
			throw new SpelException(getRightOperand().getCharPositionInLine(),
					SpelMessages.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND, (right == null ? "null" : right.getClass().getName()));
		}
		Class<?> rightClass = (Class<?>) right;
		return rightClass.isAssignableFrom(left.getClass());
	}

}
