/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.expression;

import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelExpressionParser;
import org.springframework.expression.spel.standard.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * Standard implementation of the {@link BeanExpressionResolver} interface,
 * parsing and evaluating Spring EL using Spring's expression module.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.SpelExpressionParser
 * @see org.springframework.expression.spel.standard.StandardEvaluationContext
 */
public class StandardBeanExpressionResolver extends AbstractBeanExpressionResolver {

	private ExpressionParser expressionParser = new SpelExpressionParser();


	/**
	 * Specify the EL parser to use for expression parsing.
	 * <p>Default is a {@link org.springframework.expression.spel.SpelExpressionParser},
	 * compatible with standard Unified EL style expression syntax.
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	protected Object evaluateExpression(String exprString, BeanExpressionContext evalContext) {
		try {
			Expression expr = this.expressionParser.parseExpression(exprString);
			StandardEvaluationContext ec = new StandardEvaluationContext(evalContext);
			ec.addPropertyAccessor(new BeanExpressionContextAccessor());
			ec.addPropertyAccessor(new BeanFactoryAccessor());
			ec.addPropertyAccessor(new MapAccessor());
			return expr.getValue(ec);
		}
		catch (Exception ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * Template method for customizing the expression evaluation context.
	 * <p>The default implementation is empty.
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
