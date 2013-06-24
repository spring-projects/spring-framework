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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;

/**
 * A very simple hardcoded implementation of the Expression interface that represents a
 * string literal. It is used with CompositeStringExpression when representing a template
 * expression which is made up of pieces - some being real expressions to be handled by an
 * EL implementation like Spel, and some being just textual elements.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class LiteralExpression implements Expression {

	/** Fixed literal value of this expression */
	private final String literalValue;


	public LiteralExpression(String literalValue) {
		this.literalValue = literalValue;
	}


	@Override
	public final String getExpressionString() {
		return this.literalValue;
	}

	@Override
	public String getValue() {
		return this.literalValue;
	}

	@Override
	public String getValue(EvaluationContext context) {
		return this.literalValue;
	}

	@Override
	public String getValue(Object rootObject) {
		return this.literalValue;
	}

	@Override
	public Class getValueType(EvaluationContext context) {
		return String.class;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
	}

	@Override
	public <T> T getValue(EvaluationContext context, Class<T> expectedResultType) throws EvaluationException {
		Object value = getValue(context);
		return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
	}

	@Override
	public <T> T getValue(Class<T> expectedResultType) throws EvaluationException {
		Object value = getValue();
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
	}

	@Override
	public boolean isWritable(EvaluationContext context) {
		return false;
	}

	@Override
	public Class getValueType() {
		return String.class;
	}

	@Override
	public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		Object value = getValue(rootObject);
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), desiredResultType);
	}

	@Override
	public String getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		return this.literalValue;
	}

	@Override
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException {
		Object value = getValue(context, rootObject);
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), desiredResultType);
	}

	@Override
	public Class getValueType(Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public Class getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
	}

	@Override
	public boolean isWritable(Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
	}

}
