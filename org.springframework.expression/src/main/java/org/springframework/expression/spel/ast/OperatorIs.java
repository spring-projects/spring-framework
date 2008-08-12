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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * The operator 'is' checks if an object is of the class specified in the right hand operand, in the same way that
 * instanceof does in Java.
 * 
 * @author Andy Clement
 * 
 */
public class OperatorIs extends Operator {
	// TODO should 'is' change to 'instanceof' ?

	public OperatorIs(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "is";
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		if (!(right instanceof Class<?>)) {
			throw new SpelException(getRightOperand().getCharPositionInLine(),
					SpelMessages.IS_OPERATOR_NEEDS_CLASS_OPERAND, right.getClass().getName());
		}
		// TODO Could this defer to type utilities? What would be the benefit?
		return (((Class<?>) right).isAssignableFrom(left.getClass()));
	}

}
