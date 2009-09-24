/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;

import org.springframework.util.Assert;

/**
 * Convenience methods for accessing JSP 2.0's
 * {@link javax.servlet.jsp.el.ExpressionEvaluator}.
 *
 * <p>The evaluation methods check if the value contains "${" before
 * invoking the EL evaluator, treating the value as "normal" expression
 * (i.e. a literal String value) else.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 11.07.2003
 * @see javax.servlet.jsp.el.ExpressionEvaluator#evaluate
 */
public abstract class ExpressionEvaluationUtils {

	public static final String EXPRESSION_PREFIX = "${";

	public static final String EXPRESSION_SUFFIX = "}";


	/**
	 * Check if the given expression value is an EL expression.
	 * @param value the expression to check
	 * @return <code>true</code> if the expression is an EL expression,
	 * <code>false</code> otherwise
	 */
	public static boolean isExpressionLanguage(String value) {
		return (value != null && value.contains(EXPRESSION_PREFIX));
	}

	/**
	 * Evaluate the given expression (be it EL or a literal String value)
	 * to an Object of a given type,
	 * @param attrName name of the attribute (typically a JSP tag attribute)
	 * @param attrValue value of the attribute
	 * @param resultClass class that the result should have (String, Integer, Boolean)
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors, also in case of type mismatch
	 * if the passed-in literal value is not an EL expression and not assignable to
	 * the result class
	 */
	public static Object evaluate(String attrName, String attrValue, Class resultClass, PageContext pageContext)
			throws JspException {

		if (isExpressionLanguage(attrValue)) {
			return doEvaluate(attrName, attrValue, resultClass, pageContext);
		}
		else if (attrValue != null && resultClass != null && !resultClass.isInstance(attrValue)) {
			throw new JspException("Attribute value \"" + attrValue + "\" is neither a JSP EL expression nor " +
					"assignable to result class [" + resultClass.getName() + "]");
		}
		else {
			return attrValue;
		}
	}

	/**
	 * Evaluate the given expression (be it EL or a literal String value) to an Object.
	 * @param attrName name of the attribute (typically a JSP tag attribute)
	 * @param attrValue value of the attribute
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors
	 */
	public static Object evaluate(String attrName, String attrValue, PageContext pageContext)
			throws JspException {

		if (isExpressionLanguage(attrValue)) {
			return doEvaluate(attrName, attrValue, Object.class, pageContext);
		}
		else {
			return attrValue;
		}
	}

	/**
	 * Evaluate the given expression (be it EL or a literal String value) to a String.
	 * @param attrName name of the attribute (typically a JSP tag attribute)
	 * @param attrValue value of the attribute
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors
	 */
	public static String evaluateString(String attrName, String attrValue, PageContext pageContext)
			throws JspException {

		if (isExpressionLanguage(attrValue)) {
			return (String) doEvaluate(attrName, attrValue, String.class, pageContext);
		}
		else {
			return attrValue;
		}
	}

	/**
	 * Evaluate the given expression (be it EL or a literal String value) to an integer.
	 * @param attrName name of the attribute (typically a JSP tag attribute)
	 * @param attrValue value of the attribute
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors
	 */
	public static int evaluateInteger(String attrName, String attrValue, PageContext pageContext)
			throws JspException {

		if (isExpressionLanguage(attrValue)) {
			return (Integer) doEvaluate(attrName, attrValue, Integer.class, pageContext);
		}
		else {
			return Integer.parseInt(attrValue);
		}
	}

	/**
	 * Evaluate the given expression (be it EL or a literal String value) to a boolean.
	 * @param attrName name of the attribute (typically a JSP tag attribute)
	 * @param attrValue value of the attribute
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors
	 */
	public static boolean evaluateBoolean(String attrName, String attrValue, PageContext pageContext)
			throws JspException {

		if (isExpressionLanguage(attrValue)) {
			return (Boolean) doEvaluate(attrName, attrValue, Boolean.class, pageContext);
		}
		else {
			return Boolean.valueOf(attrValue);
		}
	}


	/**
	 * Actually evaluate the given expression (be it EL or a literal String value)
	 * to an Object of a given type. Supports concatenated expressions,
	 * for example: "${var1}text${var2}"
	 * @param attrName name of the attribute
	 * @param attrValue value of the attribute
	 * @param resultClass class that the result should have
	 * @param pageContext current JSP PageContext
	 * @return the result of the evaluation
	 * @throws JspException in case of parsing errors
	 */
	private static Object doEvaluate(String attrName, String attrValue, Class resultClass, PageContext pageContext)
			throws JspException {

		Assert.notNull(attrValue, "Attribute value must not be null");
		Assert.notNull(resultClass, "Result class must not be null");
		Assert.notNull(pageContext, "PageContext must not be null");

		try {
			if (resultClass.isAssignableFrom(String.class)) {
				StringBuilder resultValue = null;
				int exprPrefixIndex;
				int exprSuffixIndex = 0;
				do {
					exprPrefixIndex = attrValue.indexOf(EXPRESSION_PREFIX, exprSuffixIndex);
					if (exprPrefixIndex != -1) {
						int prevExprSuffixIndex = exprSuffixIndex;
						exprSuffixIndex = attrValue.indexOf(EXPRESSION_SUFFIX, exprPrefixIndex + EXPRESSION_PREFIX.length());
						String expr;
						if (exprSuffixIndex != -1) {
							exprSuffixIndex += EXPRESSION_SUFFIX.length();
							expr = attrValue.substring(exprPrefixIndex, exprSuffixIndex);
						}
						else {
							expr = attrValue.substring(exprPrefixIndex);
						}
						if (expr.length() == attrValue.length()) {
							// A single expression without static prefix or suffix ->
							// parse it with the specified result class rather than String.
							return evaluateExpression(attrValue, resultClass, pageContext);
						}
						else {
							// We actually need to concatenate partial expressions into a String.
							if (resultValue == null) {
								resultValue = new StringBuilder();
							}
							resultValue.append(attrValue.substring(prevExprSuffixIndex, exprPrefixIndex));
							resultValue.append(evaluateExpression(expr, String.class, pageContext));
						}
					}
					else {
						if (resultValue == null) {
							resultValue = new StringBuilder();
						}
						resultValue.append(attrValue.substring(exprSuffixIndex));
					}
				}
				while (exprPrefixIndex != -1 && exprSuffixIndex != -1);
				return resultValue.toString();
			}
			else {
				return evaluateExpression(attrValue, resultClass, pageContext);
			}
		}
		catch (ELException ex) {
			throw new JspException("Parsing of JSP EL expression failed for attribute '" + attrName + "'", ex);
		}
	}

	private static Object evaluateExpression(String exprValue, Class resultClass, PageContext pageContext)
			throws ELException {

		return pageContext.getExpressionEvaluator().evaluate(
				exprValue, resultClass, pageContext.getVariableResolver(), null);
	}

}
