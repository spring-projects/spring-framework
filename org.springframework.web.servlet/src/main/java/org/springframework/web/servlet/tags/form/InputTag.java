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

/**
 * Data-binding-aware JSP tag for rendering an HTML '<code>input</code>'
 * element with a '<code>type</code>' of '<code>text</code>'.
 * 
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class InputTag extends AbstractHtmlInputElementTag {

	public static final String SIZE_ATTRIBUTE = "size";

	public static final String MAXLENGTH_ATTRIBUTE = "maxlength";

	public static final String ALT_ATTRIBUTE = "alt";

	public static final String ONSELECT_ATTRIBUTE = "onselect";

	public static final String READONLY_ATTRIBUTE = "readonly";

	public static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";


	private String size;

	private String maxlength;

	private String alt;

	private String onselect;

	private String autocomplete;


	/**
	 * Set the value of the '<code>size</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setSize(String size) {
		this.size = size;
	}

	/**
	 * Get the value of the '<code>size</code>' attribute.
	 */
	protected String getSize() {
		return this.size;
	}

	/**
	 * Set the value of the '<code>maxlength</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setMaxlength(String maxlength) {
		this.maxlength = maxlength;
	}

	/**
	 * Get the value of the '<code>maxlength</code>' attribute.
	 */
	protected String getMaxlength() {
		return this.maxlength;
	}

	/**
	 * Set the value of the '<code>alt</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setAlt(String alt) {
		this.alt = alt;
	}

	/**
	 * Get the value of the '<code>alt</code>' attribute.
	 */
	protected String getAlt() {
		return this.alt;
	}

	/**
	 * Set the value of the '<code>onselect</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnselect(String onselect) {
		this.onselect = onselect;
	}

	/**
	 * Get the value of the '<code>onselect</code>' attribute.
	 */
	protected String getOnselect() {
		return this.onselect;
	}

	/**
	 * Set the value of the '<code>autocomplete</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * Get the value of the '<code>autocomplete</code>' attribute.
	 */
	protected String getAutocomplete() {
		return this.autocomplete;
	}


	/**
	 * Writes the '<code>input</code>' tag to the supplied {@link TagWriter}.
	 * Uses the value returned by {@link #getType()} to determine which
	 * type of '<code>input</code>' element to render.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");

		writeDefaultAttributes(tagWriter);
		if (!hasDynamicTypeAttribute()) {
			tagWriter.writeAttribute("type", getType());
		}
		writeValue(tagWriter);

		// custom optional attributes
		writeOptionalAttribute(tagWriter, SIZE_ATTRIBUTE, getSize());
		writeOptionalAttribute(tagWriter, MAXLENGTH_ATTRIBUTE, getMaxlength());
		writeOptionalAttribute(tagWriter, ALT_ATTRIBUTE, getAlt());
		writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		tagWriter.endTag();
		return SKIP_BODY;
	}

	private boolean hasDynamicTypeAttribute() {
		return getDynamicAttributes() != null && getDynamicAttributes().containsKey("type");
	}

	/**
	 * Writes the '<code>value</code>' attribute to the supplied {@link TagWriter}.
	 * Subclasses may choose to override this implementation to control exactly
	 * when the value is written.
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		String type = hasDynamicTypeAttribute() ? (String) getDynamicAttributes().get("type") : getType();
		tagWriter.writeAttribute("value", processFieldValue(getName(), value, type));
	}

	/**
	 * Flags {@code type="checkbox"} and {@code type="radio"} as illegal 
	 * dynamic attributes.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		if ("type".equals(localName)) {
			if ("checkbox".equals(value) || "radio".equals(value)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Get the value of the '<code>type</code>' attribute. Subclasses
	 * can override this to change the type of '<code>input</code>' element
	 * rendered. Default value is '<code>text</code>'.
	 */
	protected String getType() {
		return "text";
	}

}
