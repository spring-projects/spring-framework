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

package org.springframework.expression.spel.standard;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@code SpelExpression} represents a parsed (valid) expression that is ready to be
 * evaluated in a specified context. An expression can be evaluated standalone or in a
 * specified context. During expression evaluation the context may be asked to resolve
 * references to types, beans, properties, and methods.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class SpelExpression implements Expression {

	private final String expression;

	private final SpelNodeImpl ast;

	private final SpelParserConfiguration configuration;

	// the default context is used if no override is supplied by the user
	private EvaluationContext defaultContext;


	/**
	 * Construct an expression, only used by the parser.
	 */
	public SpelExpression(String expression, SpelNodeImpl ast, SpelParserConfiguration configuration) {
		this.expression = expression;
		this.ast = ast;
		this.configuration = configuration;
	}


	// implementing Expression

	@Override
	public Object getValue() throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		return this.ast.getValue(expressionState);
	}

	@Override
	public Object getValue(Object rootObject) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		return this.ast.getValue(expressionState);
	}

	@Override
	public <T> T getValue(Class<T> expectedResultType) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		return ExpressionUtils.convertTypedValue(expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public <T> T getValue(Object rootObject, Class<T> expectedResultType) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		return ExpressionUtils.convertTypedValue(expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public Object getValue(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		return this.ast.getValue(new ExpressionState(context, this.configuration));
	}

	@Override
	public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		return this.ast.getValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
	}

	@Override
	public <T> T getValue(EvaluationContext context, Class<T> expectedResultType) throws EvaluationException {
		TypedValue typedResultValue = this.ast.getTypedValue(new ExpressionState(context, this.configuration));
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> expectedResultType) throws EvaluationException {
		TypedValue typedResultValue = this.ast.getTypedValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public Class<?> getValueType() throws EvaluationException {
		return getValueType(getEvaluationContext());
	}

	@Override
	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		return getValueType(getEvaluationContext(), rootObject);
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		ExpressionState eState = new ExpressionState(context, this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(eState).getTypeDescriptor();
		return typeDescriptor != null ? typeDescriptor.getType() : null;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		ExpressionState eState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(eState).getTypeDescriptor();
		return typeDescriptor != null ? typeDescriptor.getType() : null;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return getValueTypeDescriptor(getEvaluationContext());
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		ExpressionState eState = new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(eState).getTypeDescriptor();
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		ExpressionState eState = new ExpressionState(context, this.configuration);
		return this.ast.getValueInternal(eState).getTypeDescriptor();
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		ExpressionState eState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(eState).getTypeDescriptor();
	}

	@Override
	public String getExpressionString() {
		return this.expression;
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, this.configuration));
	}

	@Override
	public boolean isWritable(Object rootObject) throws EvaluationException {
		return this.ast.isWritable(new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
	}

	@Override
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, this.configuration), value);
	}

	@Override
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		this.ast.setValue(new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
		Assert.notNull(context, "The EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration), value);
	}

	// impl only

	/**
	 * @return return the Abstract Syntax Tree for the expression
	 */
	public SpelNode getAST() {
		return this.ast;
	}

	/**
	 * Produce a string representation of the Abstract Syntax Tree for the expression, this should ideally look like the
	 * input expression, but properly formatted since any unnecessary whitespace will have been discarded during the
	 * parse of the expression.
	 * @return the string representation of the AST
	 */
	public String toStringAST() {
		return this.ast.toStringAST();
	}

	/**
     * Return the default evaluation context that will be used if none is supplied on an evaluation call
     * @return the default evaluation context
     */
	public EvaluationContext getEvaluationContext() {
		if (this.defaultContext == null) {
			this.defaultContext = new StandardEvaluationContext();
		}
		return this.defaultContext;
	}

	/**
     * Set the evaluation context that will be used if none is specified on an evaluation call.
     * @param context an evaluation context
     */
	public void setEvaluationContext(EvaluationContext context) {
		this.defaultContext = context;
	}

	private TypedValue toTypedValue(Object object) {
		if (object == null) {
			return TypedValue.NULL;
		}
		else {
			return new TypedValue(object);
		}
	}

}
