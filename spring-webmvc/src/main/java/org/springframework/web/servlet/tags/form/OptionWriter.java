/*
 * Copyright 2002-2012 the original author or authors.
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

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

import javax.servlet.jsp.JspException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * Provides supporting functionality to render a list of '{@code option}'
 * tags based on some source object. This object can be either an array, a
 * {@link Collection}, or a {@link Map}.
 * <h3>Using an array or a {@link Collection}:</h3>
 * <p>
 * If you supply an array or {@link Collection} source object to render the
 * inner '{@code option}' tags, you may optionally specify the name of
 * the property on the objects which corresponds to the <em>value</em> of the
 * rendered '{@code option}' (i.e., the {@code valueProperty})
 * and the name of the property that corresponds to the <em>label</em> (i.e.,
 * the {@code labelProperty}). These properties are then used when
 * rendering each element of the array/{@link Collection} as an '{@code option}'.
 * If either property name is omitted, the value of {@link Object#toString()} of
 * the corresponding array/{@link Collection} element is used instead.  However,
 * if the item is an enum, {@link Enum#name()} is used as the default value.
 * </p>
 * <h3>Using a {@link Map}:</h3>
 * <p>
 * You can alternatively choose to render '{@code option}' tags by
 * supplying a {@link Map} as the source object.
 * </p>
 * <p>
 * If you <strong>omit</strong> property names for the <em>value</em> and
 * <em>label</em>:
 * </p>
 * <ul>
 * <li>the {@code key} of each {@link Map} entry will correspond to the
 * <em>value</em> of the rendered '{@code option}', and</li>
 * <li>the {@code value} of each {@link Map} entry will correspond to
 * the <em>label</em> of the rendered '{@code option}'.</li>
 * </ul>
 * <p>
 * If you <strong>supply</strong> property names for the <em>value</em> and
 * <em>label</em>:
 * </p>
 * <ul>
 * <li>the <em>value</em> of the rendered '{@code option}' will be
 * retrieved from the {@code valueProperty} on the object
 * corresponding to the {@code key} of each {@link Map} entry, and</li>
 * <li>the <em>label</em> of the rendered '{@code option}' will be
 * retrieved from the {@code labelProperty} on the object
 * corresponding to the {@code value} of each {@link Map} entry.
 * </ul>
 * <h3>When using either of these approaches:</h3>
 * <ul>
 * <li>Property names for the <em>value</em> and <em>label</em> are
 * specified as arguments to the
 * {@link #OptionWriter(Object, BindStatus, String, String, boolean) constructor}.</li>
 * <li>An '{@code option}' is marked as 'selected' if its key
 * {@link #isOptionSelected matches} the value that is bound to the tag instance.</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Scott Andrews
 * @since 2.0
 */
class OptionWriter {

	private final Object optionSource;

	private final BindStatus bindStatus;

	private final String valueProperty;

	private final String labelProperty;

	private final boolean htmlEscape;


	/**
	 * Creates a new {@code OptionWriter} for the supplied {@code objectSource}.
	 * @param optionSource the source of the {@code options} (never {@code null})
	 * @param bindStatus the {@link BindStatus} for the bound value (never {@code null})
	 * @param valueProperty the name of the property used to render {@code option} values
	 * (optional)
	 * @param labelProperty the name of the property used to render {@code option} labels
	 * (optional)
	 */
	public OptionWriter(
			Object optionSource, BindStatus bindStatus, String valueProperty, String labelProperty, boolean htmlEscape) {

		Assert.notNull(optionSource, "'optionSource' must not be null");
		Assert.notNull(bindStatus, "'bindStatus' must not be null");
		this.optionSource = optionSource;
		this.bindStatus = bindStatus;
		this.valueProperty = valueProperty;
		this.labelProperty = labelProperty;
		this.htmlEscape = htmlEscape;
	}


	/**
	 * Write the '{@code option}' tags for the configured {@link #optionSource} to
	 * the supplied {@link TagWriter}.
	 */
	public void writeOptions(TagWriter tagWriter) throws JspException {
		if (this.optionSource.getClass().isArray()) {
			renderFromArray(tagWriter);
		}
		else if (this.optionSource instanceof Collection) {
			renderFromCollection(tagWriter);
		}
		else if (this.optionSource instanceof Map) {
			renderFromMap(tagWriter);
		}
		else if (this.optionSource instanceof Class && ((Class<?>) this.optionSource).isEnum()) {
			renderFromEnum(tagWriter);
		}
		else {
			throw new JspException(
					"Type [" + this.optionSource.getClass().getName() + "] is not valid for option items");
		}
	}

	/**
	 * Renders the inner '{@code option}' tags using the {@link #optionSource}.
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromArray(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(this.optionSource), tagWriter);
	}

	/**
	 * Renders the inner '{@code option}' tags using the supplied
	 * {@link Map} as the source.
	 * @see #renderOption(TagWriter, Object, Object, Object)
	 */
	private void renderFromMap(TagWriter tagWriter) throws JspException {
		Map<?, ?> optionMap = (Map<?, ?>) this.optionSource;
		for (Map.Entry<?, ?> entry : optionMap.entrySet()) {
			Object mapKey = entry.getKey();
			Object mapValue = entry.getValue();
			Object renderValue = (this.valueProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapKey).getPropertyValue(this.valueProperty) :
					mapKey);
			Object renderLabel = (this.labelProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapValue).getPropertyValue(this.labelProperty) :
					mapValue);
			renderOption(tagWriter, mapKey, renderValue, renderLabel);
		}
	}

	/**
	 * Renders the inner '{@code option}' tags using the {@link #optionSource}.
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromCollection(TagWriter tagWriter) throws JspException {
		doRenderFromCollection((Collection<?>) this.optionSource, tagWriter);
	}

	/**
	 * Renders the inner '{@code option}' tags using the {@link #optionSource}.
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromEnum(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(((Class<?>) this.optionSource).getEnumConstants()), tagWriter);
	}

	/**
	 * Renders the inner '{@code option}' tags using the supplied {@link Collection} of
	 * objects as the source. The value of the {@link #valueProperty} field is used
	 * when rendering the '{@code value}' of the '{@code option}' and the value of the
	 * {@link #labelProperty} property is used when rendering the label.
	 */
	private void doRenderFromCollection(Collection<?> optionCollection, TagWriter tagWriter) throws JspException {
		for (Object item : optionCollection) {
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
			Object value;
			if (this.valueProperty != null) {
				value = wrapper.getPropertyValue(this.valueProperty);
			}
			else if (item instanceof Enum) {
				value = ((Enum<?>) item).name();
			}
			else {
				value = item;
			}
			Object label = (this.labelProperty != null ? wrapper.getPropertyValue(this.labelProperty) : item);
			renderOption(tagWriter, item, value, label);
		}
	}

	/**
	 * Renders an HTML '{@code option}' with the supplied value and label. Marks the
	 * value as 'selected' if either the item itself or its value match the bound value.
	 */
	private void renderOption(TagWriter tagWriter, Object item, Object value, Object label) throws JspException {
		tagWriter.startTag("option");
		writeCommonAttributes(tagWriter);

		String valueDisplayString = getDisplayString(value);
		String labelDisplayString = getDisplayString(label);

		valueDisplayString = processOptionValue(valueDisplayString);

		// allows render values to handle some strange browser compat issues.
		tagWriter.writeAttribute("value", valueDisplayString);

		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("selected", "selected");
		}
		if (isOptionDisabled()) {
			tagWriter.writeAttribute("disabled", "disabled");
		}
		tagWriter.appendValue(labelDisplayString);
		tagWriter.endTag();
	}

	/**
	 * Determines the display value of the supplied {@code Object},
	 * HTML-escaped as required.
	 */
	private String getDisplayString(Object value) {
		PropertyEditor editor = (value != null ? this.bindStatus.findEditor(value.getClass()) : null);
		return ValueFormatter.getDisplayString(value, editor, this.htmlEscape);
	}

	/**
	 * Process the option value before it is written.
	 * The default implementation simply returns the same value unchanged.
	 */
	protected String processOptionValue(String resolvedValue) {
		return resolvedValue;
	}

	/**
	 * Determine whether the supplied values matched the selected value.
	 * Delegates to {@link SelectedValueComparator#isSelected}.
	 */
	private boolean isOptionSelected(Object resolvedValue) {
		return SelectedValueComparator.isSelected(this.bindStatus, resolvedValue);
	}

	/**
	 * Determine whether the option fields should be disabled.
	 */
	protected boolean isOptionDisabled() throws JspException {
		return false;
	}

	/**
	 * Writes default attributes configured to the supplied {@link TagWriter}.
	 */
	protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
	}

}
