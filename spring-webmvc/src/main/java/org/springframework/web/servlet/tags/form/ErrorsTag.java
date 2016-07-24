/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Form tag for displaying errors for a particular field or object.
 *
 * <p>This tag supports three main usage patterns:
 *
 * <ol>
 *	<li>Field only - set '{@code path}' to the field name (or path)</li>
 *	<li>Object errors only - omit '{@code path}'</li>
 *	<li>All errors - set '{@code path}' to '{@code *}'</li>
 * </ol>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ErrorsTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * The key under which this tag exposes error messages in
	 * the {@link PageContext#PAGE_SCOPE page context scope}.
	 */
	public static final String MESSAGES_ATTRIBUTE = "messages";

	/**
	 * The HTML '{@code span}' tag.
	 */
	public static final String SPAN_TAG = "span";


	private String element = SPAN_TAG;

	private String delimiter = "<br/>";

	/**
	 * Stores any value that existed in the 'errors messages' before the tag was started.
	 */
	private Object oldMessages;

	private boolean errorMessagesWereExposed;


	/**
	 * Set the HTML element must be used to render the error messages.
	 * <p>Defaults to an HTML '{@code <span/>}' tag.
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * Get the HTML element must be used to render the error messages.
	 */
	public String getElement() {
		return this.element;
	}

	/**
	 * Set the delimiter to be used between error messages.
	 * <p>Defaults to an HTML '{@code <br/>}' tag.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Return the delimiter to be used between error messages.
	 */
	public String getDelimiter() {
		return this.delimiter;
	}


	/**
	 * Get the value for the HTML '{@code id}' attribute.
	 * <p>Appends '{@code .errors}' to the value returned by {@link #getPropertyPath()}
	 * or to the model attribute name if the {@code <form:errors/>} tag's
	 * '{@code path}' attribute has been omitted.
	 * @return the value for the HTML '{@code id}' attribute
	 * @see #getPropertyPath()
	 */
	@Override
	protected String autogenerateId() throws JspException {
		String path = getPropertyPath();
		if ("".equals(path) || "*".equals(path)) {
			path = (String) this.pageContext.getAttribute(
					FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return StringUtils.deleteAny(path, "[]") + ".errors";
	}

	/**
	 * Get the value for the HTML '{@code name}' attribute.
	 * <p>Simply returns {@code null} because the '{@code name}' attribute
	 * is not a validate attribute for the '{@code span}' element.
	 */
	@Override
	protected String getName() throws JspException {
		return null;
	}

	/**
	 * Should rendering of this tag proceed at all?
	 * <p>Only renders output when there are errors for the configured {@link #setPath path}.
	 * @return {@code true} only when there are errors for the configured {@link #setPath path}
	 */
	@Override
	protected boolean shouldRender() throws JspException {
		try {
			return getBindStatus().isError();
		}
		catch (IllegalStateException ex) {
			// Neither BindingResult nor target object available.
			return false;
		}
	}

	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag(getElement());
		writeDefaultAttributes(tagWriter);
		String delimiter = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
		String[] errorMessages = getBindStatus().getErrorMessages();
		for (int i = 0; i < errorMessages.length; i++) {
			String errorMessage = errorMessages[i];
			if (i > 0) {
				tagWriter.appendValue(delimiter);
			}
			tagWriter.appendValue(getDisplayString(errorMessage));
		}
		tagWriter.endTag();
	}

	/**
	 * Exposes any bind status error messages under {@link #MESSAGES_ATTRIBUTE this key}
	 * in the {@link PageContext#PAGE_SCOPE}.
	 * <p>Only called if {@link #shouldRender()} returns {@code true}.
	 * @see #removeAttributes()
	 */
	@Override
	protected void exposeAttributes() throws JspException {
		List<String> errorMessages = new ArrayList<>();
		errorMessages.addAll(Arrays.asList(getBindStatus().getErrorMessages()));
		this.oldMessages = this.pageContext.getAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, errorMessages, PageContext.PAGE_SCOPE);
		this.errorMessagesWereExposed = true;
	}

	/**
	 * Removes any bind status error messages that were previously stored under
	 * {@link #MESSAGES_ATTRIBUTE this key} in the {@link PageContext#PAGE_SCOPE}.
	 * @see #exposeAttributes()
	 */
	@Override
	protected void removeAttributes() {
		if (this.errorMessagesWereExposed) {
			if (this.oldMessages != null) {
				this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, this.oldMessages, PageContext.PAGE_SCOPE);
				this.oldMessages = null;
			}
			else {
				this.pageContext.removeAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
			}
		}
	}

}
