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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * Common superclass for nodes representing literals (boolean, string, number, etc).
 * 
 * @author Andy Clement
 * 
 */
public abstract class Literal extends SpelNode {

	public Literal(Token payload) {
		super(payload);
	}

	public abstract Object getLiteralValue();

	@Override
	public final Object getValue(ExpressionState state) throws SpelException {
		return getLiteralValue();
	}

	@Override
	public String toString() {
		return getLiteralValue().toString();
	}

	@Override
	public String toStringAST() {
		return toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
