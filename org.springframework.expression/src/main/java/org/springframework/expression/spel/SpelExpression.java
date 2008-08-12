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
import org.springframework.expression.spel.ast.SpelNode;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

// TODO 3 Do we need more getValue() options - for example with just a root object or with a set of variables?
// (these things are currently captured in the Context)

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
	 * Evaluate the expression in a default context that knows nothing (and therefore cannot resolve references to
	 * properties/etc). Only useful for trivial expressions like '3+4'.
	 * 
	 * @return the value of the expression
	 * @throws SpelException if there is a problem with evaluation of the expression
	 */
	public Object getValue() throws EvaluationException {
		EvaluationContext eContext = new StandardEvaluationContext();
		return ast.getValue(new ExpressionState(eContext));
	}

	// public Class<?> getValueType() throws ELException {
	// return ast.getValueType(new ExpressionState());
	// }

	/**
	 * Evaluate the expression in a specified context which can resolve references to properties, methods, types, etc.
	 * The {@link StandardEvaluationContext} can be sub classed and overridden where necessary, rather than implementing
	 * the entire EvaluationContext interface.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @return the value of the expression
	 * @throws SpelException if there is a problem with evaluation of the expression.
	 */
	public Object getValue(EvaluationContext context) throws EvaluationException {
		return ast.getValue(new ExpressionState(context));
	}

	/**
	 * Evaluate the expression in a specified context which can resolve references to properties, methods, types, etc -
	 * the type of the evaluation result is expected to be of a particular class and an exception will be thrown if it
	 * is not and cannot be converted to that type. The {@link StandardEvaluationContext} can be sub classed and
	 * overridden where necessary, rather than implementing the entire EvaluationContext interface.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @param expectedResultType the class the caller would like the result to be
	 * @return the value of the expression
	 * @throws SpelException if there is a problem with evaluation of the expression.
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
	 * Evaluate an expression and set the result to the specified value. This only makes sense when working with the
	 * expression against a context.
	 * 
	 * @param context the context in which to evaluate the expression
	 * @param value the new value
	 * @throws SpelException if there is a problem with evaluation of the expression.
	 */
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		ast.setValue(new ExpressionState(context), value);
	}

	/**
	 * Determine if an expression evaluates to an value that can be set with a value (for example, a property).
	 * 
	 * @param context the context in which to evaluate the expression
	 * @return true if the expression supports setValue()
	 * @throws SpelException if there is a problem with evaluation of the expression.
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
	
	public Class getValueType(EvaluationContext context) throws EvaluationException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public Object getValue(Class<?> expectedResultType) throws EvaluationException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
