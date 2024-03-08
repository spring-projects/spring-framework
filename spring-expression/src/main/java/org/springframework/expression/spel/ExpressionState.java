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

package org.springframework.expression.spel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * ExpressionState is for maintaining per-expression-evaluation state: any changes to
 * it are not seen by other expressions, but it gives a place to hold local variables and
 * for component expressions in a compound expression to communicate state. This is in
 * contrast to the EvaluationContext, which is shared amongst expression evaluations, and
 * any changes to it will be seen by other expressions or any code that chooses to ask
 * questions of the context.
 *
 * <p>It also acts as a place to define common utility routines that the various AST
 * nodes might need.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class ExpressionState {

	private final EvaluationContext relatedContext;

	private final TypedValue rootObject;

	private final SpelParserConfiguration configuration;

	@Nullable
	private Deque<TypedValue> contextObjects;

	@Nullable
	private Deque<VariableScope> variableScopes;

	// When entering a new scope there is a new base object which should be used
	// for '#this' references (or to act as a target for unqualified references).
	// This ArrayDeque captures those objects at each nested scope level.
	// For example:
	// #list1.?[#list2.contains(#this)]
	// On entering the selection we enter a new scope, and #this is now the
	// element from list1.
	@Nullable
	private Deque<TypedValue> scopeRootObjects;


	public ExpressionState(EvaluationContext context) {
		this(context, context.getRootObject(), new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, SpelParserConfiguration configuration) {
		this(context, context.getRootObject(), configuration);
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject) {
		this(context, rootObject, new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject, SpelParserConfiguration configuration) {
		Assert.notNull(context, "EvaluationContext must not be null");
		Assert.notNull(rootObject, "'rootObject' must not be null");
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.relatedContext = context;
		this.rootObject = rootObject;
		this.configuration = configuration;
	}


	/**
	 * The active context object is what unqualified references to properties/etc are resolved against.
	 */
	public TypedValue getActiveContextObject() {
		if (CollectionUtils.isEmpty(this.contextObjects)) {
			return this.rootObject;
		}
		return this.contextObjects.element();
	}

	public void pushActiveContextObject(TypedValue obj) {
		initContextObjects().push(obj);
	}

	public void popActiveContextObject() {
		try {
			initContextObjects().pop();
		}
		catch (NoSuchElementException ex) {
			throw new IllegalStateException("Cannot pop active context object: stack is empty");
		}
	}

	public TypedValue getRootContextObject() {
		return this.rootObject;
	}

	public TypedValue getScopeRootContextObject() {
		if (CollectionUtils.isEmpty(this.scopeRootObjects)) {
			return this.rootObject;
		}
		return this.scopeRootObjects.element();
	}

	/**
	 * Assign the value created by the specified {@link Supplier} to a named variable
	 * within the evaluation context.
	 * <p>In contrast to {@link #setVariable(String, Object)}, this method should
	 * only be invoked to support assignment within an expression.
	 * @param name the name of the variable to assign
	 * @param valueSupplier the supplier of the value to be assigned to the variable
	 * @return a {@link TypedValue} wrapping the assigned value
	 * @since 5.2.24
	 * @see EvaluationContext#assignVariable(String, Supplier)
	 */
	public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		return this.relatedContext.assignVariable(name, valueSupplier);
	}

	/**
	 * Set a named variable in the evaluation context to a specified value.
	 * <p>In contrast to {@link #assignVariable(String, Supplier)}, this method
	 * should only be invoked programmatically.
	 * @param name the name of the variable to set
	 * @param value the value to be placed in the variable
	 * @see EvaluationContext#setVariable(String, Object)
	 */
	public void setVariable(String name, @Nullable Object value) {
		this.relatedContext.setVariable(name, value);
	}

	/**
	 * Look up a named global variable in the evaluation context.
	 * @param name the name of the variable to look up
	 * @return a {@link TypedValue} containing the value of the variable, or
	 * {@link TypedValue#NULL} if the variable does not exist
	 * @see #assignVariable(String, Supplier)
	 * @see #setVariable(String, Object)
	 */
	public TypedValue lookupVariable(String name) {
		Object value = this.relatedContext.lookupVariable(name);
		return (value != null ? new TypedValue(value) : TypedValue.NULL);
	}

	public TypeComparator getTypeComparator() {
		return this.relatedContext.getTypeComparator();
	}

	public Class<?> findType(String type) throws EvaluationException {
		return this.relatedContext.getTypeLocator().findType(type);
	}

	public TypeConverter getTypeConverter() {
		return this.relatedContext.getTypeConverter();
	}

	public Object convertValue(Object value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		Object result = this.relatedContext.getTypeConverter().convertValue(
				value, TypeDescriptor.forObject(value), targetTypeDescriptor);
		if (result == null) {
			throw new IllegalStateException("Null conversion result for value [" + value + "]");
		}
		return result;
	}

	@Nullable
	public Object convertValue(TypedValue value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		Object val = value.getValue();
		return this.relatedContext.getTypeConverter().convertValue(
				val, TypeDescriptor.forObject(val), targetTypeDescriptor);
	}

	/**
	 * Enter a new scope with a new {@linkplain #getActiveContextObject() root
	 * context object} and a new local variable scope.
	 */
	public void enterScope() {
		initVariableScopes().push(new VariableScope());
		initScopeRootObjects().push(getActiveContextObject());
	}

	/**
	 * Enter a new scope with a new {@linkplain #getActiveContextObject() root
	 * context object} and a new local variable scope containing the supplied
	 * name/value pair.
	 * @param name the name of the local variable
	 * @param value the value of the local variable
	 * @deprecated as of 6.2 with no replacement; to be removed in 7.0
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public void enterScope(String name, Object value) {
		initVariableScopes().push(new VariableScope(name, value));
		initScopeRootObjects().push(getActiveContextObject());
	}

	/**
	 * Enter a new scope with a new {@linkplain #getActiveContextObject() root
	 * context object} and a new local variable scope containing the supplied
	 * name/value pairs.
	 * @param variables a map containing name/value pairs for local variables
	 * @deprecated as of 6.2 with no replacement; to be removed in 7.0
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public void enterScope(@Nullable Map<String, Object> variables) {
		initVariableScopes().push(new VariableScope(variables));
		initScopeRootObjects().push(getActiveContextObject());
	}

	public void exitScope() {
		initVariableScopes().pop();
		initScopeRootObjects().pop();
	}

	/**
	 * Set a local variable with the given name to the supplied value within the
	 * current scope.
	 * <p>If a local variable with the given name already exists, it will be
	 * overwritten.
	 * @param name the name of the local variable
	 * @param value the value of the local variable
	 * @deprecated as of 6.2 with no replacement; to be removed in 7.0
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public void setLocalVariable(String name, Object value) {
		initVariableScopes().element().setVariable(name, value);
	}

	/**
	 * Look up the value of the local variable with the given name.
	 * @param name the name of the local variable
	 * @return the value of the local variable, or {@code null} if the variable
	 * does not exist in the current scope
	 * @deprecated as of 6.2 with no replacement; to be removed in 7.0
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	@Nullable
	public Object lookupLocalVariable(String name) {
		for (VariableScope scope : initVariableScopes()) {
			if (scope.definesVariable(name)) {
				return scope.lookupVariable(name);
			}
		}
		return null;
	}

	private Deque<TypedValue> initContextObjects() {
		if (this.contextObjects == null) {
			this.contextObjects = new ArrayDeque<>();
		}
		return this.contextObjects;
	}

	private Deque<TypedValue> initScopeRootObjects() {
		if (this.scopeRootObjects == null) {
			this.scopeRootObjects = new ArrayDeque<>();
		}
		return this.scopeRootObjects;
	}

	private Deque<VariableScope> initVariableScopes() {
		if (this.variableScopes == null) {
			this.variableScopes = new ArrayDeque<>();
			// top-level empty variable scope
			this.variableScopes.add(new VariableScope());
		}
		return this.variableScopes;
	}

	public TypedValue operate(Operation op, @Nullable Object left, @Nullable Object right) throws EvaluationException {
		OperatorOverloader overloader = this.relatedContext.getOperatorOverloader();
		if (overloader.overridesOperation(op, left, right)) {
			Object returnValue = overloader.operate(op, left, right);
			return new TypedValue(returnValue);
		}
		else {
			String leftType = (left == null ? "null" : left.getClass().getName());
			String rightType = (right == null? "null" : right.getClass().getName());
			throw new SpelEvaluationException(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, leftType, rightType);
		}
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return this.relatedContext.getPropertyAccessors();
	}

	public EvaluationContext getEvaluationContext() {
		return this.relatedContext;
	}

	public SpelParserConfiguration getConfiguration() {
		return this.configuration;
	}


	/**
	 * A new local variable scope is entered when a new expression scope is
	 * entered and exited when the corresponding expression scope is exited.
	 *
	 * <p>If variable names clash with those in a higher level scope, those in
	 * the higher level scope will not be accessible within the current scope.
	 */
	private static class VariableScope {

		private final Map<String, Object> variables = new HashMap<>();

		VariableScope() {
		}

		VariableScope(String name, Object value) {
			this.variables.put(name, value);
		}

		VariableScope(@Nullable Map<String, Object> variables) {
			if (variables != null) {
				this.variables.putAll(variables);
			}
		}

		@Nullable
		Object lookupVariable(String name) {
			return this.variables.get(name);
		}

		void setVariable(String name, Object value) {
			this.variables.put(name,value);
		}

		boolean definesVariable(String name) {
			return this.variables.containsKey(name);
		}
	}

}
