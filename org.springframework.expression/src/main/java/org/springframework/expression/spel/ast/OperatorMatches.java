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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Implements the matches operator. Matches takes two operands. The first is a string and the second is a java regex. It
 * will return true when getValue() is called if the first operand matches the regex.
 * 
 * @author Andy Clement
 */
public class OperatorMatches extends Operator {

	public OperatorMatches(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "matches";
	}

	/**
	 * Check the first operand matches the regex specified as the second operand.
	 * 
	 * @param state the expression state
	 * @return true if the first operand matches the regex specified as the second operand, otherwise false
	 * @throws EvaluationException if there is a problem evaluating the expression (e.g. the regex is invalid)
	 */
	@Override
	public Boolean getValue(ExpressionState state) throws EvaluationException {
		SpelNode leftOp = getLeftOperand();
		SpelNode rightOp = getRightOperand();
		Object left = leftOp.getValue(state, String.class);
		Object right = getRightOperand().getValue(state);
		try {
			if (!(left instanceof String)) {
				throw new SpelException(leftOp.getCharPositionInLine(),
						SpelMessages.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, left);
			}
			if (!(right instanceof String)) {
				throw new SpelException(rightOp.getCharPositionInLine(),
						SpelMessages.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, right);
			}
			Pattern pattern = Pattern.compile((String) right);
			Matcher matcher = pattern.matcher((String) left);
			return matcher.matches();
		} catch (PatternSyntaxException pse) {
			throw new SpelException(rightOp.getCharPositionInLine(), pse, SpelMessages.INVALID_PATTERN, right);
		}
	}

}
