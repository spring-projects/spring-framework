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

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents a NOT operation.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorNot extends SpelNodeImpl { // Not is a unary operator so do not extend BinaryOperator

	public OperatorNot(Token payload) {
		super(payload);
	}

	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		try {
			boolean value = (Boolean)state.convertValue(getChild(0).getValueInternal(state), BOOLEAN_TYPE_DESCRIPTOR);
			return BooleanTypedValue.forValue(!value);
		}
		catch (SpelException see) {
			see.setPosition(getChild(0).getCharPositionInLine());
			throw see;
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("!").append(getChild(0).toStringAST());
		return sb.toString();
	}

}
