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

import java.util.Collection;

import javax.servlet.jsp.JspException;

import org.springframework.web.bind.WebDataBinder;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code input}'
 * element with a '{@code type}' of '{@code checkbox}'.
 *
 * <p>May be used in one of three different approaches depending on the
 * type of the {@link #getValue bound value}.
 *
 * <h3>Approach One</h3>
 * When the bound value is of type {@link Boolean} then the '{@code input(checkbox)}'
 * is marked as 'checked' if the bound value is {@code true}. The '{@code value}'
 * attribute corresponds to the resolved value of the {@link #setValue(Object) value} property.
 * <h3>Approach Two</h3>
 * When the bound value is of type {@link Collection} then the '{@code input(checkbox)}'
 * is marked as 'checked' if the configured {@link #setValue(Object) value} is present in
 * the bound {@link Collection}.
 * <h3>Approach Three</h3>
 * For any other bound value type, the '{@code input(checkbox)}' is marked as 'checked'
 * if the the configured {@link #setValue(Object) value} is equal to the bound value.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class CheckboxTag extends AbstractSingleCheckedElementTag {

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		super.writeTagContent(tagWriter);

		if (!isDisabled()) {
			// Write out the 'field was present' marker.
			tagWriter.startTag("input");
			tagWriter.writeAttribute("type", "hidden");
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			tagWriter.writeAttribute("name", name);
			tagWriter.writeAttribute("value", processFieldValue(name, "on", getInputType()));
			tagWriter.endTag();
		}

		return SKIP_BODY;
	}

	@Override
	protected void writeTagDetails(TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("type", getInputType());

		Object boundValue = getBoundValue();
		Class valueType = getBindStatus().getValueType();

		if (Boolean.class.equals(valueType) || boolean.class.equals(valueType)) {
			// the concrete type may not be a Boolean - can be String
			if (boundValue instanceof String) {
				boundValue = Boolean.valueOf((String) boundValue);
			}
			Boolean booleanValue = (boundValue != null ? (Boolean) boundValue : Boolean.FALSE);
			renderFromBoolean(booleanValue, tagWriter);
		}

		else {
			Object value = getValue();
			if (value == null) {
				throw new IllegalArgumentException("Attribute 'value' is required when binding to non-boolean values");
			}
			Object resolvedValue = (value instanceof String ? evaluate("value", value) : value);
			renderFromValue(resolvedValue, tagWriter);
		}
	}

	@Override
	protected String getInputType() {
		return "checkbox";
	}

}
