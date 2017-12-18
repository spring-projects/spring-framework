/*
 * Copyright 2002-2017 the original author or authors.
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionInvocationTargetException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Expression language AST node that represents a method reference.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MethodReference extends SpelNodeImpl {

	private final String name;

	private final boolean nullSafe;

	@Nullable
	private volatile CachedMethodExecutor cachedExecutor;


	public MethodReference(boolean nullSafe, String methodName, int pos, SpelNodeImpl... arguments) {
		super(pos, arguments);
		this.name = methodName;
		this.nullSafe = nullSafe;
	}


	public final String getName() {
		return this.name;
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		Object[] arguments = getArguments(state);
		if (state.getActiveContextObject().getValue() == null) {
			throwIfNotNullSafe(getArgumentTypes(arguments));
			return ValueRef.NullValueRef.INSTANCE;
		}
		return new MethodValueRef(state, arguments);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		EvaluationContext evaluationContext = state.getEvaluationContext();
		Object value = state.getActiveContextObject().getValue();
		TypeDescriptor targetType = state.getActiveContextObject().getTypeDescriptor();
		Object[] arguments = getArguments(state);
		TypedValue result = getValueInternal(evaluationContext, value, targetType, arguments);
		updateExitTypeDescriptor();
		return result;
	}

	private TypedValue getValueInternal(EvaluationContext evaluationContext,
			@Nullable Object value, @Nullable TypeDescriptor targetType, Object[] arguments) {

		List<TypeDescriptor> argumentTypes = getArgumentTypes(arguments);
		if (value == null) {
			throwIfNotNullSafe(argumentTypes);
			return TypedValue.NULL;
		}

		MethodExecutor executorToUse = getCachedExecutor(evaluationContext, value, targetType, argumentTypes);
		if (executorToUse != null) {
			try {
				return executorToUse.execute(evaluationContext, value, arguments);
			}
			catch (AccessException ex) {
				// Two reasons this can occur:
				// 1. the method invoked actually threw a real exception
				// 2. the method invoked was not passed the arguments it expected and
				//    has become 'stale'

				// In the first case we should not retry, in the second case we should see
				// if there is a better suited method.

				// To determine the situation, the AccessException will contain a cause.
				// If the cause is an InvocationTargetException, a user exception was
				// thrown inside the method. Otherwise the method could not be invoked.
				throwSimpleExceptionIfPossible(value, ex);

				// At this point we know it wasn't a user problem so worth a retry if a
				// better candidate can be found.
				this.cachedExecutor = null;
			}
		}

		// either there was no accessor or it no longer existed
		executorToUse = findAccessorForMethod(this.name, argumentTypes, value, evaluationContext);
		this.cachedExecutor = new CachedMethodExecutor(
				executorToUse, (value instanceof Class ? (Class<?>) value : null), targetType, argumentTypes);
		try {
			return executorToUse.execute(evaluationContext, value, arguments);
		}
		catch (AccessException ex) {
			// Same unwrapping exception handling as above in above catch block
			throwSimpleExceptionIfPossible(value, ex);
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION, this.name,
					value.getClass().getName(), ex.getMessage());
		}
	}

	private void throwIfNotNullSafe(List<TypeDescriptor> argumentTypes) {
		if (!this.nullSafe) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
					FormatHelper.formatMethodForMessage(this.name, argumentTypes));
		}
	}

	private Object[] getArguments(ExpressionState state) {
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			// Make the root object the active context again for evaluating the parameter expressions
			try {
				state.pushActiveContextObject(state.getScopeRootContextObject());
				arguments[i] = this.children[i].getValueInternal(state).getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}
		return arguments;
	}

	private List<TypeDescriptor> getArgumentTypes(Object... arguments) {
		List<TypeDescriptor> descriptors = new ArrayList<>(arguments.length);
		for (Object argument : arguments) {
			descriptors.add(TypeDescriptor.forObject(argument));
		}
		return Collections.unmodifiableList(descriptors);
	}

	@Nullable
	private MethodExecutor getCachedExecutor(EvaluationContext evaluationContext, Object value,
			@Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

		List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
		if (methodResolvers.size() != 1 ||
				!(methodResolvers.get(0) instanceof ReflectiveMethodResolver)) {
			// Not a default ReflectiveMethodResolver - don't know whether caching is valid
			return null;
		}

		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.isSuitable(value, target, argumentTypes)) {
			return executorToCheck.get();
		}
		this.cachedExecutor = null;
		return null;
	}

	private MethodExecutor findAccessorForMethod(String name, List<TypeDescriptor> argumentTypes,
			Object targetObject, EvaluationContext evaluationContext) throws SpelEvaluationException {

		List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
		for (MethodResolver methodResolver : methodResolvers) {
			try {
				MethodExecutor methodExecutor = methodResolver.resolve(
						evaluationContext, targetObject, name, argumentTypes);
				if (methodExecutor != null) {
					return methodExecutor;
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex,
						SpelMessage.PROBLEM_LOCATING_METHOD, name, targetObject.getClass());
			}
		}

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_NOT_FOUND,
				FormatHelper.formatMethodForMessage(name, argumentTypes),
				FormatHelper.formatClassNameForMessage(
						targetObject instanceof Class ? ((Class<?>) targetObject) : targetObject.getClass()));
	}

	/**
	 * Decode the AccessException, throwing a lightweight evaluation exception or, if the
	 * cause was a RuntimeException, throw the RuntimeException directly.
	 */
	private void throwSimpleExceptionIfPossible(Object value, AccessException ex) {
		if (ex.getCause() instanceof InvocationTargetException) {
			Throwable rootCause = ex.getCause().getCause();
			if (rootCause instanceof RuntimeException) {
				throw (RuntimeException) rootCause;
			}
			throw new ExpressionInvocationTargetException(getStartPosition(),
					"A problem occurred when trying to execute method '" + this.name +
					"' on object of type [" + value.getClass().getName() + "]", rootCause);
		}
	}

	private void updateExitTypeDescriptor() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.get() instanceof ReflectiveMethodExecutor) {
			Method method = ((ReflectiveMethodExecutor) executorToCheck.get()).getMethod();
			this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder(this.name);
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
	 * A method reference is compilable if it has been resolved to a reflectively accessible method
	 * and the child nodes (arguments to the method) are also compilable.
	 */
	@Override
	public boolean isCompilable() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || executorToCheck.hasProxyTarget() ||
				!(executorToCheck.get() instanceof ReflectiveMethodExecutor)) {
			return false;
		}

		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}

		ReflectiveMethodExecutor executor = (ReflectiveMethodExecutor) executorToCheck.get();
		if (executor.didArgumentConversionOccur()) {
			return false;
		}
		Class<?> clazz = executor.getMethod().getDeclaringClass();
		if (!Modifier.isPublic(clazz.getModifiers()) && executor.getPublicDeclaringClass() == null) {
			return false;
		}

		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || !(executorToCheck.get() instanceof ReflectiveMethodExecutor)) {
			throw new IllegalStateException("No applicable cached executor found: " + executorToCheck);
		}

		ReflectiveMethodExecutor methodExecutor = (ReflectiveMethodExecutor) executorToCheck.get();
		Method method = methodExecutor.getMethod();
		boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
		String descriptor = cf.lastDescriptor();

		if (descriptor == null) {
			if (!isStaticMethod) {
				// Nothing on the stack but something is needed
				cf.loadTarget(mv);
			}
		}
		else {
			if (isStaticMethod) {
				// Something on the stack when nothing is needed
				mv.visitInsn(POP);
			}
		}
		
		if (CodeFlow.isPrimitive(descriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, descriptor.charAt(0));
		}

		String classDesc = null;
		if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
			classDesc = method.getDeclaringClass().getName().replace('.', '/');
		}
		else {
			Class<?> publicDeclaringClass = methodExecutor.getPublicDeclaringClass();
			Assert.state(publicDeclaringClass != null, "No public declaring class");
			classDesc = publicDeclaringClass.getName().replace('.', '/');
		};

		if (!isStaticMethod) {
			if (descriptor == null || !descriptor.substring(1).equals(classDesc)) {
				CodeFlow.insertCheckCast(mv, "L" + classDesc);
			}
		}

		generateCodeForArguments(mv, cf, method, this.children);
		mv.visitMethodInsn((isStaticMethod ? INVOKESTATIC : INVOKEVIRTUAL), classDesc, method.getName(),
				CodeFlow.createSignatureDescriptor(method), method.getDeclaringClass().isInterface());
		cf.pushDescriptor(this.exitTypeDescriptor);
	}


	private class MethodValueRef implements ValueRef {

		private final EvaluationContext evaluationContext;

		@Nullable
		private final Object value;

		@Nullable
		private final TypeDescriptor targetType;

		private final Object[] arguments;

		public MethodValueRef(ExpressionState state, Object[] arguments) {
			this.evaluationContext = state.getEvaluationContext();
			this.value = state.getActiveContextObject().getValue();
			this.targetType = state.getActiveContextObject().getTypeDescriptor();
			this.arguments = arguments;
		}

		@Override
		public TypedValue getValue() {
			TypedValue result = MethodReference.this.getValueInternal(
					this.evaluationContext, this.value, this.targetType, this.arguments);
			updateExitTypeDescriptor();
			return result;
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}


	private static class CachedMethodExecutor {

		private final MethodExecutor methodExecutor;

		@Nullable
		private final Class<?> staticClass;

		@Nullable
		private final TypeDescriptor target;

		private final List<TypeDescriptor> argumentTypes;

		public CachedMethodExecutor(MethodExecutor methodExecutor, @Nullable Class<?> staticClass,
				@Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

			this.methodExecutor = methodExecutor;
			this.staticClass = staticClass;
			this.target = target;
			this.argumentTypes = argumentTypes;
		}

		public boolean isSuitable(Object value, @Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {
			return ((this.staticClass == null || this.staticClass == value) &&
					ObjectUtils.nullSafeEquals(this.target, target) && this.argumentTypes.equals(argumentTypes));
		}

		public boolean hasProxyTarget() {
			return (this.target != null && Proxy.isProxyClass(this.target.getType()));
		}

		public MethodExecutor get() {
			return this.methodExecutor;
		}
	}

}
