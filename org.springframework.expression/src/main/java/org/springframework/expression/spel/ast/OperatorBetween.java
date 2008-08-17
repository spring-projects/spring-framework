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

import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Represents the between operator. The left operand to between must be a single value and the right operand must be a
 * list - this operator returns true if the left operand is between (using the registered comparator) the two elements
 * in the list. The definition of between being inclusive follows the SQL BETWEEN definition.
 * 
 * @author Andy Clement
 */
public class OperatorBetween extends Operator {

	public OperatorBetween(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "between";
	}

	/**
	 * Returns a boolean based on whether a value is in the range expressed. The first operand is any value whilst the
	 * second is a list of two values - those two values being the bounds allowed for the first operand (inclusive).
	 * 
	 * @param state the expression state
	 * @return true if the left operand is in the range specified, false otherwise
	 * @throws EvaluationException if there is a problem evaluating the expression
	 */
	@Override
	public Boolean getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		if (!(right instanceof List) || ((List<?>) right).size() != 2) {
			throw new SpelException(getRightOperand().getCharPositionInLine(),
					SpelMessages.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
		}
		List<?> l = (List<?>) right;
		Object low = l.get(0);
		Object high = l.get(1);
		TypeComparator comparator = state.getTypeComparator();
		try {
			return (comparator.compare(left, low) >= 0 && comparator.compare(left, high) <= 0);
		} catch (SpelException ex) {
			ex.setPosition(getCharPositionInLine());
			throw ex;
		}
	}

}
