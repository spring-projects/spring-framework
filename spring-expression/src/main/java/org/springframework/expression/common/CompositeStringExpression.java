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

package org.springframework.expression.common;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;

/**
 * Represents a template expression broken into pieces.
 *
 * <p>Each piece will be an {@link Expression}, but pure text parts of the
 * template will be represented as {@link LiteralExpression} objects. An example
 * of a template expression might be:
 *
 * <pre class="code">
 * &quot;Hello ${getName()}&quot;
 * </pre>
 *
 * which will be represented as a {@code CompositeStringExpression} of two parts:
 * the first part being a {@link LiteralExpression} representing 'Hello ' and the
 * second part being a real expression that will call {@code getName()} when invoked.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class CompositeStringExpression implements Expression {

	private final String expressionString;

	/** The array of expressions that make up the composite expression. */
	private final Expression[] expressions;


	public CompositeStringExpression(String expressionString, Expression[] expressions) {
		this.expressionString = expressionString;
		this.expressions = expressions;
	}


	@Override
	public final String getExpressionString() {
		return this.expressionString;
	}

	public final Expression[] getExpressions() {
		return this.expressions;
	}

	@Override
	public String getValue() throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	public <T> @Nullable T getValue(@Nullable Class<T> expectedResultType) throws EvaluationException {
		String value = getValue();
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
	}

	@Override
	public String getValue(@Nullable Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	public <T> @Nullable T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException {
		String value = getValue(rootObject);
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), desiredResultType);
	}

	@Override
	public String getValue(EvaluationContext context) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(context, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType)
			throws EvaluationException {

		String value = getValue(context);
		return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
	}

	@Override
	public String getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(context, rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		String value = getValue(context,rootObject);
		return ExpressionUtils.convertTypedValue(context, new TypedValue(value), desiredResultType);
	}

	@Override
	public Class<?> getValueType() {
		return String.class;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) {
		return String.class;
	}

	@Override
	public Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context) {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

}
