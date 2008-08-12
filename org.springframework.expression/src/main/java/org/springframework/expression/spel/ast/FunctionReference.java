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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * A function reference is of the form "#someFunction(a,b,c)". Functions may be defined in the context prior to the
 * expression being evaluated or within the expression itself using a lambda function definition. For example: Lambda
 * function definition in an expression: "(#max = {|x,y|$x>$y?$x:$y};max(2,3))" Calling context defined function:
 * "#isEven(37)"
 * 
 * Functions are very simplistic, the arguments are not part of the definition (right now), so the names must be unique.
 * 
 * @author Andy Clement
 * 
 */
public class FunctionReference extends SpelNode {

	private final String name;

	public FunctionReference(Token payload) {
		super(payload);
		name = payload.getText();
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object o = state.lookupVariable(name);
		if (o == null) {
			throw new SpelException(SpelMessages.FUNCTION_NOT_DEFINED, name);
		}
		if (!(o instanceof Lambda || o instanceof Method)) {
			throw new SpelException(SpelMessages.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, name, o.getClass());
		}
		
		// FUNCTION REF NEEDS TO DO ARG CONVERSION??
		if (o instanceof Lambda) {
			return executeLambdaFunction(state, (Lambda) o);
		} else { // o instanceof Method
			return executeFunctionJLRMethod(state, (Method) o);
		}
	}

	/* Execute a function represented as a java.lang.reflect.Method */
	private Object executeFunctionJLRMethod(ExpressionState state, Method m) throws EvaluationException {
		Object[] functionArgs = getArguments(state);
		if (m.getParameterTypes().length != functionArgs.length) {
			throw new SpelException(SpelMessages.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, functionArgs.length, m
					.getParameterTypes().length);
		}
		
		// Check if arguments need converting
		Class<?>[] expectedParams = m.getParameterTypes();
		Object[] argsToPass = new Object[functionArgs.length];
		TypeConverter converter = state.getEvaluationContext().getTypeUtils().getTypeConverter();
		for (int arg=0; arg<argsToPass.length; arg++) {
			if (functionArgs[arg]==null || functionArgs[arg].getClass()==expectedParams[arg]) {
				argsToPass[arg]=functionArgs[arg];
			} else {
				argsToPass[arg]=converter.convertValue(functionArgs[arg], expectedParams[arg]);
			}
		}
		
		try {
			return m.invoke(m.getClass(), argsToPass);
		} catch (IllegalArgumentException e) {
			throw new SpelException(getCharPositionInLine(), e, SpelMessages.EXCEPTION_DURING_FUNCTION_CALL, name, e
					.getMessage());
		} catch (IllegalAccessException e) {
			throw new SpelException(getCharPositionInLine(), e, SpelMessages.EXCEPTION_DURING_FUNCTION_CALL, name, e
					.getMessage());
		} catch (InvocationTargetException e) {
			throw new SpelException(getCharPositionInLine(), e, SpelMessages.EXCEPTION_DURING_FUNCTION_CALL, name, e
					.getMessage());
		}
	}

	/*
	 * Execute a function that was defined as a lambda function.
	 */
	private Object executeLambdaFunction(ExpressionState state, Lambda lambdaExpression) throws EvaluationException {
		Object[] functionArgs = getArguments(state);
		List<String> args = lambdaExpression.getArguments();
		if (args.size() != functionArgs.length) {
			throw new SpelException(SpelMessages.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, functionArgs.length, args
					.size());
		}
		Map<String, Object> argMap = new HashMap<String, Object>();
		for (int i = 0; i < args.size(); i++) {
			argMap.put(args.get(i), functionArgs[i]);
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
		StringBuilder sb = new StringBuilder("#").append(name);
		sb.append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	// to 'assign' to a function don't use the () suffix and so it is just a variable reference
	@Override
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	/**
	 * Compute the arguments to the function, they are the children of this expression node.
	 * @return an array of argument values for the function call
	 */
	private Object[] getArguments(ExpressionState state) throws EvaluationException {
		// Compute arguments to the function
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = getChild(i).getValue(state);
		}
		return arguments;
	}

}
