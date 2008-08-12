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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

// TODO what should be the difference between like and matches?
/**
 * Implements the matches operator.
 * 
 * @author Andy Clement
 */
public class OperatorMatches extends Operator {

	public OperatorMatches(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		try {
			Pattern pattern = Pattern.compile((String) right);
			Matcher matcher = pattern.matcher((String) left);
			return matcher.matches();
		} catch (PatternSyntaxException pse) {
			throw new SpelException(pse, SpelMessages.INVALID_PATTERN, right);
		}
	}

	@Override
	public String getOperatorName() {
		return "matches";
	}

}
