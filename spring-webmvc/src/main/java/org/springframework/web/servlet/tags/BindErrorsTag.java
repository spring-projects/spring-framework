/*
 * Copyright 2002-present the original author or authors.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import org.jspecify.annotations.Nullable;

import org.springframework.validation.Errors;

/**
 * This {@code <hasBindErrors>} tag provides an {@link Errors} instance in case of
 * bind errors. The HTML escaping flag participates in a page-wide or
 * application-wide setting (i.e. by HtmlEscapeTag or a "defaultHtmlEscape"
 * context-param in web.xml).
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
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set HTML escaping for this tag, as boolean value.
 * Overrides the default HTML escaping setting for the current page.</td>
 * </tr>
 * <tr>
 * <td>name</td>
 * <td>true</td>
 * <td>true</td>
 * <td>The name of the bean in the request that needs to be inspected for errors.
 * If errors are available for this bean, they will be bound under the
 * 'errors' key.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BindTag
 * @see org.springframework.validation.Errors
 */
@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

	/**
	 * Page context attribute containing {@link Errors}.
	 */
	public static final String ERRORS_VARIABLE_NAME = "errors";


	private String name = "";

	private @Nullable Errors errors;


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
		this.errors = getRequestContext().getErrors(this.name, isHtmlEscape());
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
	public final @Nullable Errors getErrors() {
		return this.errors;
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.errors = null;
	}

}
