/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.ast;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A function reference is of the form "#someFunction(a,b,c)".
 *
 * <p>Functions can be either a {@link Method} (for static Java methods) or a
 * {@link MethodHandle} and must be registered in the context prior to evaluation
 * of the expression. See the {@code registerFunction()} methods in
 * {@link org.springframework.expression.spel.support.StandardEvaluationContext}
 * for details.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.expression.spel.support.StandardEvaluationContext#registerFunction(String, Method)
 * @see org.springframework.expression.spel.support.StandardEvaluationContext#registerFunction(String, MethodHandle)
 */
public class FunctionReference extends SpelNodeImpl {

	private final String name;

	// Captures the most recently used method for the function invocation *if* the method
	// can safely be used for compilation (i.e. no argument conversion is going on)
	@Nullable
	private volatile Method method;


	public FunctionReference(String functionName, int startPos, int endPos, SpelNodeImpl... arguments) {
		super(startPos, endPos, arguments);
		this.name = functionName;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = state.lookupVariable(this.name);
		if (value == TypedValue.NULL) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
		}
		Object function = value.getValue();

		// Static Java method registered via a Method.
		// Note: "javaMethod" cannot be named "method" due to a bug in Checkstyle.
		if (function instanceof Method javaMethod) {
			try {
				return executeFunctionViaMethod(state, javaMethod);
			}
			catch (SpelEvaluationException ex) {
				ex.setPosition(getStartPosition());
				throw ex;
			}
		}

		// Function registered via a MethodHandle.
		if (function instanceof MethodHandle methodHandle) {
			try {
				return executeFunctionViaMethodHandle(state, methodHandle);
			}
			catch (SpelEvaluationException ex) {
				ex.setPosition(getStartPosition());
				throw ex;
			}
		}

		// Neither a Method nor a MethodHandle?
		throw new SpelEvaluationException(
				SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
	}

	/**
	 * Execute a function represented as a {@link Method}.
	 * @param state the expression evaluation state
	 * @param method the method to invoke
	 * @return the return value of the invoked Java method
	 * @throws EvaluationException if there is any problem invoking the method
	 */
	private TypedValue executeFunctionViaMethod(ExpressionState state, Method method) throws EvaluationException {
		Object[] functionArgs = getArguments(state);

		if (!method.isVarArgs()) {
			int declaredParamCount = method.getParameterCount();
			if (declaredParamCount != functionArgs.length) {
				throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
						this.name, functionArgs.length, declaredParamCount);
			}
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
		}

		// Convert arguments if necessary and remap them for varargs if required
		TypeConverter converter = state.getEvaluationContext().getTypeConverter();
		boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
		if (method.isVarArgs()) {
			functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
					method.getParameterTypes(), functionArgs);
		}
		boolean compilable = false;

		try {
			ReflectionUtils.makeAccessible(method);
			Object result = method.invoke(method.getClass(), functionArgs);
			compilable = !argumentConversionOccurred;
			return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
		}
		catch (Exception ex) {
			Throwable cause = ((ex instanceof InvocationTargetException ite && ite.getCause() != null) ?
					ite.getCause() : ex);
			throw new SpelEvaluationException(getStartPosition(), cause, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, cause.getMessage());
		}
		finally {
			if (compilable) {
				this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
				this.method = method;
			}
			else {
				this.exitTypeDescriptor = null;
				this.method = null;
			}
		}
	}

	/**
	 * Execute a function represented as {@link MethodHandle}.
	 * <p>Method types that take no arguments (fully bound handles or static methods
	 * with no parameters) can use {@link MethodHandle#invoke(Object...)} which is the most
	 * efficient. Otherwise, {@link MethodHandle#invokeWithArguments(Object...)} is used.
	 * @param state the expression evaluation state
	 * @param methodHandle the method handle to invoke
	 * @return the return value of the invoked Java method
	 * @throws EvaluationException if there is any problem invoking the method
	 * @since 6.1
	 */
	private TypedValue executeFunctionViaMethodHandle(ExpressionState state, MethodHandle methodHandle) throws EvaluationException {
		Object[] functionArgs = getArguments(state);
		MethodType declaredParams = methodHandle.type();
		int spelParamCount = functionArgs.length;
		int declaredParamCount = declaredParams.parameterCount();

		// We don't use methodHandle.isVarargsCollector(), because a MethodHandle created via
		// MethodHandle#bindTo() is "never a variable-arity method handle, even if the original
		// target method handle was." Thus, we merely assume/suspect that varargs are supported
		// if the last parameter type is an array.
		boolean isSuspectedVarargs = declaredParams.lastParameterType().isArray();

		if (isSuspectedVarargs) {
			if (spelParamCount < declaredParamCount - 1) {
				// Varargs, but the number of provided arguments (potentially 0) is insufficient
				// for a varargs invocation for the number of declared parameters.
				//
				// As stated in the Javadoc for MethodHandle#asVarargsCollector(), "the caller
				// must supply, at a minimum, N-1 arguments, where N is the arity of the target."
				throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
						this.name, spelParamCount, (declaredParamCount - 1) + " or more");
			}
		}
		else if (spelParamCount != declaredParamCount) {
			// Incorrect number and not varargs. Perhaps a subset of arguments was provided,
			// but the MethodHandle wasn't bound?
			throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
					this.name, spelParamCount, declaredParamCount);
		}

		// simplest case: the MethodHandle is fully bound or represents a static method with no params:
		if (declaredParamCount == 0) {
			try {
				return new TypedValue(methodHandle.invoke());
			}
			catch (Throwable ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
						this.name, ex.getMessage());
			}
			finally {
				// Note: we consider MethodHandles not compilable
				this.exitTypeDescriptor = null;
				this.method = null;
			}
		}

		// more complex case, we need to look at conversion and varargs repackaging
		Integer varArgPosition = null;
		if (isSuspectedVarargs) {
			varArgPosition = declaredParamCount - 1;
		}
		TypeConverter converter = state.getEvaluationContext().getTypeConverter();
		ReflectionHelper.convertAllMethodHandleArguments(converter, functionArgs, methodHandle, varArgPosition);

		if (isSuspectedVarargs) {
			if (declaredParamCount == 1) {
				// We only repackage the varargs if it is the ONLY argument -- for example,
				// when we are dealing with a bound MethodHandle.
				functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
						methodHandle.type().parameterArray(), functionArgs);
			}
			else if (spelParamCount == declaredParamCount) {
				// If the varargs were supplied already packaged in an array, we have to create
				// a new array, add the non-varargs arguments to the beginning of that array,
				// and add the unpackaged varargs arguments to the end of that array. The reason
				// is that MethodHandle.invokeWithArguments(Object...) does not expect varargs
				// to be packaged in an array, in contrast to how method invocation works with
				// reflection.
				int actualVarargsIndex = functionArgs.length - 1;
				if (actualVarargsIndex >= 0 && functionArgs[actualVarargsIndex] instanceof Object[] argsToUnpack) {
					Object[] newArgs = new Object[actualVarargsIndex + argsToUnpack.length];
					System.arraycopy(functionArgs, 0, newArgs, 0, actualVarargsIndex);
					System.arraycopy(argsToUnpack, 0, newArgs, actualVarargsIndex, argsToUnpack.length);
					functionArgs = newArgs;
				}
			}
		}

		try {
			return new TypedValue(methodHandle.invokeWithArguments(functionArgs));
		}
		catch (Throwable ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, ex.getMessage());
		}
		finally {
			// Note: we consider MethodHandles not compilable
			this.exitTypeDescriptor = null;
			this.method = null;
		}
	}

	@Override
	public String toStringAST() {
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (int i = 0; i < getChildCount(); i++) {
			sj.add(getChild(i).toStringAST());
		}
		return '#' + this.name + sj;
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
		Method method = this.method;
		if (method == null) {
			return false;
		}
		int methodModifiers = method.getModifiers();
		if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
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
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		Method method = this.method;
		Assert.state(method != null, "No method handle");
		String classDesc = method.getDeclaringClass().getName().replace('.', '/');
		generateCodeForArguments(mv, cf, method, this.children);
		mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
				CodeFlow.createSignatureDescriptor(method), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
