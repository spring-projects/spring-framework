/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * The {@code <option>} tag renders a single HTML 'option'. Sets 'selected' as
 * appropriate based on bound value.
 *
 * <p><b>Must be used nested inside a {@link SelectTag}.</b>
 *
 * <p>Provides full support for databinding by marking an
 * '{@code option}' as 'selected' if the {@link #setValue value}
 * matches the value bound to the out {@link SelectTag}.
 *
 * <p>The {@link #setValue value} property is required and corresponds to
 * the '{@code value}' attribute of the rendered '{@code option}'.
 *
 * <p>An optional {@link #setLabel label} property can be specified, the
 * value of which corresponds to inner text of the rendered
 * '{@code option}' tag. If no {@link #setLabel label} is specified
 * then the {@link #setValue value} property will be used when rendering
 * the inner text.
 *
 * <p>
 * <table>
 * <caption>Attribute Summary</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">Attribute</th>
 * <th class="colOne">Required?</th>
 * <th class="colOne">Runtime Expression?</th>
 * <th class="colLast">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssErrorClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute. Used when the bound field has
 * errors.</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute. Setting the value of this attribute to 'true'
 * will disable the HTML element.</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>Enable/disable HTML escaping of rendered values.</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>label</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Event Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>value</p></td>
 * <td><p>true</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OptionTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * The name of the JSP variable used to expose the value for this tag.
	 */
	public static final String VALUE_VARIABLE_NAME = "value";

	/**
	 * The name of the JSP variable used to expose the display value for this tag.
	 */
	public static final String DISPLAY_VALUE_VARIABLE_NAME = "displayValue";

	/**
	 * The name of the '{@code selected}' attribute.
	 */
	private static final String SELECTED_ATTRIBUTE = "selected";

	/**
	 * The name of the '{@code value}' attribute.
	 */
	private static final String VALUE_ATTRIBUTE = VALUE_VARIABLE_NAME;

	/**
	 * The name of the '{@code disabled}' attribute.
	 */
	private static final String DISABLED_ATTRIBUTE = "disabled";


	/**
	 * The 'value' attribute of the rendered HTML {@code <option>} tag.
	 */
	@Nullable
	private Object value;

	/**
	 * The text body of the rendered HTML {@code <option>} tag.
	 */
	@Nullable
	private String label;

	@Nullable
	private Object oldValue;

	@Nullable
	private Object oldDisplayValue;

	private boolean disabled;


	/**
	 * Set the 'value' attribute of the rendered HTML {@code <option>} tag.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Get the 'value' attribute of the rendered HTML {@code <option>} tag.
	 */
	@Nullable
	protected Object getValue() {
		return this.value;
	}

	/**
	 * Set the value of the '{@code disabled}' attribute.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Get the value of the '{@code disabled}' attribute.
	 */
	protected boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Set the text body of the rendered HTML {@code <option>} tag.
	 * <p>May be a runtime expression.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Get the text body of the rendered HTML {@code <option>} tag.
	 */
	@Nullable
	protected String getLabel() {
		return this.label;
	}


	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = getLabelValue(value);
		renderOption(value, label, tagWriter);
	}

	@Override
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		String label = bodyContent.getString();
		renderOption(value, label, tagWriter);
	}

	/**
	 * Make sure we are under a '{@code select}' tag before proceeding.
	 */
	@Override
	protected void onWriteTagContent() {
		assertUnderSelectTag();
	}

	@Override
	protected void exposeAttributes() throws JspException {
		Object value = resolveValue();
		this.oldValue = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(VALUE_VARIABLE_NAME, value);
		this.oldDisplayValue = this.pageContext.getAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, getDisplayString(value, getBindStatus().getEditor()));
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}

	@Override
	protected void removeAttributes() {
		if (this.oldValue != null) {
			this.pageContext.setAttribute(VALUE_ATTRIBUTE, this.oldValue);
			this.oldValue = null;
		}
		else {
			this.pageContext.removeAttribute(VALUE_VARIABLE_NAME);
		}

		if (this.oldDisplayValue != null) {
			this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, this.oldDisplayValue);
			this.oldDisplayValue = null;
		}
		else {
			this.pageContext.removeAttribute(DISPLAY_VALUE_VARIABLE_NAME);
		}
	}

	private void renderOption(Object value, String label, TagWriter tagWriter) throws JspException {
		tagWriter.startTag("option");
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttributes(tagWriter);
		String renderedValue = getDisplayString(value, getBindStatus().getEditor());
		renderedValue = processFieldValue(getSelectTag().getName(), renderedValue, "option");
		tagWriter.writeAttribute(VALUE_ATTRIBUTE, renderedValue);
		if (isSelected(value)) {
			tagWriter.writeAttribute(SELECTED_ATTRIBUTE, SELECTED_ATTRIBUTE);
		}
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.appendValue(label);
		tagWriter.endTag();
	}

	@Override
	protected String autogenerateId() throws JspException {
		return null;
	}

	/**
	 * Return the value of the label for this '{@code option}' element.
	 * <p>If the {@link #setLabel label} property is set then the resolved value
	 * of that property is used, otherwise the value of the {@code resolvedValue}
	 * argument is used.
	 */
	private String getLabelValue(Object resolvedValue) throws JspException {
		String label = getLabel();
		Object labelObj = (label == null ? resolvedValue : evaluate("label", label));
		return getDisplayString(labelObj, getBindStatus().getEditor());
	}

	private void assertUnderSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "option", "select");
	}

	private SelectTag getSelectTag() {
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	private boolean isSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(getBindStatus(), resolvedValue);
	}

	@Nullable
	private Object resolveValue() throws JspException {
		return evaluate(VALUE_VARIABLE_NAME, getValue());
	}

}
