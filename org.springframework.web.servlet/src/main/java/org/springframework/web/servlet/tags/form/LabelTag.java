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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Databinding-aware JSP tag for rendering an HTML '<code>label</code>' element
 * that defines text that is associated with a single form element.
 *
 * <p>The {@link #setFor(String) 'for'} attribute is required.
 *
 * <p>See the "formTags" showcase application that ships with the
 * full Spring distribution for an example of this class in action.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class LabelTag extends AbstractHtmlElementTag {

	/**
	 * The HTML '<code>label</code>' tag.
	 */
	private static final String LABEL_TAG = "label";

	/**
	 * The name of the '<code>for</code>' attribute.
	 */
	private static final String FOR_ATTRIBUTE = "for";


	/**
	 * The {@link TagWriter} instance being used.
	 * <p>Stored so we can close the tag on {@link #doEndTag()}.
	 */
	private TagWriter tagWriter;

	/**
	 * The value of the '<code>for</code>' attribute.
	 */
	private String forId;


	/**
	 * Set the value of the '<code>for</code>' attribute.
	 * <p>Defaults to the value of {@link #getPath}; may be a runtime expression.
	 * @throws IllegalArgumentException if the supplied value is <code>null</code> 
	 */
	public void setFor(String forId) {
		Assert.notNull(forId, "'forId' must not be null");
		this.forId = forId;
	}

	/**
	 * Get the value of the '<code>id</code>' attribute.
	 * <p>May be a runtime expression.
	 */
	public String getFor() {
		return this.forId;
	}


	/**
	 * Writes the opening '<code>label</code>' tag and forces a block tag so
	 * that body content is written correctly.
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag(LABEL_TAG);
		tagWriter.writeAttribute(FOR_ATTRIBUTE, resolveFor());
		writeDefaultAttributes(tagWriter);
		tagWriter.forceBlock();
		this.tagWriter = tagWriter;
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Overrides {@link #getName()} to always return <code>null</code>,
	 * because the '<code>name</code>' attribute is not supported by the
	 * '<code>label</code>' tag.
	 * @return the value for the HTML '<code>name</code>' attribute
	 */
	@Override
	protected String getName() throws JspException {
		// This also suppresses the 'id' attribute (which is okay for a <label/>)
		return null;
	}

	/**
	 * Determine the '<code>for</code>' attribute value for this tag,
	 * autogenerating one if none specified.
	 * @see #getFor()
	 * @see #autogenerateFor()
	 */
	protected String resolveFor() throws JspException {
		if (StringUtils.hasText(this.forId)) {
			return getDisplayString(evaluate(FOR_ATTRIBUTE, this.forId));
		}
		else {
			return autogenerateFor();
		}
	}

	/**
	 * Autogenerate the '<code>for</code>' attribute value for this tag.
	 * <p>The default implementation delegates to {@link #getPropertyPath()},
	 * deleting invalid characters (such as "[" or "]").
	 */
	protected String autogenerateFor() throws JspException {
		return StringUtils.deleteAny(getPropertyPath(), "[]");
	}

	/**
	 * Close the '<code>label</code>' tag.
	 */
	@Override
	public int doEndTag() throws JspException {
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * Disposes of the {@link TagWriter} instance.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
	}

}
