/*
 * Copyright 2002-2018 the original author or authors.
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

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.TagSupport;
import jakarta.servlet.jsp.tagext.TryCatchFinally;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * <p>The {@code <nestedPath>} tag supports and assists with nested beans or
 * bean properties in the model. Exports a "nestedPath" variable of type String
 * in request scope, visible to the current page and also included pages, if any.
 *
 * <p>The BindTag will auto-detect the current nested path and automatically
 * prepend it to its own path to form a complete path to the bean or bean property.
 *
 * <p>This tag will also prepend any existing nested path that is currently set.
 * Thus, you can nest multiple nested-path tags.
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
 * <td>path</td>
 * <td>true</td>
 * <td>true</td>
 * <td>Set the path that this tag should apply. E.g. 'customer' to allow bind
 * paths like 'address.street' rather than 'customer.address.street'.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class NestedPathTag extends TagSupport implements TryCatchFinally {

	/**
	 * Name of the exposed variable within the scope of this tag: "nestedPath".
	 */
	public static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";


	@Nullable
	private String path;

	/** Caching a previous nested path, so that it may be reset. */
	@Nullable
	private String previousNestedPath;


	/**
	 * Set the path that this tag should apply.
	 * <p>E.g. "customer" to allow bind paths like "address.street"
	 * rather than "customer.address.street".
	 * @see BindTag#setPath
	 */
	public void setPath(@Nullable String path) {
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
	@Nullable
	public String getPath() {
		return this.path;
	}


	@Override
	public int doStartTag() throws JspException {
		// Save previous nestedPath value, build and expose current nestedPath value.
		// Use request scope to expose nestedPath to included pages too.
		this.previousNestedPath =
				(String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		String nestedPath =
				(this.previousNestedPath != null ? this.previousNestedPath + getPath() : getPath());
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, nestedPath, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Reset any previous nestedPath value.
	 */
	@Override
	public int doEndTag() {
		if (this.previousNestedPath != null) {
			// Expose previous nestedPath value.
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// Remove exposed nestedPath value.
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}

		return EVAL_PAGE;
	}

	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.previousNestedPath = null;
	}

}
