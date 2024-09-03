/*
 * Copyright 2002-2023 the original author or authors.
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

import java.beans.PropertyEditor;

import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;

/**
 * The {@code <bind>} tag supports evaluation of binding errors for a certain
 * bean or bean property. Exposes a "status" variable of type
 * {@link org.springframework.web.servlet.support.BindStatus},
 * to both Java expressions and JSP EL expressions.
 *
 * <p>Can be used to bind to any bean or bean property in the model.
 * The specified path determines whether the tag exposes the status of the
 * bean itself (showing object-level errors), a specific bean property
 * (showing field errors), or a matching set of bean properties
 * (showing all corresponding field errors).
 *
 * <p>The {@link org.springframework.validation.Errors} object that has
 * been bound using this tag is exposed to collaborating tags, as well
 * as the bean property that this errors object applies to. Nested tags
 * such as the {@link TransformTag} can access those exposed properties.
 *
 * <h3>Attribute Summary</h3>
 * <table>
 * <thead>
 * <tr>
 * <th class="table-header col-first">Attribute</th>
 * <th class="table-header col-second">Required?</th>
 * <th class="table-header col-second">Runtime Expression?</th>
 * <th class="table-header col-last">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="even-row-color">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Set HTML escaping for this tag, as boolean value. Overrides the default
 * HTML escaping setting for the current page.</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>ignoreNestedPath</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Set whether to ignore a nested path, if any.
 * Default is to not ignore.</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>path</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>The path to the bean or bean property to bind status information for.
 * For instance account.name, company.address.zipCode or just employee. The status
 * object will exported to the page scope, specifically for this bean or bean
 * property</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPath
 */
@SuppressWarnings("serial")
public class BindTag extends HtmlEscapingAwareTag implements EditorAwareTag {

	/**
	 * Name of the exposed variable within the scope of this tag: "status".
	 */
	public static final String STATUS_VARIABLE_NAME = "status";


	private String path = "";

	private boolean ignoreNestedPath = false;

	@Nullable
	private BindStatus status;

	@Nullable
	private Object previousPageStatus;

	@Nullable
	private Object previousRequestStatus;


	/**
	 * Set the path that this tag should apply. Can be a bean (e.g. "person")
	 * to get global errors, or a bean property (e.g. "person.name") to get
	 * field errors (also supporting nested fields and "person.na*" mappings).
	 * "person.*" will return all errors for the specified bean, both global
	 * and field errors.
	 * @see org.springframework.validation.Errors#getGlobalErrors
	 * @see org.springframework.validation.Errors#getFieldErrors
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Return the path that this tag applies to.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Set whether to ignore a nested path, if any.
	 * Default is to not ignore.
	 */
	public void setIgnoreNestedPath(boolean ignoreNestedPath) {
		this.ignoreNestedPath = ignoreNestedPath;
	}

	/**
	 * Return whether to ignore a nested path, if any.
	 */
	public boolean isIgnoreNestedPath() {
		return this.ignoreNestedPath;
	}


	@Override
	protected final int doStartTagInternal() throws Exception {
		String resolvedPath = getPath();
		if (!isIgnoreNestedPath()) {
			String nestedPath = (String) this.pageContext.getAttribute(
					NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
			// only prepend if not already an absolute path
			if (nestedPath != null && !resolvedPath.startsWith(nestedPath) &&
					!resolvedPath.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
				resolvedPath = nestedPath + resolvedPath;
			}
		}

		try {
			this.status = new BindStatus(getRequestContext(), resolvedPath, isHtmlEscape());
		}
		catch (IllegalStateException ex) {
			throw new JspTagException(ex.getMessage());
		}

		// Save previous status values, for re-exposure at the end of this tag.
		this.previousPageStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.previousRequestStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);

		// Expose this tag's status object as PageContext attribute,
		// making it available for JSP EL.
		this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.status, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() {
		// Reset previous status values.
		if (this.previousPageStatus != null) {
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousPageStatus, PageContext.PAGE_SCOPE);
		}
		if (this.previousRequestStatus != null) {
			this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousRequestStatus, PageContext.REQUEST_SCOPE);
		}
		else {
			this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return EVAL_PAGE;
	}


	/**
	 * Return the current BindStatus.
	 */
	private BindStatus getStatus() {
		Assert.state(this.status != null, "No current BindStatus");
		return this.status;
	}

	/**
	 * Retrieve the property that this tag is currently bound to,
	 * or {@code null} if bound to an object rather than a specific property.
	 * Intended for cooperating nesting tags.
	 * @return the property that this tag is currently bound to,
	 * or {@code null} if none
	 */
	@Nullable
	public final String getProperty() {
		return getStatus().getExpression();
	}

	/**
	 * Retrieve the Errors instance that this tag is currently bound to.
	 * Intended for cooperating nesting tags.
	 * @return the current Errors instance, or {@code null} if none
	 */
	@Nullable
	public final Errors getErrors() {
		return getStatus().getErrors();
	}

	@Override
	@Nullable
	public final PropertyEditor getEditor() {
		return getStatus().getEditor();
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.status = null;
		this.previousPageStatus = null;
		this.previousRequestStatus = null;
	}

}
