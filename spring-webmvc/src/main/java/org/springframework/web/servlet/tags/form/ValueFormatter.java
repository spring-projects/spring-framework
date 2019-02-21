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

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Package-visible helper class for formatting values for rendering via a form tag.
 * Supports two styles of formatting: plain and {@link PropertyEditor}-aware.
 *
 * <p>Plain formatting simply prevents the string '{@code null}' from appearing,
 * replacing it with an empty String, and adds HTML escaping as required.
 *
 * <p>{@link PropertyEditor}-aware formatting will attempt to use the supplied
 * {@link PropertyEditor} to render any non-String value before applying the
 * default rules of plain formatting.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class ValueFormatter {

	/**
	 * Build the display value of the supplied {@code Object}, HTML escaped
	 * as required. This version is <strong>not</strong> {@link PropertyEditor}-aware.
	 * @see #getDisplayString(Object, java.beans.PropertyEditor, boolean)
	 */
	public static String getDisplayString(@Nullable Object value, boolean htmlEscape) {
		String displayValue = ObjectUtils.getDisplayString(value);
		return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
	}

	/**
	 * Build the display value of the supplied {@code Object}, HTML escaped
	 * as required. If the supplied value is not a {@link String} and the supplied
	 * {@link PropertyEditor} is not null then the {@link PropertyEditor} is used
	 * to obtain the display value.
	 * @see #getDisplayString(Object, boolean)
	 */
	public static String getDisplayString(
			@Nullable Object value, @Nullable PropertyEditor propertyEditor, boolean htmlEscape) {

		if (propertyEditor != null && !(value instanceof String)) {
			try {
				propertyEditor.setValue(value);
				String text = propertyEditor.getAsText();
				if (text != null) {
					return getDisplayString(text, htmlEscape);
				}
			}
			catch (Throwable ex) {
				// The PropertyEditor might not support this value... pass through.
			}
		}
		return getDisplayString(value, htmlEscape);
	}

}
