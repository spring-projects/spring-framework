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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Local functions are references like $fn() where fn is the 'local' to lookup in the local scope Example: "(#sqrt={|n|
 * T(Math).sqrt($n)};#delegate={|f,n| $f($n)};#delegate(#sqrt,4))"
 * 
 * @author Andy Clement
 */
public class LocalFunctionReference extends SpelNode {

	private final String name;

	public LocalFunctionReference(Token payload) {
		super(payload);
		name = payload.getText();
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object o = state.lookupLocalVariable(name);
		if (o == null) {
			throw new SpelException(SpelMessages.FUNCTION_NOT_DEFINED, name);
		}
		if (!(o instanceof Lambda)) {
			throw new SpelException(SpelMessages.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, name, o.getClass().getName());
		}

		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = getChild(i).getValue(state);
		}
		Lambda lambdaExpression = (Lambda) o;
		List<String> args = lambdaExpression.getArguments();
		Map<String, Object> argMap = new HashMap<String, Object>();
		if (args.size() != arguments.length) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
					arguments.length, args.size());
		}
		for (int i = 0; i < args.size(); i++) {
			argMap.put(args.get(i), arguments[i]);
		}
		try {
			state.enterScope(argMap);
			return ((SpelNode) lambdaExpression.getExpression()).getValue(state);
		} finally {
			state.exitScope();
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("$").append(name);
		sb.append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

}
