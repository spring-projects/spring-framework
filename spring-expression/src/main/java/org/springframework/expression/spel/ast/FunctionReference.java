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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.util.ReflectionUtils;

/**
 * A function reference is of the form "#someFunction(a,b,c)". Functions must be
 * registered in the evaluation context prior to the invocation of the expression.
 * A function may either be a static java {@link Method} or an instance of a
 * {@link MethodExecutor} which will be executed with a target parameter <code>null</code>.
 *
 * <p>Functions are very simplistic, the arguments are not part of the definition (right now),
 * so the names must be unique.
 *
 * @author Andy Clement
 * @author Oliver Becker
 * @since 3.0
 */
public class FunctionReference extends SpelNodeImpl {

	private final String name;

	public FunctionReference(String functionName, int pos, SpelNodeImpl... arguments) {
		super(pos, arguments);
		name = functionName;
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue o = state.lookupVariable(this.name);
		if (o == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
		}

		Object value = o.getValue();
		try {
			// Two possibilities: a Java static method or a MethodExecutor instance
			if (value instanceof Method) {
				return executeFunctionJLRMethod(state, (Method) value);
			} else if (value instanceof MethodExecutor) {
				return executeFunctionMethodExecutor(state, (MethodExecutor) value);
			}
		}
		catch (SpelEvaluationException se) {
			se.setPosition(getStartPosition());
			throw se;
		}

		throw new SpelEvaluationException(getStartPosition(),
				SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED,
				this.name, value == null ? null : value.getClass());
	}

	/**
	 * Execute a function represented as a java.lang.reflect.Method.
	 *
	 * @param state the expression evaluation state
	 * @param method the java method to invoke
	 * @return the return value of the invoked Java method
	 * @throws EvaluationException if there is any problem invoking the method
	 */
	private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
		Object[] functionArgs = getArguments(state);

		if (!method.isVarArgs() && method.getParameterTypes().length != functionArgs.length) {
			throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
					functionArgs.length, method.getParameterTypes().length);
		}
		// Only static methods can be called in this way
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_MUST_BE_STATIC,
					method.getDeclaringClass().getName() + "." + method.getName(), this.name);
		}

		// Convert arguments if necessary and remap them for varargs if required
		if (functionArgs != null) {
			TypeConverter converter = state.getEvaluationContext().getTypeConverter();
			ReflectionHelper.convertAllArguments(converter, functionArgs, method);
		}
		if (method.isVarArgs()) {
			functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(method.getParameterTypes(), functionArgs);
		}

		try {
			ReflectionUtils.makeAccessible(method);
			Object result = method.invoke(method.getClass(), functionArgs);
			return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, ex.getMessage());
		}
	}

	/**
	 * Execute a function represented as a {@link MethodExecutor}.
	 *
	 * @param state the expression evaluation state
	 * @param executor the MethodExecutor to be invoked
	 * @return the return value of the invoked executor
	 */
	private TypedValue executeFunctionMethodExecutor(ExpressionState state, MethodExecutor executor) {
		try {
			// there is no target instance, so pass null
			return executor.execute(state.getEvaluationContext(), null, getArguments(state));
		}
		catch (AccessException ex) {
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.EXCEPTION_DURING_FUNCTION_CALL, this.name, ex.getMessage());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("#").append(this.name);
		sb.append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	// to 'assign' to a function don't use the () suffix and so it is just a variable reference

	/**
	 * Compute the arguments to the function, they are the children of this expression node.
	 *
	 * @return an array of argument values for the function call
	 */
	private Object[] getArguments(ExpressionState state) throws EvaluationException {
		// Compute arguments to the function
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = children[i].getValueInternal(state).getValue();
		}
		return arguments;
	}

}
