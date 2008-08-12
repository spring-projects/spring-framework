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

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents an entry in a map initializer structure like "#{'a':3,'b':2}" Both "'a':3" and "'b':2" would be MapEntry
 * instances.
 * 
 * @author Andy Clement
 * 
 */
public class MapEntry extends SpelNode {

	public MapEntry(Token payload) {
		super(payload);
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		String k = getChild(0).toStringAST();
		String v = getChild(1).toStringAST();
		sb.append(k).append(":").append(v);
		return sb.toString();
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		throw new SpelException(SpelMessages.MAPENTRY_SHOULD_NOT_BE_EVALUATED);
	}

	/**
	 * Return the value of the key for this map entry.
	 */
	public Object getKeyValue(ExpressionState state) throws EvaluationException {
		return getChild(0).getValue(state);
	}

	/**
	 * Return the value of the value for this map entry.
	 */
	public Object getValueValue(ExpressionState state) throws EvaluationException {
		return getChild(1).getValue(state);
	}

}
