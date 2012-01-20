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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents the elvis operator ?:.  For an expression "a?:b" if a is not null, the value of the expression
 * is "a", if a is null then the value of the expression is "b".
 *
 * @author Andy Clement
 * @since 3.0
 */
public class Elvis extends SpelNodeImpl {

	public Elvis(int pos, SpelNodeImpl... args) {
		super(pos,args);
	}

	/**
	 * Evaluate the condition and if not null, return it.  If it is null return the other value.
	 * @param state the expression state
	 * @throws EvaluationException if the condition does not evaluate correctly to a boolean or there is a problem
	 * executing the chosen alternative
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = children[0].getValueInternal(state);
		if (value.getValue()!=null && !((value.getValue() instanceof String) && ((String)value.getValue()).length()==0)) {
			return value;
		} else {
			return children[1].getValueInternal(state);
		}
	}

	@Override
	public String toStringAST() {
		return new StringBuilder().append(getChild(0).toStringAST()).append(" ?: ").append(getChild(1).toStringAST()).toString();
	}

}
