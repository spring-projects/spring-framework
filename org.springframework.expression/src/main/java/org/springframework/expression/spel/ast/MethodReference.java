/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MethodReference extends SpelNodeImpl {

	private final String name;

	private volatile MethodExecutor cachedExecutor;


	public MethodReference(Token payload) {
		super(payload);
		name = payload.getText();
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue currentContext = state.getActiveContextObject();
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = getChild(i).getValueInternal(state).getValue();
		}
		if (currentContext.getValue() == null) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
					FormatHelper.formatMethodForMessage(name, getTypes(arguments)));
		}

		MethodExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(
						state.getEvaluationContext(), state.getActiveContextObject().getValue(), arguments);
			}
			catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
				this.cachedExecutor = null;
			}
		}

		// either there was no accessor or it no longer existed
		executorToUse = findAccessorForMethod(this.name, getTypes(arguments), state);
		this.cachedExecutor = executorToUse;
		try {
			return executorToUse.execute(
					state.getEvaluationContext(), state.getActiveContextObject().getValue(), arguments);
		} catch (AccessException ae) {
			throw new SpelException(getCharPositionInLine(), ae, SpelMessages.EXCEPTION_DURING_METHOD_INVOCATION,
					this.name, state.getActiveContextObject().getValue().getClass().getName(), ae.getMessage());
		}
	}

	private Class<?>[] getTypes(Object... arguments) {
		Class<?>[] argumentTypes = new Class[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			argumentTypes[i] = (arguments[i]==null?Object.class:arguments[i].getClass());
		}
		return argumentTypes;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	private MethodExecutor findAccessorForMethod(String name, Class<?>[] argumentTypes, ExpressionState state)
			throws SpelException {

		TypedValue context = state.getActiveContextObject();
		Object contextObject = context.getValue();
		EvaluationContext eContext = state.getEvaluationContext();

		List<MethodResolver> mResolvers = eContext.getMethodResolvers();
		if (mResolvers != null) {
			for (MethodResolver methodResolver : mResolvers) {
				try {
					MethodExecutor cEx = methodResolver.resolve(
							state.getEvaluationContext(), contextObject, name, argumentTypes);
					if (cEx != null) {
						return cEx;
					}
				}
				catch (AccessException ex) {
					throw new SpelException(ex, SpelMessages.PROBLEM_LOCATING_METHOD, name, contextObject.getClass());
				}
			}
		}
		throw new SpelException(SpelMessages.METHOD_NOT_FOUND, FormatHelper.formatMethodForMessage(name, argumentTypes),
				FormatHelper.formatClassNameForMessage(contextObject instanceof Class ? ((Class<?>) contextObject) : contextObject.getClass()));
	}

}
