/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * Property editor that trims Strings.
 *
 * <p>Optionally allows transforming an empty string into a {@code null} value.
 * Needs to be explicitly registered, e.g. for command binding.
 *
 * @author Juergen Hoeller
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class StringTrimmerEditor extends PropertyEditorSupport {

	private final String charsToDelete;

	private final boolean emptyAsNull;


	/**
	 * Create a new StringTrimmerEditor.
	 * @param emptyAsNull {@code true} if an empty String is to be
	 * transformed into {@code null}
	 */
	public StringTrimmerEditor(boolean emptyAsNull) {
		this.charsToDelete = null;
		this.emptyAsNull = emptyAsNull;
	}

	/**
	 * Create a new StringTrimmerEditor.
	 * @param charsToDelete a set of characters to delete, in addition to
	 * trimming an input String. Useful for deleting unwanted line breaks:
	 * e.g. "\r\n\f" will delete all new lines and line feeds in a String.
	 * @param emptyAsNull {@code true} if an empty String is to be
	 * transformed into {@code null}
	 */
	public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull) {
		this.charsToDelete = charsToDelete;
		this.emptyAsNull = emptyAsNull;
	}


	@Override
	public void setAsText(String text) {
		if (text == null) {
			setValue(null);
		}
		else {
			String value = text.trim();
			if (this.charsToDelete != null) {
				value = StringUtils.deleteAny(value, this.charsToDelete);
			}
			if (this.emptyAsNull && "".equals(value)) {
				setValue(null);
			}
			else {
				setValue(value);
			}
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

}
