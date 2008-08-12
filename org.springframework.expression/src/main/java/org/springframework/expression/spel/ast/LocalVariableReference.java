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
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * A variable reference such as $someVar. Local variables are only visible at the current scoping level or below within
 * an expression. Calling a function introduces a new nested scope.
 * 
 * @author Andy Clement
 * 
 */
public class LocalVariableReference extends SpelNode {

	private final String name;

	public LocalVariableReference(Token payload) {
		super(payload);
		name = payload.getText();
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		Object result = state.lookupLocalVariable(name);
		if (result == null) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.LOCAL_VARIABLE_NOT_DEFINED, name);
		}
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws SpelException {
		// Object oldValue = state.lookupVariable(name);
		state.setLocalVariable(name, value);
	}

	@Override
	public String toStringAST() {
		return new StringBuilder("$").append(name).toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return true;
	}

}
