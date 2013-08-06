/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * Represents a ternary expression, for example: "someCheck()?true:false".
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class Ternary extends SpelNodeImpl {


	public Ternary(int pos, SpelNodeImpl... args) {
		super(pos,args);
	}

	/**
	 * Evaluate the condition and if true evaluate the first alternative, otherwise
	 * evaluate the second alternative.
	 * @param state the expression state
	 * @throws EvaluationException if the condition does not evaluate correctly to a
	 *         boolean or there is a problem executing the chosen alternative
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Boolean value = this.children[0].getValue(state, Boolean.class);
		if (value == null) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
		if (value.booleanValue()) {
			return this.children[1].getValueInternal(state);
		}
		else {
			return this.children[2].getValueInternal(state);
		}
	}

	@Override
	public String toStringAST() {
		return new StringBuilder().append(getChild(0).toStringAST()).append(" ? ").append(getChild(1).toStringAST())
				.append(" : ").append(getChild(2).toStringAST()).toString();
	}

}
