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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents the between operator. The left operand to between must be a single value and the right operand must be a
 * list - this operator returns true if the left operand is between (using the registered comparator) the two elements
 * in the list.
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

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
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
			// TODO between is inclusive, is that OK
			return (comparator.compare(left, low) >= 0 && comparator.compare(left, high) <= 0);
		} catch (SpelException ee) {
			ee.setPosition(getCharPositionInLine());
			throw ee;
		}
	}

}
