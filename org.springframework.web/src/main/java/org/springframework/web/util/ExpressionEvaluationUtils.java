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

package org.springframework.web.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.Expression;

import org.springframework.util.Assert;

/**
 * Convenience methods for accessing JSP 2.0's
 * {@link javax.servlet.jsp.el.ExpressionEvaluator}.
 *
 * <p>This class will by default use standard <code>evaluate</code> calls.
 * If your application server happens to be inefficient in that respect,
 * consider setting Spring's "cacheJspExpressions" context-param in
 * <code>web.xml</code> to "true", which will use <code>parseExpression</code>
 * calls with cached Expression objects instead.
 *
 * <p>The evaluation methods check if the value contains "${" before
 * invoking the EL evaluator, treating the value as "normal" expression
 * (i.e. a literal String value) else.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 11.07.2003
 * @see javax.servlet.jsp.el.ExpressionEvaluator#evaluate
 * @see javax.servlet.jsp.el.ExpressionEvaluator#parseExpression
 */
public abstract class ExpressionEvaluationUtils {

	/**
	 * JSP 2.0 expression cache parameter at the servlet context level
	 * (i.e. a context-param in <code>web.xml</code>): "cacheJspExpressions".
	 */
	public static final String EXPRESSION_CACHE_CONTEXT_PARAM = "cacheJspExpressions";

	public static final String EXPRESSION_PREFIX = "${";

	public static final String EXPRESSION_SUFFIX = "}";


	private static final String EXPRESSION_CACHE_FLAG_CONTEXT_ATTR =
			ExpressionEvaluationUtils.class.getName() + ".CACHE_JSP_EXPRESSIONS";

	private static final String EXPRESSION_CACHE_MAP_CONTEXT_ATTR =
			ExpressionEvaluationUtils.class.getName() + ".JSP_EXPRESSION_CACHE";


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
				int exprPrefixIndex = -1;
				int exprSuffixIndex = 0;
				do {
					exprPrefixIndex = attrValue.indexOf(EXPRESSION_PREFIX, exprSuffixIndex);
					if (exprPrefixIndex != -1) {
						int prevExprSuffixIndex = exprSuffixIndex;
						exprSuffixIndex = attrValue.indexOf(EXPRESSION_SUFFIX, exprPrefixIndex + EXPRESSION_PREFIX.length());
						String expr = null;
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

		Map<ExpressionCacheKey, Expression> expressionCache = getJspExpressionCache(pageContext);
		if (expressionCache != null) {
			// We are supposed to explicitly create and cache JSP Expression objects.
			ExpressionCacheKey cacheKey = new ExpressionCacheKey(exprValue, resultClass);
			Expression expr = expressionCache.get(cacheKey);
			if (expr == null) {
				expr = pageContext.getExpressionEvaluator().parseExpression(exprValue, resultClass, null);
				expressionCache.put(cacheKey, expr);
			}
			return expr.evaluate(pageContext.getVariableResolver());
		}
		else {
			// We're simply calling the JSP 2.0 evaluate method straight away.
			return pageContext.getExpressionEvaluator().evaluate(
					exprValue, resultClass, pageContext.getVariableResolver(), null);
		}
	}

	/**
	 * Determine whether JSP 2.0 expressions are supposed to be cached
	 * and return the corresponding cache Map, or <code>null</code> if
	 * caching is not enabled.
	 * @param pageContext current JSP PageContext
	 * @return the cache Map, or <code>null</code> if caching is disabled
	 */
	@SuppressWarnings("unchecked")
	private static Map<ExpressionCacheKey, Expression> getJspExpressionCache(PageContext pageContext) {
		ServletContext servletContext = pageContext.getServletContext();
		Map<ExpressionCacheKey, Expression> cacheMap =
				(Map<ExpressionCacheKey, Expression>) servletContext.getAttribute(EXPRESSION_CACHE_MAP_CONTEXT_ATTR);
		if (cacheMap == null) {
			Boolean cacheFlag = (Boolean) servletContext.getAttribute(EXPRESSION_CACHE_FLAG_CONTEXT_ATTR);
			if (cacheFlag == null) {
				cacheFlag = Boolean.valueOf(servletContext.getInitParameter(EXPRESSION_CACHE_CONTEXT_PARAM));
				servletContext.setAttribute(EXPRESSION_CACHE_FLAG_CONTEXT_ATTR, cacheFlag);
			}
			if (cacheFlag) {
				cacheMap = Collections.synchronizedMap(new HashMap<ExpressionCacheKey, Expression>());
				servletContext.setAttribute(EXPRESSION_CACHE_MAP_CONTEXT_ATTR, cacheMap);
			}
		}
		return cacheMap;
	}


	/**
	 * Cache key class for JSP 2.0 Expression objects.
	 */
	private static class ExpressionCacheKey {

		private final String value;
		private final Class resultClass;
		private final int hashCode;

		public ExpressionCacheKey(String value, Class resultClass) {
			this.value = value;
			this.resultClass = resultClass;
			this.hashCode = this.value.hashCode() * 29 + this.resultClass.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ExpressionCacheKey)) {
				return false;
			}
			ExpressionCacheKey other = (ExpressionCacheKey) obj;
			return (this.value.equals(other.value) && this.resultClass.equals(other.resultClass));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}
	}

}
