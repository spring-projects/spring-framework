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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * An expression capable of evaluating itself against context objects.
 *
 * <p>Encapsulates the details of a previously parsed expression string.
 *
 * <p>Provides a common abstraction for expression evaluation.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface Expression {

	/**
	 * Return the original string used to create this expression (unmodified).
	 * @return the original expression string
	 */
	String getExpressionString();

	/**
	 * Evaluate this expression in the default context and return the result of evaluation.
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	Object getValue() throws EvaluationException;

	/**
	 * Evaluate this expression in the default context and return the result of evaluation.
	 * <p>If the result of the evaluation does not match (and cannot be converted to)
	 * the expected result type then an exception will be thrown.
	 * @param desiredResultType the type the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	<T> T getValue(@Nullable Class<T> desiredResultType) throws EvaluationException;

	/**
	 * Evaluate this expression in the default context against the specified root object
	 * and return the result of evaluation.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	Object getValue(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * Evaluate this expression in the default context against the specified root object
	 * and return the result of evaluation.
	 * <p>If the result of the evaluation does not match (and cannot be converted to)
	 * the expected result type then an exception will be thrown.
	 * @param rootObject the root object against which to evaluate the expression
	 * @param desiredResultType the type the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	<T> T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context and return the result of evaluation.
	 * @param context the context in which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	Object getValue(EvaluationContext context) throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context against the specified root object
	 * and return the result of evaluation.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	Object getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context and return the result of evaluation.
	 * <p>If the result of the evaluation does not match (and cannot be converted to)
	 * the expected result type then an exception will be thrown.
	 * @param context the context in which to evaluate the expression
	 * @param desiredResultType the type the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	<T> T getValue(EvaluationContext context, @Nullable Class<T> desiredResultType)
			throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context against the specified root object
	 * and return the result of evaluation.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * <p>If the result of the evaluation does not match (and cannot be converted to)
	 * the expected result type then an exception will be thrown.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @param desiredResultType the type the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	<T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException;

	/**
	 * Return the most general type that can be passed to the
	 * {@link #setValue(EvaluationContext, Object)} method using the default context.
	 * @return the most general type of value that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	Class<?> getValueType() throws EvaluationException;

	/**
	 * Return the most general type that can be passed to the
	 * {@link #setValue(Object, Object)} method using the default context.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * Return the most general type that can be passed to the
	 * {@link #setValue(EvaluationContext, Object)} method for the given context.
	 * @param context the context in which to evaluate the expression
	 * @return the most general type of value that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	Class<?> getValueType(EvaluationContext context) throws EvaluationException;

	/**
	 * Return the most general type that can be passed to the
	 * {@link #setValue(EvaluationContext, Object, Object)} method for the given context.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * Return a descriptor for the most general type that can be passed to one of
	 * the {@code setValue(...)} methods using the default context.
	 * @return a type descriptor for values that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

	/**
	 * Return a descriptor for the most general type that can be passed to the
	 * {@link #setValue(Object, Object)} method using the default context.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for values that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * Return a descriptor for the most general type that can be passed to the
	 * {@link #setValue(EvaluationContext, Object)} method for the given context.
	 * @param context the context in which to evaluate the expression
	 * @return a type descriptor for values that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

	/**
	 * Return a descriptor for the most general type that can be passed to the
	 * {@link #setValue(EvaluationContext, Object, Object)} method for the given
	 * context.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for values that can be set in this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException;

	/**
	 * Determine if this expression can be written to, i.e. setValue() can be called.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * Determine if this expression can be written to, i.e. setValue() can be called.
	 * @param context the context in which the expression should be checked
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(EvaluationContext context) throws EvaluationException;

	/**
	 * Determine if this expression can be written to, i.e. setValue() can be called.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * @param context the context in which the expression should be checked
	 * @param rootObject the root object against which to evaluate the expression
	 * @return {@code true} if the expression is writable; {@code false} otherwise
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * Set this expression in the default context to the value provided.
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 * @param context the context in which to set the value of the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 * <p>The supplied root object will be used as an override for any default root object
	 * configured in the context.
	 * @param context the context in which to set the value of the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value)
			throws EvaluationException;

}
