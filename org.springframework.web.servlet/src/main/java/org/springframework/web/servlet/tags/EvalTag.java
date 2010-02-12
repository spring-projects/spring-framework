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

package org.springframework.web.servlet.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.beans.BeansException;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.web.util.ExpressionEvaluationUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * JSP tag for evaluating expressions with the Spring Expression Language (SpEL).
 * Supports the standard JSP evaluation context consisting of implicit variables and scoped attributes.
 * 
 * @author Keith Donald
 * @since 3.0.1
 */
public class EvalTag extends HtmlEscapingAwareTag {

	private ExpressionParser expressionParser;
	
	private String expression;

	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;

	/**
	 * Set the expression to evaluate.
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * Set the variable name to expose the evaluation result under.
	 * Defaults to rendering the result to the current JspWriter
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * Set the scope to export the evaluation result to.
	 * This attribute has no meaning unless var is also defined.
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * Set JavaScript escaping for this tag, as boolean value.
	 * Default is "false".
	 */
	public void setJavaScriptEscape(String javaScriptEscape) throws JspException {
		this.javaScriptEscape =
				ExpressionEvaluationUtils.evaluateBoolean("javaScriptEscape", javaScriptEscape, this.pageContext);
	}

	@Override
	public int doStartTagInternal() throws JspException {
		this.expressionParser = new SpelExpressionParser();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		Expression expression = this.expressionParser.parseExpression(this.expression);
		EvaluationContext context = createEvaluationContext();
		if (this.var == null) {
			try {
				String result = expression.getValue(context, String.class);
				result = isHtmlEscape() ? HtmlUtils.htmlEscape(result) : result;
				result = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result;
				pageContext.getOut().print(result);
			}
			catch (IOException e) {
				throw new JspException(e);
			}
		}
		else {
			pageContext.setAttribute(var, expression.getValue(context), scope);
		}
		return EVAL_PAGE;
	}
	
	private EvaluationContext createEvaluationContext() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new JspPropertyAccessor(this.pageContext));
		ConversionService conversionService = getConversionService();
		if (conversionService != null) {
			context.setTypeConverter(new StandardTypeConverter());
		}
		return context;
	}
	
	private ConversionService getConversionService() {
		try {
			return (ConversionService) this.pageContext.getRequest().getAttribute("org.springframework.core.convert.ConversionService");
		} catch (BeansException e) {
			return null;
		}
	}
	
	private static class JspPropertyAccessor implements PropertyAccessor {

		private PageContext pageContext;
		
		public JspPropertyAccessor(PageContext pageContext) {
			this.pageContext = pageContext;
		}
		
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		public boolean canRead(EvaluationContext context, Object target,
				String name) throws AccessException {
			if (name.equals("pageContext")) {
				return true;
			}
			// TODO support all other JSP implicit variables defined at http://java.sun.com/javaee/6/docs/api/javax/servlet/jsp/el/ImplicitObjectELResolver.html
			return this.pageContext.findAttribute(name) != null;
		}

		public TypedValue read(EvaluationContext context, Object target,
				String name) throws AccessException {
			if (name.equals("pageContext")) {
				return new TypedValue(this.pageContext);
			}
			// TODO support all other JSP implicit variables defined at http://java.sun.com/javaee/6/docs/api/javax/servlet/jsp/el/ImplicitObjectELResolver.html
			return new TypedValue(this.pageContext.findAttribute(name));
		}

		public boolean canWrite(EvaluationContext context, Object target,
				String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target,
				String name, Object newValue) throws AccessException {
			throw new UnsupportedOperationException();
		}
		
	}

}