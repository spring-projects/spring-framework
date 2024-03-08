/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.tags;

import java.io.IOException;

import jakarta.el.ELContext;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
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
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * The {@code <eval>} tag evaluates a Spring expression (SpEL) and either prints
 * the result or assigns it to a variable. Supports the standard JSP evaluation
 * context consisting of implicit variables and scoped attributes.
 *
 * <table>
 * <caption>Attribute Summary</caption>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Required?</th>
 * <th>Runtime Expression?</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>expression</td>
 * <td>true</td>
 * <td>true</td>
 * <td>The expression to evaluate.</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set HTML escaping for this tag, as a boolean value.
 * Overrides the default HTML escaping setting for the current page.</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set JavaScript escaping for this tag, as a boolean value.
 * Default is {@code false}.</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The scope for the var. 'application', 'session', 'request' and 'page'
 * scopes are supported. Defaults to page scope. This attribute has no effect
 * unless the var attribute is also defined.</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The name of the variable to export the evaluation result to.
 * If not specified the evaluation result is converted to a String and written
 * as output.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0.1
 */
@SuppressWarnings("serial")
public class EvalTag extends HtmlEscapingAwareTag {

	/**
	 * {@link jakarta.servlet.jsp.PageContext} attribute for the
	 * page-level {@link EvaluationContext} instance.
	 */
	private static final String EVALUATION_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.EVALUATION_CONTEXT";


	private final ExpressionParser expressionParser = new SpelExpressionParser();

	@Nullable
	private Expression expression;

	@Nullable
	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * Set the expression to evaluate.
	 */
	public void setExpression(String expression) {
		this.expression = this.expressionParser.parseExpression(expression);
	}

	/**
	 * Set the variable name to expose the evaluation result under.
	 * Defaults to rendering the result to the current JspWriter.
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
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	public int doStartTagInternal() throws JspException {
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		EvaluationContext evaluationContext =
				(EvaluationContext) this.pageContext.getAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE);
		if (evaluationContext == null) {
			evaluationContext = createEvaluationContext(this.pageContext);
			this.pageContext.setAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE, evaluationContext);
		}
		if (this.var != null) {
			Object result = (this.expression != null ? this.expression.getValue(evaluationContext) : null);
			this.pageContext.setAttribute(this.var, result, this.scope);
		}
		else {
			try {
				String result = (this.expression != null ?
						this.expression.getValue(evaluationContext, String.class) : null);
				result = ObjectUtils.getDisplayString(result);
				result = htmlEscape(result);
				result = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result);
				this.pageContext.getOut().print(result);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		return EVAL_PAGE;
	}

	private EvaluationContext createEvaluationContext(PageContext pageContext) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new JspPropertyAccessor(pageContext));
		context.addPropertyAccessor(new MapAccessor());
		context.addPropertyAccessor(new EnvironmentAccessor());
		context.setBeanResolver(new BeanFactoryResolver(getRequestContext().getWebApplicationContext()));
		ConversionService conversionService = getConversionService(pageContext);
		if (conversionService != null) {
			context.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		return context;
	}

	@Nullable
	private ConversionService getConversionService(PageContext pageContext) {
		return (ConversionService) pageContext.getRequest().getAttribute(ConversionService.class.getName());
	}


	@SuppressWarnings("deprecation")
	private static class JspPropertyAccessor implements PropertyAccessor {

		private final PageContext pageContext;

		@Nullable
		private final ELContext elContext;


		public JspPropertyAccessor(PageContext pageContext) {
			this.pageContext = pageContext;
			this.elContext = pageContext.getELContext();
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			return (target == null &&
					(resolveImplicitVariable(name) != null || this.pageContext.findAttribute(name) != null));
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			Object implicitVar = resolveImplicitVariable(name);
			if (implicitVar != null) {
				return new TypedValue(implicitVar);
			}
			return new TypedValue(this.pageContext.findAttribute(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			throw new UnsupportedOperationException();
		}

		@Nullable
		private Object resolveImplicitVariable(String name) throws AccessException {
			if (this.elContext == null) {
				return null;
			}
			try {
				return this.elContext.getELResolver().getValue(this.elContext, name, null);
			}
			catch (Exception ex) {
				throw new AccessException(
						"Unexpected exception occurred accessing '" + name + "' as an implicit variable", ex);
			}
		}
	}

}
