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

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.util.Assert;

/**
 * Abstract implementation of the {@link BeanExpressionResolver} interface.
 * Handles the common mixing of expression parts with literal parts.
 *
 * <p>Subclasses need to implement the {@link #evaluateExpression} template
 * method for actual expression evaluation.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see #setExpressionPrefix
 * @see #setExpressionSuffix
 */
public abstract class AbstractBeanExpressionResolver implements BeanExpressionResolver {

	/** Default expression prefix: "#{" */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** Default expression suffix: "}" */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;


	/**
	 * Set the prefix that an expression string starts with.
	 * The default is "#{".
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * Set the suffix that an expression string ends with.
	 * The default is "}".
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}


	public Object evaluate(String value, BeanExpressionContext evalContext) {
		if (value == null) {
			return null;
		}
		Object result = "";
		int prefixIndex = value.indexOf(this.expressionPrefix);
		int endIndex = 0;
		while (prefixIndex != -1) {
			int exprStart = prefixIndex + this.expressionPrefix.length();
			int suffixIndex = value.indexOf(this.expressionSuffix, exprStart);
			if (suffixIndex != -1) {
				if (prefixIndex > 0) {
					result = result + value.substring(endIndex, prefixIndex);
				}
				endIndex = suffixIndex + this.expressionSuffix.length();
				String expr = value.substring(exprStart, suffixIndex);
				Object exprResult = evaluateExpression(expr, evalContext);
				if (result != null && !"".equals(result)) {
					result = result.toString() + exprResult.toString();
				}
				else {
					result = exprResult;
				}
				prefixIndex = value.indexOf(this.expressionPrefix, suffixIndex);
			}
			else {
				prefixIndex = -1;
			}
		}
		if (endIndex < value.length()) {
			return result + value.substring(endIndex);
		}
		else {
			return result;
		}
	}

	/**
	 * Evaluate the given expression.
	 * @param exprString the expression String to evaluate
	 * @param evalContext the context to evaluate the expression within
	 * @return the evaluation result
	 */
	protected abstract Object evaluateExpression(String exprString, BeanExpressionContext evalContext);

}
