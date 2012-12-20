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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;

/**
 * Represents a template expression broken into pieces. Each piece will be an Expression but pure text parts to the
 * template will be represented as LiteralExpression objects. An example of a template expression might be:
 *
 * <pre class="code">
 * &quot;Hello ${getName()}&quot;</pre>
 *
 * which will be represented as a CompositeStringExpression of two parts. The first part being a
 * LiteralExpression representing 'Hello ' and the second part being a real expression that will
 * call <code>getName()</code> when invoked.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class CompositeStringExpression implements Expression {

	private final String expressionString;

	/** The array of expressions that make up the composite expression */
	private final Expression[] expressions;


	public CompositeStringExpression(String expressionString, Expression[] expressions) {
		this.expressionString = expressionString;
		this.expressions = expressions;
	}


	public final String getExpressionString() {
		return this.expressionString;
	}

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

	public String getValue(Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

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

	public String getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(context, rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	public Class<?> getValueType(EvaluationContext context) {
		return String.class;
	}

	public Class<?> getValueType() {
		return String.class;
	}

	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
		return TypeDescriptor.valueOf(String.class);
	}

	public TypeDescriptor getValueTypeDescriptor() {
		return TypeDescriptor.valueOf(String.class);
	}

	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	public <T> T getValue(EvaluationContext context, Class<T> expectedResultType) throws EvaluationException {
		Object value = getValue(context);
		return ExpressionUtils.convert(context, value, expectedResultType);
	}

	public <T> T getValue(Class<T> expectedResultType) throws EvaluationException {
		Object value = getValue();
		return ExpressionUtils.convert(null, value, expectedResultType);
	}

	public boolean isWritable(EvaluationContext context) {
		return false;
	}

	public Expression[] getExpressions() {
		return expressions;
	}


	public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		Object value = getValue(rootObject);
		return ExpressionUtils.convert(null, value, desiredResultType);
	}

	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType)
			throws EvaluationException {
		Object value = getValue(context,rootObject);
		return ExpressionUtils.convert(context, value, desiredResultType);
	}

	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		return String.class;
	}

	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		return String.class;
	}

	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		return false;
	}

	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	public boolean isWritable(Object rootObject) throws EvaluationException {
		return false;
	}

	public void setValue(Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

}
