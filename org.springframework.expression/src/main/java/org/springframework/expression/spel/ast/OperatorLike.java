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
 * Implements the like operator. The like operator behaves the same as the SQL LIKE operator. The first operand is
 * compared against the expression supplied as the second operand. This expression supports two wildcards: % meaning any
 * string of any length, and _ meaning any single character.
 * 
 * @author Andy Clement
 */
public class OperatorLike extends Operator {

	public OperatorLike(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "like";
	}

	@Override
	public Boolean getValue(ExpressionState state) throws EvaluationException {
		SpelNode leftOp = getLeftOperand();
		SpelNode rightOp = getRightOperand();
		Object left = leftOp.getValue(state, String.class);
		Object right = getRightOperand().getValue(state);
		try {
			if (!(left instanceof String)) {
				throw new SpelException(leftOp.getCharPositionInLine(),
						SpelMessages.INVALID_FIRST_OPERAND_FOR_LIKE_OPERATOR, left);
			}
			if (!(right instanceof String)) {
				throw new SpelException(rightOp.getCharPositionInLine(),
						SpelMessages.INVALID_SECOND_OPERAND_FOR_LIKE_OPERATOR, right);
			}
			// Translate that pattern to a java regex
			// not really the best option, what if the right operand already had regex related chars in it?
			String likePattern = (String) right;
			likePattern = likePattern.replace('_', '.');
			likePattern = likePattern.replaceAll("%", ".*");
			Pattern pattern = Pattern.compile(likePattern);
			Matcher matcher = pattern.matcher((String) left);
			return matcher.matches();
		} catch (PatternSyntaxException pse) {
			throw new SpelException(rightOp.getCharPositionInLine(), pse, SpelMessages.INVALID_PATTERN, right);
		}
	}
}
