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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Implements the matches operator. Matches takes two operands. The first is a string and the second is a java regex. It
 * will return true when getValue() is called if the first operand matches the regex.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorMatches extends Operator {

	public OperatorMatches(int pos, SpelNodeImpl... operands) {
		super("matches", pos, operands);
	}

	/**
	 * Check the first operand matches the regex specified as the second operand.
	 * @param state the expression state
	 * @return true if the first operand matches the regex specified as the second operand, otherwise false
	 * @throws EvaluationException if there is a problem evaluating the expression (e.g. the regex is invalid)
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();
		Object left = leftOp.getValue(state, String.class);
		Object right = getRightOperand().getValueInternal(state).getValue();
		try {
			if (!(left instanceof String)) {
				throw new SpelEvaluationException(leftOp.getStartPosition(),
						SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, left);
			}
			if (!(right instanceof String)) {
				throw new SpelEvaluationException(rightOp.getStartPosition(),
						SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, right);
			}
			Pattern pattern = Pattern.compile((String) right);
			Matcher matcher = pattern.matcher((String) left);
			return BooleanTypedValue.forValue(matcher.matches());
		}
		catch (PatternSyntaxException pse) {
			throw new SpelEvaluationException(rightOp.getStartPosition(), pse, SpelMessage.INVALID_PATTERN, right);
		}
	}

}
