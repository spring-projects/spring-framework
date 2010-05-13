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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * Abstract base class to provide common methods for
 * implementing databinding-aware JSP tags for rendering an HTML '<code>input</code>'
 * element with a '<code>type</code>' of '<code>checkbox</code>' or '<code>radio</code>'.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class AbstractCheckedElementTag extends AbstractHtmlInputElementTag {

	/**
	 * Render the '<code>input(checkbox)</code>' with the supplied value, marking the
	 * '<code>input</code>' element as 'checked' if the supplied value matches the
	 * bound value.
	 */
	protected void renderFromValue(Object value, TagWriter tagWriter) throws JspException {
		renderFromValue(value, value, tagWriter);
	}

	/**
	 * Render the '<code>input(checkbox)</code>' with the supplied value, marking the
	 * '<code>input</code>' element as 'checked' if the supplied value matches the
	 * bound value.
	 */
	protected void renderFromValue(Object item, Object value, TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("value", convertToDisplayString(value));
		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * Determines whether the supplied value matched the selected value
	 * through delegating to {@link SelectedValueComparator#isSelected}.
	 */
	private boolean isOptionSelected(Object value) throws JspException {
		return SelectedValueComparator.isSelected(getBindStatus(), value);
	}

	/**
	 * Render the '<code>input(checkbox)</code>' with the supplied value, marking
	 * the '<code>input</code>' element as 'checked' if the supplied Boolean is
	 * <code>true</code>.
	 */
	protected void renderFromBoolean(Boolean boundValue, TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("value", "true");
		if (boundValue) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * Return a unique ID for the bound name within the current PageContext.
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return TagIdGenerator.nextId(super.autogenerateId(), this.pageContext);
	}


	/**
	 * Writes the '<code>input</code>' element to the supplied
	 * {@link org.springframework.web.servlet.tags.form.TagWriter},
	 * marking it as 'checked' if appropriate.
	 */
	@Override
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
