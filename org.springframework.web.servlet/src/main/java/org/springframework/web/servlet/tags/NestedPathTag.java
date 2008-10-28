/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.springframework.beans.PropertyAccessor;
import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * <p>Nested-path tag, to support and assist with nested beans or bean properties
 * in the model. Exports a "nestedPath" variable of type String in request scope,
 * visible to the current page and also included pages, if any.
 *
 * <p>The BindTag will auto-detect the current nested path and automatically
 * prepend it to its own path to form a complete path to the bean or bean property.
 *
 * <p>This tag will also prepend any existing nested path that is currently set.
 * Thus, you can nest multiple nested-path tags.
 *
 * <p>Thanks to Seth Ladd for the suggestion and the original implementation!
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public class NestedPathTag extends TagSupport implements TryCatchFinally {

	/**
	 * Name of the exposed variable within the scope of this tag: "nestedPath".
	 */
	public static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";


	private String path;

	/** Caching a previous nested path, so that it may be reset */
	private String previousNestedPath;


	/**
	 * Set the path that this tag should apply.
	 * <p>E.g. "customer" to allow bind paths like "address.street"
	 * rather than "customer.address.street".
	 * @see BindTag#setPath
	 */
	public void setPath(String path) {
		if (path == null) {
			path = "";
		}
		if (path.length() > 0 && !path.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
			path += PropertyAccessor.NESTED_PROPERTY_SEPARATOR;
		}
		this.path = path;
	}

	/**
	 * Return the path that this tag applies to.
	 */
	public String getPath() {
		return this.path;
	}


	@Override
	public int doStartTag() throws JspException {
		String resolvedPath = ExpressionEvaluationUtils.evaluateString("path", getPath(), pageContext);

		// Save previous nestedPath value, build and expose current nestedPath value.
		// Use request scope to expose nestedPath to included pages too.
		this.previousNestedPath =
				(String) pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		String nestedPath =
				(this.previousNestedPath != null ? this.previousNestedPath + resolvedPath : resolvedPath);
		pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, nestedPath, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Reset any previous nestedPath value.
	 */
	@Override
	public int doEndTag() {
		if (this.previousNestedPath != null) {
			// Expose previous nestedPath value.
			pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// Remove exposed nestedPath value.
			pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}

		return EVAL_PAGE;
	}

	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	public void doFinally() {
		this.previousNestedPath = null;
	}

}
