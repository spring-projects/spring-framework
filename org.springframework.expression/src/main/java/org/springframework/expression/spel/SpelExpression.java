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
package org.springframework.expression.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.ast.SpelNode;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * A SpelExpressions represents a parsed (valid) expression that is ready to be evaluated in a specified context. An
 * expression can be evaluated standalone or in a specified context. During expression evaluation the context may be
 * asked to resolve references to types, beans, properties, methods.
 * 
 * @author Andy Clement
 * 
 */
public class SpelExpression implements Expression {
	private final String expression;
	public final SpelNode ast;

	/**
	 * Construct an expression, only used by the parser.
	 * 
	 * @param expression
	 * @param ast
	 */
	SpelExpression(String expression, SpelNode ast) {
		this.expression = expression;
		this.ast = ast;
	}

	/**
	 * @return the expression string that was parsed to create this expression instance
	 */
	public String getExpressionString() {
		return expression;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getValue() throws EvaluationException {
		EvaluationContext eContext = new StandardEvaluationContext();
		return ast.getValue(new ExpressionState(eContext));
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getValue(EvaluationContext context) throws EvaluationException {
		return ast.getValue(new ExpressionState(context));
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getValue(EvaluationContext context, Class<?> expectedResultType) throws EvaluationException {
		Object result = ast.getValue(new ExpressionState(context));

		if (result != null && expectedResultType != null) {
			Class<?> resultType = result.getClass();
			if (expectedResultType.isAssignableFrom(resultType)) {
				return result;
			}
			// Attempt conversion to the requested type, may throw an exception
			return context.getTypeUtils().getTypeConverter().convertValue(result, expectedResultType);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		ast.setValue(new ExpressionState(context), value);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return ast.isWritable(new ExpressionState(context));
	}

	/**
	 * @return return the Abstract Syntax Tree for the expression
	 */
	public SpelNode getAST() {
		return ast;
	}

	/**
	 * Produce a string representation of the Abstract Syntax Tree for the expression, this should ideally look like the
	 * input expression, but properly formatted since any unnecessary whitespace will have been discarded during the
	 * parse of the expression.
	 * 
	 * @return the string representation of the AST
	 */
	public String toStringAST() {
		return ast.toStringAST();
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getValueType(EvaluationContext context) throws EvaluationException {
		// TODO is this a legal implementation? The null return value could be very unhelpful. See other getValueType()
		// also.
		Object value = getValue(context);
		if (value == null) {
			return null;
		} else {
			return value.getClass();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getValueType() throws EvaluationException {
		Object value = getValue();
		if (value == null) {
			return null;
		} else {
			return value.getClass();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getValue(Class<?> expectedResultType) throws EvaluationException {
		Object result = getValue();
		return ExpressionUtils.convert(null, result, expectedResultType);
	}

}
