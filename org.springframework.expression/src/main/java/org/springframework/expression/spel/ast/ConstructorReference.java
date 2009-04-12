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
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

// TODO asc array constructor call logic has been removed for now
/**
 * Represents the invocation of a constructor. Either a constructor on a regular type or construction of an array. When
 * an array is constructed, an initializer can be specified.
 * <p>
 * Examples:<br>
 * new String('hello world')<br>
 * new int[]{1,2,3,4}<br>
 * new int[3] new int[3]{1,2,3}
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ConstructorReference extends SpelNodeImpl {

	// TODO is this caching safe - passing the expression around will mean this executor is also being passed around
	/**
	 * The cached executor that may be reused on subsequent evaluations.
	 */
	private volatile ConstructorExecutor cachedExecutor;

	public ConstructorReference(Token payload) {
		super(payload);
	}

	/**
	 * Implements getValue() - delegating to the code for building an array or a simple type.
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return createNewInstance(state);
	}

	/**
	 * Create a new ordinary object and return it.
	 * @param state the expression state within which this expression is being evaluated
	 * @return the new object
	 * @throws EvaluationException if there is a problem creating the object
	 */
	private TypedValue createNewInstance(ExpressionState state) throws EvaluationException {
		Object[] arguments = new Object[getChildCount() - 1];
		Class<?>[] argumentTypes = new Class[getChildCount() - 1];
		for (int i = 0; i < arguments.length; i++) {
			TypedValue childValue = getChild(i + 1).getValueInternal(state);
			Object value = childValue.getValue();
			arguments[i] = value;
			argumentTypes[i] = (value==null?Object.class:value.getClass());
		}

		ConstructorExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(state.getEvaluationContext(), arguments);
			}
			catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
				this.cachedExecutor = null;
			}
		}

		// either there was no accessor or it no longer exists
		String typename = (String) getChild(0).getValueInternal(state).getValue();
		executorToUse = findExecutorForConstructor(typename, argumentTypes, state);
		try {
			this.cachedExecutor = executorToUse;
			TypedValue result = executorToUse.execute(state.getEvaluationContext(), arguments);
			return result;
		} catch (AccessException ae) {
			throw new SpelException(ae, SpelMessages.EXCEPTION_DURING_CONSTRUCTOR_INVOCATION, typename, ae.getMessage());
		}
	}

	/**
	 * Go through the list of registered constructor resolvers and see if any can find a constructor that takes the
	 * specified set of arguments.
	 * @param typename the type trying to be constructed
	 * @param argumentTypes the types of the arguments supplied that the constructor must take
	 * @param state the current state of the expression
	 * @return a reusable ConstructorExecutor that can be invoked to run the constructor or null
	 * @throws SpelException if there is a problem locating the constructor
	 */
	private ConstructorExecutor findExecutorForConstructor(
			String typename, Class<?>[] argumentTypes, ExpressionState state) throws SpelException {

		EvaluationContext eContext = state.getEvaluationContext();
		List<ConstructorResolver> cResolvers = eContext.getConstructorResolvers();
		if (cResolvers != null) {
			for (ConstructorResolver ctorResolver : cResolvers) {
				try {
					ConstructorExecutor cEx = ctorResolver.resolve(state.getEvaluationContext(), typename,
							argumentTypes);
					if (cEx != null) {
						return cEx;
					}
				}
				catch (AccessException ex) {
					throw new SpelException(ex, SpelMessages.PROBLEM_LOCATING_CONSTRUCTOR, typename,
							FormatHelper.formatMethodForMessage("", argumentTypes));
				}
			}
		}
		throw new SpelException(SpelMessages.CONSTRUCTOR_NOT_FOUND, typename, FormatHelper.formatMethodForMessage("",
				argumentTypes));
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");

		int index = 0;
		sb.append(getChild(index++).toStringAST());

			sb.append("(");
			for (int i = index; i < getChildCount(); i++) {
				if (i > index)
					sb.append(",");
				sb.append(getChild(i).toStringAST());
			}
			sb.append(")");
		return sb.toString();
	}

}
