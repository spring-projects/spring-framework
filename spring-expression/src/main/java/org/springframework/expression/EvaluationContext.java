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

package org.springframework.expression;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * Expressions are executed in an evaluation context. It is in this context that
 * references are resolved when encountered during expression evaluation.
 *
 * <p>There are two default implementations of this interface.
 * <ul>
 * <li>{@link org.springframework.expression.spel.support.SimpleEvaluationContext
 * SimpleEvaluationContext}: a simpler builder-style {@code EvaluationContext}
 * variant for data-binding purposes, which allows for opting into several SpEL
 * features as needed.</li>
 * <li>{@link org.springframework.expression.spel.support.StandardEvaluationContext
 * StandardEvaluationContext}: a powerful and highly configurable {@code EvaluationContext}
 * implementation, which can be extended, rather than having to implement everything
 * manually.</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public interface EvaluationContext {

	/**
	 * Return the default root context object against which unqualified
	 * properties, methods, etc. should be resolved.
	 * <p>This can be overridden when evaluating an expression.
	 */
	TypedValue getRootObject();

	/**
	 * Return a list of accessors that will be asked in turn to read/write a property.
	 * <p>The default implementation returns an empty list.
	 */
	default List<PropertyAccessor> getPropertyAccessors() {
		return Collections.emptyList();
	}

	/**
	 * Return a list of index accessors that will be asked in turn to access or
	 * set an indexed value.
	 * <p>The default implementation returns an empty list.
	 * @since 6.2
	 */
	default List<IndexAccessor> getIndexAccessors() {
		return Collections.emptyList();
	}

	/**
	 * Return a list of resolvers that will be asked in turn to locate a constructor.
	 * <p>The default implementation returns an empty list.
	 */
	default List<ConstructorResolver> getConstructorResolvers() {
		return Collections.emptyList();
	}

	/**
	 * Return a list of resolvers that will be asked in turn to locate a method.
	 * <p>The default implementation returns an empty list.
	 */
	default List<MethodResolver> getMethodResolvers() {
		return Collections.emptyList();
	}

	/**
	 * Return a bean resolver that can look up beans by name.
	 */
	@Nullable
	BeanResolver getBeanResolver();

	/**
	 * Return a type locator that can be used to find types, either by short or
	 * fully qualified name.
	 */
	TypeLocator getTypeLocator();

	/**
	 * Return a type converter that can convert (or coerce) a value from one type to another.
	 */
	TypeConverter getTypeConverter();

	/**
	 * Return a type comparator for comparing pairs of objects for equality.
	 */
	TypeComparator getTypeComparator();

	/**
	 * Return an operator overloader that may support mathematical operations
	 * between more than the standard set of types.
	 */
	OperatorOverloader getOperatorOverloader();

	/**
	 * Assign the value created by the specified {@link Supplier} to a named variable
	 * within this evaluation context.
	 * <p>In contrast to {@link #setVariable(String, Object)}, this method should only
	 * be invoked to support the assignment operator ({@code =}) within an expression.
	 * <p>By default, this method delegates to {@code setVariable(String, Object)},
	 * providing the value created by the {@code valueSupplier}. Concrete implementations
	 * may override this <em>default</em> method to provide different semantics.
	 * @param name the name of the variable to assign
	 * @param valueSupplier the supplier of the value to be assigned to the variable
	 * @return a {@link TypedValue} wrapping the assigned value
	 * @since 5.2.24
	 */
	default TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		TypedValue typedValue = valueSupplier.get();
		setVariable(name, typedValue.getValue());
		return typedValue;
	}

	/**
	 * Set a named variable in this evaluation context to a specified value.
	 * <p>In contrast to {@link #assignVariable(String, Supplier)}, this method
	 * should only be invoked programmatically when interacting directly with the
	 * {@code EvaluationContext} &mdash; for example, to provide initial
	 * configuration for the context.
	 * @param name the name of the variable to set
	 * @param value the value to be placed in the variable
	 * @see #lookupVariable(String)
	 */
	void setVariable(String name, @Nullable Object value);

	/**
	 * Look up a named variable within this evaluation context.
	 * @param name the name of the variable to look up
	 * @return the value of the variable, or {@code null} if not found
	 */
	@Nullable
	Object lookupVariable(String name);

	/**
	 * Determine if assignment is enabled within expressions evaluated by this evaluation
	 * context.
	 * <p>If this method returns {@code false}, the assignment ({@code =}), increment
	 * ({@code ++}), and decrement ({@code --}) operators are disabled.
	 * <p>By default, this method returns {@code true}. Concrete implementations may override
	 * this <em>default</em> method to disable assignment.
	 * @return {@code true} if assignment is enabled; {@code false} otherwise
	 * @since 5.3.38
	 */
	default boolean isAssignmentEnabled() {
		return true;
	}

}
