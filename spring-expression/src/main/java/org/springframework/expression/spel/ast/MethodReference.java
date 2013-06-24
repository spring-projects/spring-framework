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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionInvocationTargetException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

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

	private volatile MethodExecutor cachedExecutor;


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
		TypedValue currentContext = state.getActiveContextObject();
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			// Make the root object the active context again for evaluating the parameter
			// expressions
			try {
				state.pushActiveContextObject(state.getRootContextObject());
				arguments[i] = this.children[i].getValueInternal(state).getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}
		if (currentContext.getValue() == null) {
			if (this.nullSafe) {
				return ValueRef.NullValueRef.instance;
			}
			else {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
						FormatHelper.formatMethodForMessage(this.name, getTypes(arguments)));
			}
		}
		return new MethodValueRef(state,state.getEvaluationContext(),state.getActiveContextObject().getValue(),arguments);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue currentContext = state.getActiveContextObject();
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			// Make the root object the active context again for evaluating the parameter
			// expressions
			try {
				state.pushActiveContextObject(state.getRootContextObject());
				arguments[i] = this.children[i].getValueInternal(state).getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}
		if (currentContext.getValue() == null) {
			if (this.nullSafe) {
				return TypedValue.NULL;
			}
			else {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
						FormatHelper.formatMethodForMessage(this.name, getTypes(arguments)));
			}
		}

		MethodExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(state.getEvaluationContext(),
						state.getActiveContextObject().getValue(), arguments);
			}
			catch (AccessException ae) {
				// Two reasons this can occur:
				// 1. the method invoked actually threw a real exception
				// 2. the method invoked was not passed the arguments it expected and has become 'stale'

				// In the first case we should not retry, in the second case we should see if there is a
				// better suited method.

				// To determine which situation it is, the AccessException will contain a cause.
				// If the cause is an InvocationTargetException, a user exception was thrown inside the method.
				// Otherwise the method could not be invoked.
				throwSimpleExceptionIfPossible(state, ae);

				// at this point we know it wasn't a user problem so worth a retry if a better candidate can be found
				this.cachedExecutor = null;
			}
		}

		// either there was no accessor or it no longer existed
		executorToUse = findAccessorForMethod(this.name, getTypes(arguments), state);
		this.cachedExecutor = executorToUse;
		try {
			return executorToUse.execute(state.getEvaluationContext(),
					state.getActiveContextObject().getValue(), arguments);
		}
		catch (AccessException ae) {
			// Same unwrapping exception handling as above in above catch block
			throwSimpleExceptionIfPossible(state, ae);
			throw new SpelEvaluationException( getStartPosition(), ae, SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION,
					this.name, state.getActiveContextObject().getValue().getClass().getName(), ae.getMessage());
		}
	}

	/**
	 * Decode the AccessException, throwing a lightweight evaluation exception or, if the cause was a RuntimeException,
	 * throw the RuntimeException directly.
	 */
	private void throwSimpleExceptionIfPossible(ExpressionState state, AccessException ae) {
		if (ae.getCause() instanceof InvocationTargetException) {
			Throwable rootCause = ae.getCause().getCause();
			if (rootCause instanceof RuntimeException) {
				throw (RuntimeException) rootCause;
			}
			throw new ExpressionInvocationTargetException(getStartPosition(),
					"A problem occurred when trying to execute method '" + this.name +
					"' on object of type '" + state.getActiveContextObject().getValue().getClass().getName() + "'",
					rootCause);
		}
	}

	private List<TypeDescriptor> getTypes(Object... arguments) {
		List<TypeDescriptor> descriptors = new ArrayList<TypeDescriptor>(arguments.length);
		for (Object argument : arguments) {
			descriptors.add(TypeDescriptor.forObject(argument));
		}
		return descriptors;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.name).append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	private MethodExecutor findAccessorForMethod(String name,
			List<TypeDescriptor> argumentTypes, ExpressionState state)
			throws SpelEvaluationException {
		return findAccessorForMethod(name, argumentTypes,
				state.getActiveContextObject().getValue(), state.getEvaluationContext());
	}

	private MethodExecutor findAccessorForMethod(String name,
			List<TypeDescriptor> argumentTypes, Object contextObject, EvaluationContext eContext)
			throws SpelEvaluationException {

		List<MethodResolver> methodResolvers = eContext.getMethodResolvers();
		if (methodResolvers != null) {
			for (MethodResolver methodResolver : methodResolvers) {
				try {
					MethodExecutor methodExecutor = methodResolver.resolve(eContext,
							contextObject, name, argumentTypes);
					if (methodExecutor != null) {
						return methodExecutor;
					}
				}
				catch (AccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.PROBLEM_LOCATING_METHOD, name,
							contextObject.getClass());
				}
			}
		}
		throw new SpelEvaluationException(
				getStartPosition(),
				SpelMessage.METHOD_NOT_FOUND,
				FormatHelper.formatMethodForMessage(name, argumentTypes),
				FormatHelper.formatClassNameForMessage(contextObject instanceof Class ? ((Class<?>) contextObject)
						: contextObject.getClass()));
	}


	private class MethodValueRef implements ValueRef {

		private final ExpressionState state;

		private final EvaluationContext evaluationContext;

		private final Object target;

		private final Object[] arguments;


		MethodValueRef(ExpressionState state, EvaluationContext evaluationContext, Object object, Object[] arguments) {
			this.state = state;
			this.evaluationContext = evaluationContext;
			this.target = object;
			this.arguments = arguments;
		}


		@Override
		public TypedValue getValue() {
			MethodExecutor executorToUse = MethodReference.this.cachedExecutor;
			if (executorToUse != null) {
				try {
					return executorToUse.execute(this.evaluationContext, this.target, this.arguments);
				}
				catch (AccessException ae) {
					// Two reasons this can occur:
					// 1. the method invoked actually threw a real exception
					// 2. the method invoked was not passed the arguments it expected and has become 'stale'

					// In the first case we should not retry, in the second case we should see if there is a
					// better suited method.

					// To determine which situation it is, the AccessException will contain a cause.
					// If the cause is an InvocationTargetException, a user exception was thrown inside the method.
					// Otherwise the method could not be invoked.
					throwSimpleExceptionIfPossible(this.state, ae);

					// at this point we know it wasn't a user problem so worth a retry if a better candidate can be found
					MethodReference.this.cachedExecutor = null;
				}
			}

			// either there was no accessor or it no longer existed
			executorToUse = findAccessorForMethod(MethodReference.this.name, getTypes(this.arguments), this.target, this.evaluationContext);
			MethodReference.this.cachedExecutor = executorToUse;
			try {
				return executorToUse.execute(this.evaluationContext, this.target, this.arguments);
			}
			catch (AccessException ex) {
				// Same unwrapping exception handling as above in above catch block
				throwSimpleExceptionIfPossible(this.state, ex);
				throw new SpelEvaluationException(getStartPosition(), ex,
						SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION,
						MethodReference.this.name, this.state.getActiveContextObject().getValue().getClass().getName(),
						ex.getMessage());
			}
		}

		@Override
		public void setValue(Object newValue) {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}

}
