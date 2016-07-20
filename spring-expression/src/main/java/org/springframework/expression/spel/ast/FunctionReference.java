/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.util.ReflectionUtils;

/**
 * A function reference is of the form "#someFunction(a,b,c)". Functions may be defined
 * in the context prior to the expression being evaluated or within the expression itself
 * using a lambda function definition. For example: Lambda function definition in an
 * expression: "(#max = {|x,y|$x>$y?$x:$y};max(2,3))" Calling context defined function:
 * "#isEven(37)". Functions may also be static java methods, registered in the context
 * prior to invocation of the expression.
 *
 * <p>Functions are very simplistic, the arguments are not part of the definition
 * (right now), so the names must be unique.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class FunctionReference extends SpelNodeImpl {

	private final String name;

	// Captures the most recently used method for the function invocation *if* the method
	// can safely be used for compilation (i.e. no argument conversion is going on)
	private Method method;
	
	private boolean argumentConversionOccurred;


	public FunctionReference(String functionName, int pos, SpelNodeImpl... arguments) {
		super(pos,arguments);
		this.name = functionName;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = state.lookupVariable(this.name);
		if (value == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
		}

		// Two possibilities: a lambda function or a Java static method registered as a function
		if (!(value.getValue() instanceof Method)) {
			throw new SpelEvaluationException(
					SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
		}

		try {
			return executeFunctionJLRMethod(state, (Method) value.getValue());
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

	/**
	 * Execute a function represented as a java.lang.reflect.Method.
	 * @param state the expression evaluation state
	 * @param method the method to invoke
	 * @return the return value of the invoked Java method
	 * @throws EvaluationException if there is any problem invoking the method
	 */
	private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
		this.method = null;
		Object[] functionArgs = getArguments(state);

		if (!method.isVarArgs() && method.getParameterCount() != functionArgs.length) {
			throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
					functionArgs.length, method.getParameterCount());
		}
		// Only static methods can be called in this way
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.FUNCTION_MUST_BE_STATIC,
					method.getDeclaringClass().getName() + "." + method.getName(), this.name);
		}

		argumentConversionOccurred = false;
		// Convert arguments if necessary and remap them for varargs if required
		if (functionArgs != null) {
			TypeConverter converter = state.getEvaluationContext().getTypeConverter();
			argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
		}
		if (method.isVarArgs()) {
			functionArgs =
					ReflectionHelper.setupArgumentsForVarargsInvocation(method.getParameterTypes(), functionArgs);
		}

		try {
			ReflectionUtils.makeAccessible(method);
			Object result = method.invoke(method.getClass(), functionArgs);
			if (!argumentConversionOccurred) {
				this.method = method;
				this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
			}
			return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, ex.getMessage());
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

	/**
	 * Compute the arguments to the function, they are the children of this expression node.
	 * @return an array of argument values for the function call
	 */
	private Object[] getArguments(ExpressionState state) throws EvaluationException {
		// Compute arguments to the function
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = this.children[i].getValueInternal(state).getValue();
		}
		return arguments;
	}
	
	@Override
	public boolean isCompilable() {
		if (this.method == null || this.argumentConversionOccurred) {
			return false;
		}
		int methodModifiers = this.method.getModifiers();
		if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
				!Modifier.isPublic(this.method.getDeclaringClass().getModifiers())) {
			return false;
		}
		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		return true;
	}
	
	@Override 
	public void generateCode(MethodVisitor mv,CodeFlow cf) {
		String classDesc = this.method.getDeclaringClass().getName().replace('.', '/');
		generateCodeForArguments(mv, cf, this.method, this.children);
		mv.visitMethodInsn(INVOKESTATIC, classDesc, this.method.getName(),
				CodeFlow.createSignatureDescriptor(this.method), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
