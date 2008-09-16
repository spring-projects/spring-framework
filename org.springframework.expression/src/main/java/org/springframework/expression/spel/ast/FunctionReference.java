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
import java.lang.reflect.Modifier;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.reflection.ReflectionUtils;

/**
 * A function reference is of the form "#someFunction(a,b,c)". Functions may be defined in the context prior to the
 * expression being evaluated or within the expression itself using a lambda function definition. For example: Lambda
 * function definition in an expression: "(#max = {|x,y|$x>$y?$x:$y};max(2,3))" Calling context defined function:
 * "#isEven(37)". Functions may also be static java methods, registered in the context prior to invocation of the
 * expression.
 * 
 * Functions are very simplistic, the arguments are not part of the definition (right now), so the names must be unique.
 * 
 * @author Andy Clement
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

		// Two possibilities: a lambda function or a Java static method registered as a function
		if (!(o instanceof Method)) {
			throw new SpelException(SpelMessages.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, name, o.getClass());
		}

		return executeFunctionJLRMethod(state, (Method) o);
	}

	/**
	 * Execute a function represented as a java.lang.reflect.Method.
	 * 
	 * @param state the expression evaluation state
	 * @param the java method to invoke
	 * @return the return value of the invoked Java method
	 * @throws EvaluationException if there is any problem invoking the method
	 */
	private Object executeFunctionJLRMethod(ExpressionState state, Method m) throws EvaluationException {
		Object[] functionArgs = getArguments(state);

		if (!m.isVarArgs() && m.getParameterTypes().length != functionArgs.length) {
			throw new SpelException(SpelMessages.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, functionArgs.length, m
					.getParameterTypes().length);
		}
		// Only static methods can be called in this way
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.FUNCTION_MUST_BE_STATIC, m
					.getDeclaringClass().getName()
					+ "." + m.getName(), name);
		}

		// Convert arguments if necessary and remap them for varargs if required
		if (functionArgs != null) {
			EvaluationContext ctx = state.getEvaluationContext();
			TypeConverter converter = null;
			if (ctx.getTypeUtils() != null) {
				converter = ctx.getTypeUtils().getTypeConverter();
			}
			ReflectionUtils.convertArguments(m.getParameterTypes(), m.isVarArgs(), converter, functionArgs);
		}
		if (m.isVarArgs()) {
			functionArgs = ReflectionUtils.setupArgumentsForVarargsInvocation(m.getParameterTypes(), functionArgs);
		}

		try {
			return m.invoke(m.getClass(), functionArgs);
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
