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

import java.util.Collection;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents the 'in' operator and returns true if the left operand can be found within the collection passed as the
 * right operand.
 * 
 * @author Andy Clement
 */
public class OperatorIn extends Operator {

	public OperatorIn(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "in";
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		if (right instanceof Collection<?>) {
			Collection<?> c = (Collection<?>) right;
			for (Object element : c) {
				if (state.getTypeComparator().compare(left, element) == 0) {
					return true;
				}
			}
			return false;
		}
		throw new SpelException(SpelMessages.OPERATOR_IN_CANNOT_DETERMINE_MEMBERSHIP, left.getClass().getName(), right
				.getClass().getName());
	}

}
