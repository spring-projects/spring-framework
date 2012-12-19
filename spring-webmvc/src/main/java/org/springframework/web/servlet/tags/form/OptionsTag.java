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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * Convenient tag that allows one to supply a collection of objects
 * that are to be rendered as '{@code option}' tags within a
 * '{@code select}' tag.
 *
 * <p><i>Must</i> be used within a {@link SelectTag 'select' tag}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @since 2.0
 */
@SuppressWarnings("serial")
public class OptionsTag extends AbstractHtmlElementTag {

	/**
	 * The {@link java.util.Collection}, {@link java.util.Map} or array of
	 * objects used to generate the inner '{@code option}' tags.
	 */
	private Object items;

	/**
	 * The name of the property mapped to the '{@code value}' attribute
	 * of the '{@code option}' tag.
	 */
	private String itemValue;

	/**
	 * The name of the property mapped to the inner text of the
	 * '{@code option}' tag.
	 */
	private String itemLabel;

	private String disabled;


	/**
	 * Set the {@link java.util.Collection}, {@link java.util.Map} or array
	 * of objects used to generate the inner '{@code option}' tags.
	 * <p>Required when wishing to render '{@code option}' tags from an
	 * array, {@link java.util.Collection} or {@link java.util.Map}.
	 * <p>Typically a runtime expression.
	 */
	public void setItems(Object items) {
		this.items = items;
	}

	/**
	 * Get the {@link java.util.Collection}, {@link java.util.Map} or array
	 * of objects used to generate the inner '{@code option}' tags.
	 * <p>Typically a runtime expression.
	 */
	protected Object getItems() {
		return this.items;
	}

	/**
	 * Set the name of the property mapped to the '{@code value}'
	 * attribute of the '{@code option}' tag.
	 * <p>Required when wishing to render '{@code option}' tags from
	 * an array or {@link java.util.Collection}.
	 * <p>May be a runtime expression.
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * Return the name of the property mapped to the '{@code value}'
	 * attribute of the '{@code option}' tag.
	 */
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * Set the name of the property mapped to the label (inner text) of the
	 * '{@code option}' tag.
	 * <p>May be a runtime expression.
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * Get the name of the property mapped to the label (inner text) of the
	 * '{@code option}' tag.
	 * <p>May be a runtime expression.
	 */
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * Set the value of the '{@code disabled}' attribute.
	 * <p>May be a runtime expression.
	 * @param disabled the value of the '{@code disabled}' attribute
	 */
	public void setDisabled(String disabled) {
		this.disabled = disabled;
	}

	/**
	 * Get the value of the '{@code disabled}' attribute.
	 */
	protected String getDisabled() {
		return this.disabled;
	}

	/**
	 * Is the current HTML tag disabled?
	 * @return {@code true} if this tag is disabled
	 */
	protected boolean isDisabled() throws JspException {
		return evaluateBoolean("disabled", getDisabled());
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		SelectTag selectTag = getSelectTag();
		Object items = getItems();
		Object itemsObject = null;
		if (items != null) {
			itemsObject = (items instanceof String ? evaluate("items", items) : items);
		} else {
			Class<?> selectTagBoundType = selectTag.getBindStatus().getValueType();
			if (selectTagBoundType != null && selectTagBoundType.isEnum()) {
				itemsObject = selectTagBoundType.getEnumConstants();
			}
		}
		if (itemsObject != null) {
			String selectName = selectTag.getName();
			String itemValue = getItemValue();
			String itemLabel = getItemLabel();
			String valueProperty =
					(itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
			String labelProperty =
					(itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);
			OptionsWriter optionWriter = new OptionsWriter(selectName, itemsObject, valueProperty, labelProperty);
			optionWriter.writeOptions(tagWriter);
		}
		return SKIP_BODY;
	}

	/**
	 * Appends a counter to a specified id,
	 * since we're dealing with multiple HTML elements.
	 */
	@Override
	protected String resolveId() throws JspException {
		Object id = evaluate("id", getId());
		if (id != null) {
			String idString = id.toString();
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		return null;
	}

	private SelectTag getSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "options", "select");
		return (SelectTag) findAncestorWithClass(this, SelectTag.class);
	}

	@Override
	protected BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}


	/**
	 * Inner class that adapts OptionWriter for multiple options to be rendered.
	 */
	private class OptionsWriter extends OptionWriter {

		private final String selectName;

		public OptionsWriter(String selectName, Object optionSource, String valueProperty, String labelProperty) {
			super(optionSource, getBindStatus(), valueProperty, labelProperty, isHtmlEscape());
			this.selectName = selectName;
		}

		@Override
		protected boolean isOptionDisabled() throws JspException {
			return isDisabled();
		}

		@Override
		protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
			writeOptionalAttribute(tagWriter, "id", resolveId());
			writeOptionalAttributes(tagWriter);
		}

		@Override
		protected String processOptionValue(String value) {
			return processFieldValue(this.selectName, value, "option");
		}

	}

}
