/*
 * Copyright 2004-2008 the original author or authors.
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

/**
 * An expression capable of evaluating itself against context objects. Encapsulates the details of a previously parsed
 * expression string. Provides a common abstraction for expression evaluation independent of any language like OGNL or
 * the Unified EL.
 * 
 * @author Keith Donald
 * @author Andy Clement
 */
public interface Expression {

	/**
	 * Evaluate this expression in the default standard context.
	 * 
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue() throws EvaluationException;

	/**
	 * Evaluate the expression in the default standard context. If the result of the evaluation does not match (and
	 * cannot be converted to) the expected result type then an exception will be returned.
	 * 
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue(Class<?> desiredResultType) throws EvaluationException;

	/**
	 * Evaluate this expression in the provided context and return the result of evaluation.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue(EvaluationContext context) throws EvaluationException;

	/**
	 * Evaluate the expression in a specified context which can resolve references to properties, methods, types, etc -
	 * the type of the evaluation result is expected to be of a particular class and an exception will be thrown if it
	 * is not and cannot be converted to that type.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @param desiredResultType the class the caller would like the result to be
	 * @return the evaluation result
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public Object getValue(EvaluationContext context, Class<?> desiredResultType) throws EvaluationException;

	/**
	 * Set this expression in the provided context to the value provided.
	 * 
	 * @param context the context in which to set the value of the expression
	 * @param value the new value
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	public void setValue(EvaluationContext context, Object value) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)} method for
	 * the given context.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class getValueType(EvaluationContext context) throws EvaluationException;

	/**
	 * Returns the most general type that can be passed to the {@link #setValue(EvaluationContext, Object)} method using
	 * the default context.
	 * 
	 * @return the most general type of value that can be set on this context
	 * @throws EvaluationException if there is a problem determining the type
	 */
	public Class getValueType() throws EvaluationException;

	/**
	 * Returns the original string used to create this expression, unmodified.
	 * 
	 * @return the original expression string
	 */
	public String getExpressionString();

	/**
	 * Determine if an expression can be written to, i.e. setValue() can be called.
	 * 
	 * @param context the context in which the expression should be checked
	 * @return true if the expression is writable
	 * @throws EvaluationException if there is a problem determining if it is writable
	 */
	public boolean isWritable(EvaluationContext context) throws EvaluationException;
}