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
 * Data-binding aware JSP tag for rendering a hidden HTML '<code>input</code>' field
 * containing the databound value.
 *
 * <p>Example (binding to 'name' property of form backing object):
 * <pre class="code>
 * &lt;form:hidden path=&quot;name&quot;/&gt;
 * </pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HiddenInputTag extends AbstractHtmlElementTag {

	/**
	 * The name of the '<code>disabled</code>' attribute.
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";

	private String disabled;

	/**
	 * Get the value of the '<code>disabled</code>' attribute.
	 */
	public String getDisabled() {
		return this.disabled;
	}

	/**
	 * Set the value of the '<code>disabled</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setDisabled(String disabled) {
		this.disabled = disabled;
	}

	/**
	 * Flags "type" as an illegal dynamic attribute.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * Writes the HTML '<code>input</code>' tag to the supplied {@link TagWriter} including the
	 * databound value.
	 * @see #writeDefaultAttributes(TagWriter)
	 * @see #getBoundValue()
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", "hidden");
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, "hidden"));
		tagWriter.endTag();
		return SKIP_BODY;
	}

	/**
	 * Is the current HTML tag disabled?
	 */
	protected boolean isDisabled() throws JspException {
		return evaluateBoolean(DISABLED_ATTRIBUTE, getDisabled());
	}

}
