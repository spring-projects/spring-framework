/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

/**
 * Property editor that convert to a {@code null} for an empty string.
 * <p>
 * This class does not trim a given string. If the {@link StringTrimmerEditor} used,
 * you can trim a given string.
 * This class does not used by default.
 * If this class is used, developer must be register it in custom editor.
 *
 * @author Kazuki Shimizu
 * @since 4.2.2
 * @see StringTrimmerEditor
 */
public class EmptyStringToNullEditor extends PropertyEditorSupport {

	/**
	 * Sets the property value by parsing a given string.
	 * <p>
	 * If a given string is empty, this method set a {@code null} as the property value.
	 *
	 * @param text a given string
	 */
	@Override
	public void setAsText(String text) {
		if (text == null || text.isEmpty()) {
			setValue(null);
		} else {
			setValue(text);
		}
	}

	/**
	 * Gets the property value as a string suitable for presentation to a human to edit.
	 *
	 * @return the property value as a string. If property value is {@code null},
	 * return value become an empty string.
	 */
	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null) ? value.toString() : "";
	}

}
