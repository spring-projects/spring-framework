/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;

/**
 * An expression capable of evaluating itself against context objects.
 * Encapsulates the details of a previously parsed expression string.
 * Provides a common abstraction for expression evaluation independent
 * of any language like OGNL or the Unified EL.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface Expression {

	/**
	 * Evaluate this expression in the default standard context.
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	Object getValue() throws EvaluationException;

	/**
	 * Evaluate this expression against the specified root object
	 * @param rootObject the root object against which properties/etc will be resolved
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	Object getValue(Object rootObject) throws EvaluationException;

	/**
	 * Evaluate the expression in the default context. If the result of the evaluation does not match (and
	 * cannot be converted to) the expected result type then an exception will be returned.
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	<T> T getValue(Class<T> desiredResultType) throws EvaluationException;

	/**
	 * Evaluate the expression in the default context against the specified root object. If the
	 * result of the evaluation does not match (and cannot be converted to) the expected result type
	 * then an exception will be returned.
	 * @param rootObject the root object against which properties/etc will be resolved
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	<T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context and return the result of evaluation.
	 * @param context the context in which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	Object getValue(EvaluationContext context) throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context and return the result of evaluation, but use
	 * the supplied root context as an override for any default root object specified in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which properties/etc will be resolved
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * Evaluate the expression in a specified context which can resolve references to properties, methods, types, etc -
	 * the type of the evaluation result is expected to be of a particular class and an exception will be thrown if it
	 * is not and cannot be converted to that type.
	 * @param context the context in which to evaluate the expression
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	<T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException;

	/**
	 * Evaluate the expression in a specified context which can resolve references to properties, methods, types, etc -
	 * the type of the evaluation result is expected to be of a particular class and an exception will be thrown if it
	 * is not and cannot be converted to that type.  The supplied root object overrides any default specified on the
	 * supplied context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which properties/etc will be resolved
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	<T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method using the default context.
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	Class getValueType() throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method using the default context.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	Class getValueType(Object rootObject) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method for the given context.
	 * @param context the context in which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	Class getValueType(EvaluationContext context) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method for the given context. The supplied root object overrides any specified in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	Class getValueType(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method using the default context.
	 * @return a type descriptor for the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method using the default context.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)}
	 * method for the given context.
	 * @param context the context in which to evaluate the expression
	 * @return a type descriptor for the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)} method for
	 * the given context. The supplied root object overrides any specified in the context.
	 * @param context the context in which to evaluate the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @return a type descriptor for the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * Determine if an expression can be written to, i.e. setValue() can be called.
	 * @param context the context in which the expression should be checked
	 * @return true if the expression is writable
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(EvaluationContext context) throws EvaluationException;

	/**
	 * Determine if an expression can be written to, i.e. setValue() can be called.
	 * The supplied root object overrides any specified in the context.
	 * @param context the context in which the expression should be checked
	 * @param rootObject the root object against which to evaluate the expression
	 * @return true if the expression is writable
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * Determine if an expression can be written to, i.e. setValue() can be called.
	 * @param rootObject the root object against which to evaluate the expression
	 * @return true if the expression is writable
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	boolean isWritable(Object rootObject) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 *
	 * @param context the context in which to set the value of the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(EvaluationContext context, Object value) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(Object rootObject, Object value) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 * The supplied root object overrides any specified in the context.
	 * @param context the context in which to set the value of the expression
	 * @param rootObject the root object against which to evaluate the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException;

	/**
	 * Returns the original string used to create this expression, unmodified.
	 * @return the original expression string
	 */
	String getExpressionString();

}
