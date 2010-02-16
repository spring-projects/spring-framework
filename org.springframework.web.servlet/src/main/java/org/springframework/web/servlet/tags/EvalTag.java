/*
 * Copyright 2002-2010 the original author or authors.
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

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private EvaluationContext evaluationContext;

	private Expression expression;

	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	@Override
	public void setPageContext(PageContext pageContext) {
		super.setPageContext(pageContext);
		this.evaluationContext = createEvaluationContext(pageContext);
	}

	/**
	 * Set the expression to evaluate.
	 */
	public void setExpression(String expression) {
		this.expression = this.expressionParser.parseExpression(expression);
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
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		if (this.var == null) {
			try {
				String result = this.expression.getValue(this.evaluationContext, String.class);
				result = isHtmlEscape() ? HtmlUtils.htmlEscape(result) : result;
				result = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result;
				pageContext.getOut().print(result);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		else {
			Object result = this.expression.getValue(this.evaluationContext);
			pageContext.setAttribute(this.var, result, this.scope);
		}
		return EVAL_PAGE;
	}


	private EvaluationContext createEvaluationContext(PageContext pageContext) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new JspPropertyAccessor(pageContext));
		ConversionService conversionService = getConversionService(pageContext);
		if (conversionService != null) {
			context.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		return context;
	}
	
	private ConversionService getConversionService(PageContext pageContext) {
		return (ConversionService) pageContext.getRequest().getAttribute(ConversionService.class.getName());
	}


	private static class JspPropertyAccessor implements PropertyAccessor {

		private final PageContext pageContext;
		
		public JspPropertyAccessor(PageContext pageContext) {
			this.pageContext = pageContext;
		}
		
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (resolveImplicitVariable(name) != null || this.pageContext.findAttribute(name) != null);
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Object implicitVar = resolveImplicitVariable(name);
			if (implicitVar != null) {
				return new TypedValue(implicitVar);
			}
			return new TypedValue(this.pageContext.findAttribute(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue) {
			throw new UnsupportedOperationException();
		}
		
		private Object resolveImplicitVariable(String name) throws AccessException {
			try {
				return this.pageContext.getVariableResolver().resolveVariable(name);
			}
			catch (Exception ex) {
				throw new AccessException(
						"Unexpected exception occurred accessing '" + name + "' as an implicit variable", ex);
			}
		}
	}

}
