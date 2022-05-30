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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.EditorAwareTag;
import org.springframework.web.servlet.tags.NestedPathTag;

/**
 * Base tag for all data-binding aware JSP form tags.
 *
 * <p>Provides the common {@link #setPath path} and {@link #setId id} properties.
 * Provides sub-classes with utility methods for accessing the {@link BindStatus}
 * of their bound value and also for {@link #writeOptionalAttribute interacting}
 * with the {@link TagWriter}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractDataBoundFormElementTag extends AbstractFormTag implements EditorAwareTag {

	/**
	 * Name of the exposed path variable within the scope of this tag: "nestedPath".
	 * Same value as {@link org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME}.
	 */
	protected static final String NESTED_PATH_VARIABLE_NAME = NestedPathTag.NESTED_PATH_VARIABLE_NAME;


	/**
	 * The property path from the {@link FormTag#setModelAttribute form object}.
	 */
	@Nullable
	private String path;

	/**
	 * The value of the '{@code id}' attribute.
	 */
	@Nullable
	private String id;

	/**
	 * The {@link BindStatus} of this tag.
	 */
	@Nullable
	private BindStatus bindStatus;


	/**
	 * Set the property path from the {@link FormTag#setModelAttribute form object}.
	 * May be a runtime expression.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get the {@link #evaluate resolved} property path for the
	 * {@link FormTag#setModelAttribute form object}.
	 */
	protected final String getPath() throws JspException {
		String resolvedPath = (String) evaluate("path", this.path);
		return (resolvedPath != null ? resolvedPath : "");
	}

	/**
	 * Set the value of the '{@code id}' attribute.
	 * <p>May be a runtime expression; defaults to the value of {@link #getName()}.
	 * Note that the default value may not be valid for certain tags.
	 */
	@Override
	public void setId(@Nullable String id) {
		this.id = id;
	}

	/**
	 * Get the value of the '{@code id}' attribute.
	 */
	@Override
	@Nullable
	public String getId() {
		return this.id;
	}


	/**
	 * Writes the default set of attributes to the supplied {@link TagWriter}.
	 * Further abstract sub-classes should override this method to add in
	 * any additional default attributes but <strong>must</strong> remember
	 * to call the {@code super} method.
	 * <p>Concrete sub-classes should call this method when/if they want
	 * to render default attributes.
	 * @param tagWriter the {@link TagWriter} to which any attributes are to be written
	 */
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttribute(tagWriter, "name", getName());
	}

	/**
	 * Determine the '{@code id}' attribute value for this tag,
	 * autogenerating one if none specified.
	 * @see #getId()
	 * @see #autogenerateId()
	 */
	@Nullable
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? idString : null);
		}
		return autogenerateId();
	}

	/**
	 * Autogenerate the '{@code id}' attribute value for this tag.
	 * <p>The default implementation simply delegates to {@link #getName()},
	 * deleting invalid characters (such as "[" or "]").
	 */
	@Nullable
	protected String autogenerateId() throws JspException {
		String name = getName();
		return (name != null ? StringUtils.deleteAny(name, "[]") : null);
	}

	/**
	 * Get the value for the HTML '{@code name}' attribute.
	 * <p>The default implementation simply delegates to
	 * {@link #getPropertyPath()} to use the property path as the name.
	 * For the most part this is desirable as it links with the server-side
	 * expectation for data binding. However, some subclasses may wish to change
	 * the value of the '{@code name}' attribute without changing the bind path.
	 * @return the value for the HTML '{@code name}' attribute
	 */
	@Nullable
	protected String getName() throws JspException {
		return getPropertyPath();
	}

	/**
	 * Get the {@link BindStatus} for this tag.
	 */
	protected BindStatus getBindStatus() throws JspException {
		if (this.bindStatus == null) {
			// HTML escaping in tags is performed by the ValueFormatter class.
			String nestedPath = getNestedPath();
			String pathToUse = (nestedPath != null ? nestedPath + getPath() : getPath());
			if (pathToUse.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
				pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
			}
			this.bindStatus = new BindStatus(getRequestContext(), pathToUse, false);
		}
		return this.bindStatus;
	}

	/**
	 * Get the value of the nested path that may have been exposed by the
	 * {@link NestedPathTag}.
	 */
	@Nullable
	protected String getNestedPath() {
		return (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
	}

	/**
	 * Build the property path for this tag, including the nested path
	 * but <i>not</i> prefixed with the name of the form attribute.
	 * @see #getNestedPath()
	 * @see #getPath()
	 */
	protected String getPropertyPath() throws JspException {
		String expression = getBindStatus().getExpression();
		return (expression != null ? expression : "");
	}

	/**
	 * Get the bound value.
	 * @see #getBindStatus()
	 */
	@Nullable
	protected final Object getBoundValue() throws JspException {
		return getBindStatus().getValue();
	}

	/**
	 * Get the {@link PropertyEditor}, if any, in use for value bound to this tag.
	 */
	@Nullable
	protected PropertyEditor getPropertyEditor() throws JspException {
		return getBindStatus().getEditor();
	}

	/**
	 * Exposes the {@link PropertyEditor} for {@link EditorAwareTag}.
	 * <p>Use {@link #getPropertyEditor()} for internal rendering purposes.
	 */
	@Override
	@Nullable
	public final PropertyEditor getEditor() throws JspException {
		return getPropertyEditor();
	}

	/**
	 * Get a display String for the given value, converted by a PropertyEditor
	 * that the BindStatus may have registered for the value's Class.
	 */
	protected String convertToDisplayString(@Nullable Object value) throws JspException {
		PropertyEditor editor = (value != null ? getBindStatus().findEditor(value.getClass()) : null);
		return getDisplayString(value, editor);
	}

	/**
	 * Process the given form field through a {@link RequestDataValueProcessor}
	 * instance if one is configured or otherwise returns the same value.
	 */
	protected final String processFieldValue(@Nullable String name, String value, String type) {
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		ServletRequest request = this.pageContext.getRequest();
		if (processor != null && request instanceof HttpServletRequest) {
			value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
		}
		return value;
	}

	/**
	 * Disposes of the {@link BindStatus} instance.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.bindStatus = null;
	}

}
