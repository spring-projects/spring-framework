/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.Label;
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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Expression language AST node that represents a method reference (i.e., a
 * method invocation other than a simple property reference).
 *
 * <h3>Null-safe Invocation</h3>
 *
 * <p>Null-safe invocation is supported via the {@code '?.'} operator. For example,
 * {@code 'counter?.incrementBy(1)'} will evaluate to {@code null} if {@code counter}
 * is {@code null} and will otherwise evaluate to the value returned from the
 * invocation of {@code counter.incrementBy(1)}. As of Spring Framework 7.0,
 * null-safe invocation also applies when invoking a method on an {@link Optional}
 * target. For example, if {@code counter} is of type {@code Optional<Counter>},
 * the expression {@code 'counter?.incrementBy(1)'} will evaluate to {@code null}
 * if {@code counter} is {@code null} or {@link Optional#isEmpty() empty} and will
 * otherwise evaluate the value returned from the invocation of
 * {@code counter.get().incrementBy(1)}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class MethodReference extends SpelNodeImpl {

	private final boolean nullSafe;

	private final String name;

	private @Nullable Character originalPrimitiveExitTypeDescriptor;

	private volatile @Nullable CachedMethodExecutor cachedExecutor;


	public MethodReference(boolean nullSafe, String methodName, int startPos, int endPos, SpelNodeImpl... arguments) {
		super(startPos, endPos, arguments);
		this.name = methodName;
		this.nullSafe = nullSafe;
	}


	/**
	 * Does this node represent a null-safe method reference?
	 * @since 6.0.13
	 */
	@Override
	public final boolean isNullSafe() {
		return this.nullSafe;
	}

	/**
	 * Get the name of the referenced method.
	 */
	public final String getName() {
		return this.name;
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		@Nullable Object[] arguments = getArguments(state);
		if (state.getActiveContextObject().getValue() == null) {
			if (!isNullSafe()) {
				throw nullTargetException(getArgumentTypes(arguments));
			}
			return ValueRef.NullValueRef.INSTANCE;
		}
		return new MethodValueRef(state, arguments);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		EvaluationContext evaluationContext = state.getEvaluationContext();
		TypedValue contextObject = state.getActiveContextObject();
		Object target = contextObject.getValue();
		TypeDescriptor targetType = contextObject.getTypeDescriptor();
		@Nullable Object[] arguments = getArguments(state);
		TypedValue result = getValueInternal(evaluationContext, target, targetType, arguments);
		updateExitTypeDescriptor();
		return result;
	}

	private TypedValue getValueInternal(EvaluationContext evaluationContext, @Nullable Object target,
			@Nullable TypeDescriptor targetType, @Nullable Object[] arguments) {

		List<TypeDescriptor> argumentTypes = getArgumentTypes(arguments);
		Optional<?> fallbackOptionalTarget = null;
		boolean isEmptyOptional = false;

		if (isNullSafe()) {
			if (target == null) {
				return TypedValue.NULL;
			}
			if (target instanceof Optional<?> optional) {
				if (optional.isPresent()) {
					target = optional.get();
					fallbackOptionalTarget = optional;
				}
				else {
					isEmptyOptional = true;
				}
			}
		}

		if (target == null) {
			throw nullTargetException(argumentTypes);
		}

		MethodExecutor executorToUse = getCachedExecutor(evaluationContext, target, targetType, argumentTypes);
		if (executorToUse != null) {
			try {
				return executorToUse.execute(evaluationContext, target, arguments);
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
				throwSimpleExceptionIfPossible(target, ex);

				// At this point we know it wasn't a user problem so worth a retry if a
				// better candidate can be found.
				this.cachedExecutor = null;
				executorToUse = null;
			}
		}

		// Either there was no cached executor, or it no longer exists.

		// First, attempt to find the method on the target object.
		Object targetToUse = target;
		MethodExecutorSearchResult searchResult = findMethodExecutor(argumentTypes, target, evaluationContext);
		if (searchResult.methodExecutor != null) {
			executorToUse = searchResult.methodExecutor;
		}
		// Second, attempt to find the method on the original Optional instance.
		else if (fallbackOptionalTarget != null) {
			searchResult = findMethodExecutor(argumentTypes, fallbackOptionalTarget, evaluationContext);
			if (searchResult.methodExecutor != null) {
				executorToUse = searchResult.methodExecutor;
				targetToUse = fallbackOptionalTarget;
			}
		}
		// If we got this far, that means we failed to find an executor for both the
		// target and the fallback target. So, we return NULL if the original target
		// is a null-safe empty Optional.
		else if (isEmptyOptional) {
			return TypedValue.NULL;
		}

		if (executorToUse == null) {
			String method = FormatHelper.formatMethodForMessage(this.name, argumentTypes);
			String className = FormatHelper.formatClassNameForMessage(
					target instanceof Class<?> clazz ? clazz : target.getClass());
			if (searchResult.accessException != null) {
				throw new SpelEvaluationException(
						getStartPosition(), searchResult.accessException, SpelMessage.PROBLEM_LOCATING_METHOD, method, className);
			}
			else {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_NOT_FOUND, method, className);
			}
		}

		this.cachedExecutor = new CachedMethodExecutor(
				executorToUse, (targetToUse instanceof Class<?> clazz ? clazz : null), targetType, argumentTypes);
		try {
			return executorToUse.execute(evaluationContext, targetToUse, arguments);
		}
		catch (AccessException ex) {
			// Same unwrapping exception handling as in above catch block
			throwSimpleExceptionIfPossible(targetToUse, ex);
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION, this.name,
					targetToUse.getClass().getName(), ex.getMessage());
		}
	}

	private SpelEvaluationException nullTargetException(List<TypeDescriptor> argumentTypes) {
		return new SpelEvaluationException(getStartPosition(),
				SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
				FormatHelper.formatMethodForMessage(this.name, argumentTypes));
	}

	private @Nullable Object[] getArguments(ExpressionState state) {
		@Nullable Object[] arguments = new Object[getChildCount()];
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

	private List<TypeDescriptor> getArgumentTypes(@Nullable Object... arguments) {
		List<@Nullable TypeDescriptor> descriptors = new ArrayList<>(arguments.length);
		for (Object argument : arguments) {
			descriptors.add(TypeDescriptor.forObject(argument));
		}
		return Collections.unmodifiableList(descriptors);
	}

	private @Nullable MethodExecutor getCachedExecutor(EvaluationContext evaluationContext, Object target,
			@Nullable TypeDescriptor targetType, List<TypeDescriptor> argumentTypes) {

		List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
		if (methodResolvers.size() != 1 || !(methodResolvers.get(0) instanceof ReflectiveMethodResolver)) {
			// Not a default ReflectiveMethodResolver - don't know whether caching is valid
			return null;
		}

		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.isSuitable(target, targetType, argumentTypes)) {
			return executorToCheck.get();
		}
		this.cachedExecutor = null;
		return null;
	}

	private MethodExecutorSearchResult findMethodExecutor(List<TypeDescriptor> argumentTypes, Object target,
			EvaluationContext evaluationContext) throws SpelEvaluationException {

		AccessException accessException = null;
		for (MethodResolver methodResolver : evaluationContext.getMethodResolvers()) {
			try {
				MethodExecutor methodExecutor = methodResolver.resolve(
						evaluationContext, target, this.name, argumentTypes);
				if (methodExecutor != null) {
					return new MethodExecutorSearchResult(methodExecutor, null);
				}
			}
			catch (AccessException ex) {
				accessException = ex;
				break;
			}
		}

		return new MethodExecutorSearchResult(null, accessException);
	}

	/**
	 * Decode the AccessException, throwing a lightweight evaluation exception or,
	 * if the cause was a RuntimeException, throw the RuntimeException directly.
	 */
	private void throwSimpleExceptionIfPossible(Object target, AccessException ex) {
		if (ex.getCause() instanceof InvocationTargetException cause) {
			Throwable rootCause = cause.getCause();
			if (rootCause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new ExpressionInvocationTargetException(getStartPosition(),
					"A problem occurred when trying to execute method '" + this.name +
					"' on object of type [" + target.getClass().getName() + "]", rootCause);
		}
	}

	private void updateExitTypeDescriptor() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.get() instanceof ReflectiveMethodExecutor reflectiveMethodExecutor) {
			Method method = reflectiveMethodExecutor.getMethod();
			String descriptor = CodeFlow.toDescriptor(method.getReturnType());
			if (isNullSafe() && CodeFlow.isPrimitive(descriptor) && (descriptor.charAt(0) != 'V')) {
				this.originalPrimitiveExitTypeDescriptor = descriptor.charAt(0);
				this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
			}
			else {
				this.exitTypeDescriptor = descriptor;
			}
		}
	}

	@Override
	public String toStringAST() {
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (int i = 0; i < getChildCount(); i++) {
			sj.add(getChild(i).toStringAST());
		}
		return this.name + sj;
	}

	/**
	 * A method reference is compilable if it has been resolved to a reflectively accessible method
	 * and the child nodes (arguments to the method) are also compilable.
	 */
	@Override
	public boolean isCompilable() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || executorToCheck.hasProxyTarget() ||
				!(executorToCheck.get() instanceof ReflectiveMethodExecutor executor)) {
			return false;
		}

		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		if (executor.didArgumentConversionOccur()) {
			return false;
		}

		Method method = executor.getMethod();
		return (Modifier.isPublic(method.getModifiers()) && executor.getPublicDeclaringClass() != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || !(executorToCheck.get() instanceof ReflectiveMethodExecutor methodExecutor)) {
			throw new IllegalStateException("No applicable cached executor found: " + executorToCheck);
		}
		Method method = methodExecutor.getMethod();

		Class<?> publicDeclaringClass = methodExecutor.getPublicDeclaringClass();
		Assert.state(publicDeclaringClass != null,
				() -> "Failed to find public declaring class for method: " + method);

		String classDesc = publicDeclaringClass.getName().replace('.', '/');
		boolean isStatic = Modifier.isStatic(method.getModifiers());
		String descriptor = cf.lastDescriptor();

		if (descriptor == null && !isStatic) {
			// Nothing on the stack but something is needed
			cf.loadTarget(mv);
		}

		Label skipIfNull = null;
		if (isNullSafe() && (descriptor != null || !isStatic)) {
			skipIfNull = new Label();
			Label continueLabel = new Label();
			mv.visitInsn(DUP);
			mv.visitJumpInsn(IFNONNULL, continueLabel);
			CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
			mv.visitJumpInsn(GOTO, skipIfNull);
			mv.visitLabel(continueLabel);
		}

		if (descriptor != null && isStatic) {
			// A static method call will not consume what is on the stack, so
			// it needs to be popped off.
			mv.visitInsn(POP);
		}

		if (CodeFlow.isPrimitive(descriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, descriptor.charAt(0));
		}

		if (!isStatic && (descriptor == null || !descriptor.substring(1).equals(classDesc))) {
			CodeFlow.insertCheckCast(mv, "L" + classDesc);
		}

		generateCodeForArguments(mv, cf, method, this.children);
		boolean isInterface = publicDeclaringClass.isInterface();
		int opcode = (isStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL);
		mv.visitMethodInsn(opcode, classDesc, method.getName(), CodeFlow.createSignatureDescriptor(method),
				isInterface);
		cf.pushDescriptor(this.exitTypeDescriptor);

		if (this.originalPrimitiveExitTypeDescriptor != null) {
			// The output of the accessor will be a primitive but from the block above it might be null,
			// so to have a 'common stack' element at the skipIfNull target we need to box the primitive.
			CodeFlow.insertBoxIfNecessary(mv, this.originalPrimitiveExitTypeDescriptor);
		}

		if (skipIfNull != null) {
			if ("V".equals(this.exitTypeDescriptor)) {
				// If the method return type is 'void', we need to push a null object
				// reference onto the stack to satisfy the needs of the skipIfNull target.
				mv.visitInsn(ACONST_NULL);
			}
			mv.visitLabel(skipIfNull);
		}
	}


	private class MethodValueRef implements ValueRef {

		private final EvaluationContext evaluationContext;

		private final @Nullable Object target;

		private final @Nullable TypeDescriptor targetType;

		private final @Nullable Object[] arguments;

		public MethodValueRef(ExpressionState state, @Nullable Object[] arguments) {
			this.evaluationContext = state.getEvaluationContext();
			this.target = state.getActiveContextObject().getValue();
			this.targetType = state.getActiveContextObject().getTypeDescriptor();
			this.arguments = arguments;
		}

		@Override
		public TypedValue getValue() {
			TypedValue result = MethodReference.this.getValueInternal(
					this.evaluationContext, this.target, this.targetType, this.arguments);
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


	private record MethodExecutorSearchResult(@Nullable MethodExecutor methodExecutor, @Nullable AccessException accessException) {
	}

	private record CachedMethodExecutor(MethodExecutor methodExecutor, @Nullable Class<?> staticClass,
			@Nullable TypeDescriptor targetType, List<TypeDescriptor> argumentTypes) {

		public boolean isSuitable(Object target, @Nullable TypeDescriptor targetType, List<TypeDescriptor> argumentTypes) {
			return ((this.staticClass == null || this.staticClass == target) &&
					ObjectUtils.nullSafeEquals(this.targetType, targetType) && this.argumentTypes.equals(argumentTypes));
		}

		public boolean hasProxyTarget() {
			return (this.targetType != null && Proxy.isProxyClass(this.targetType.getType()));
		}

		public MethodExecutor get() {
			return this.methodExecutor;
		}
	}

}
