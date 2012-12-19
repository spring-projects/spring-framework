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

package org.springframework.web.servlet.tags;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.validation.Errors;
import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * JSP tag that evaluates content if there are binding errors
 * for a certain bean. Exports an "errors" variable of type
 * {@link org.springframework.validation.Errors} for the given bean.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BindTag
 * @see org.springframework.validation.Errors
 */
@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

	public static final String ERRORS_VARIABLE_NAME = "errors";


	private String name;

	private Errors errors;


	/**
	 * Set the name of the bean that this tag should check.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the bean that this tag checks.
	 */
	public String getName() {
		return this.name;
	}


	@Override
	protected final int doStartTagInternal() throws ServletException, JspException {
		String resolvedName = ExpressionEvaluationUtils.evaluateString("name", this.name, pageContext);
		this.errors = getRequestContext().getErrors(resolvedName, isHtmlEscape());

		if (this.errors != null && this.errors.hasErrors()) {
			this.pageContext.setAttribute(ERRORS_VARIABLE_NAME, this.errors, PageContext.REQUEST_SCOPE);
			return EVAL_BODY_INCLUDE;
		}
		else {
			return SKIP_BODY;
		}
	}

	@Override
	public int doEndTag() {
		this.pageContext.removeAttribute(ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		return EVAL_PAGE;
	}

	/**
	 * Retrieve the Errors instance that this tag is currently bound to.
	 * <p>Intended for cooperating nesting tags.
	 */
	public final Errors getErrors() {
		return this.errors;
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.errors = null;
	}

}
