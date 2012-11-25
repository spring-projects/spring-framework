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

package org.springframework.web.util;

import javax.servlet.ServletContext;
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
 * <p><b>See {@link #isSpringJspExpressionSupportActive} for guidelines
 * on when to use Spring's JSP expression support as opposed to the
 * built-in expression support in JSP 2.0+ containers.</b>
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 11.07.2003
 * @see javax.servlet.jsp.el.ExpressionEvaluator#evaluate
 * @deprecated as of Spring 3.2, in favor of the JSP 2.0+ native support
 * for embedded expressions in JSP pages (also applying to tag attributes)
 */
@Deprecated
public abstract class ExpressionEvaluationUtils {

	/**
	 * Expression support parameter at the servlet context level
	 * (i.e. a context-param in <code>web.xml</code>): "springJspExpressionSupport".
	 */
	public static final String EXPRESSION_SUPPORT_CONTEXT_PARAM = "springJspExpressionSupport";

	public static final String EXPRESSION_PREFIX = "${";

	public static final String EXPRESSION_SUFFIX = "}";


	/**
	 * Check whether Spring's JSP expression support is actually active.
	 * <p>Note that JSP 2.0+ containers come with expression support themselves:
	 * However, it will only be active for web applications declaring Servlet 2.4
	 * or higher in their <code>web.xml</code> deployment descriptor.
	 * <p>If a <code>web.xml</code> context-param named "springJspExpressionSupport" is
	 * found, its boolean value will be taken to decide whether this support is active.
	 * If not found, the default is for expression support to be inactive on Servlet 3.0
	 * containers with web applications declaring Servlet 2.4 or higher in their
	 * <code>web.xml</code>. For backwards compatibility, Spring's expression support
	 * will remain active for applications declaring Servlet 2.3 or earlier. However,
	 * on Servlet 2.4/2.5 containers, we can't find out what the application has declared;
	 * as of Spring 3.2, we won't activate Spring's expression support at all then since
	 * it got deprecated and will be removed in the next iteration of the framework.
	 * @param pageContext current JSP PageContext
	 * @return <code>true</code> if active (ExpressionEvaluationUtils will actually evaluate expressions);
	 * <code>false</code> if not active (ExpressionEvaluationUtils will return given values as-is,
	 * relying on the JSP container pre-evaluating values before passing them to JSP tag attributes)
	 */
	public static boolean isSpringJspExpressionSupportActive(PageContext pageContext) {
		ServletContext sc = pageContext.getServletContext();
		String springJspExpressionSupport = sc.getInitParameter(EXPRESSION_SUPPORT_CONTEXT_PARAM);
		if (springJspExpressionSupport != null) {
			return Boolean.valueOf(springJspExpressionSupport);
		}
		if (sc.getMajorVersion() >= 3) {
			// We're on a Servlet 3.0+ container: Let's check what the application declares...
			if (sc.getEffectiveMajorVersion() == 2 && sc.getEffectiveMinorVersion() < 4) {
				// Application declares Servlet 2.3- in its web.xml: JSP 2.0 expressions not active.
				// Activate our own expression support.
				return true;
			}
		}
		return false;
	}

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

		if (isSpringJspExpressionSupportActive(pageContext) && isExpressionLanguage(attrValue)) {
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

		if (isSpringJspExpressionSupportActive(pageContext) && isExpressionLanguage(attrValue)) {
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

		if (isSpringJspExpressionSupportActive(pageContext) && isExpressionLanguage(attrValue)) {
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

		if (isSpringJspExpressionSupportActive(pageContext) && isExpressionLanguage(attrValue)) {
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

		if (isSpringJspExpressionSupportActive(pageContext) && isExpressionLanguage(attrValue)) {
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
