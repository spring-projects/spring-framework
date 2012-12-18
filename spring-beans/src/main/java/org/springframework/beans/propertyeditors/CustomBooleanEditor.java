/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.util.StringUtils;

/**
 * Property editor for Boolean/boolean properties.
 *
 * <p>This is not meant to be used as system PropertyEditor but rather as
 * locale-specific Boolean editor within custom controller code, to parse
 * UI-caused boolean strings into boolean properties of beans and check
 * them in the UI form.
 *
 * <p>In web MVC code, this editor will typically be registered with
 * {@code binder.registerCustomEditor} calls in an implementation
 * of BaseCommandController's {@code initBinder} method.
 *
 * @author Juergen Hoeller
 * @since 10.06.2003
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder
 */
public class CustomBooleanEditor extends PropertyEditorSupport {

	public static final String VALUE_TRUE = "true";
	public static final String VALUE_FALSE = "false";

	public static final String VALUE_ON = "on";
	public static final String VALUE_OFF = "off";

	public static final String VALUE_YES = "yes";
	public static final String VALUE_NO = "no";

	public static final String VALUE_1 = "1";
	public static final String VALUE_0 = "0";


	private final String trueString;

	private final String falseString;

	private final boolean allowEmpty;


	/**
	 * Create a new CustomBooleanEditor instance, with "true"/"on"/"yes"
	 * and "false"/"off"/"no" as recognized String values.
	 * <p>The "allowEmpty" parameter states if an empty String should
	 * be allowed for parsing, i.e. get interpreted as null value.
	 * Else, an IllegalArgumentException gets thrown in that case.
	 * @param allowEmpty if empty strings should be allowed
	 */
	public CustomBooleanEditor(boolean allowEmpty) {
		this(null, null, allowEmpty);
	}

	/**
	 * Create a new CustomBooleanEditor instance,
	 * with configurable String values for true and false.
	 * <p>The "allowEmpty" parameter states if an empty String should
	 * be allowed for parsing, i.e. get interpreted as null value.
	 * Else, an IllegalArgumentException gets thrown in that case.
	 * @param trueString the String value that represents true:
	 * for example, "true" (VALUE_TRUE), "on" (VALUE_ON),
	 * "yes" (VALUE_YES) or some custom value
	 * @param falseString the String value that represents false:
	 * for example, "false" (VALUE_FALSE), "off" (VALUE_OFF),
	 * "no" (VALUE_NO) or some custom value
	 * @param allowEmpty if empty strings should be allowed
	 * @see #VALUE_TRUE
	 * @see #VALUE_FALSE
	 * @see #VALUE_ON
	 * @see #VALUE_OFF
	 * @see #VALUE_YES
	 * @see #VALUE_NO
	 */
	public CustomBooleanEditor(String trueString, String falseString, boolean allowEmpty) {
		this.trueString = trueString;
		this.falseString = falseString;
		this.allowEmpty = allowEmpty;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String input = (text != null ? text.trim() : null);
		if (this.allowEmpty && !StringUtils.hasLength(input)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (this.trueString != null && input.equalsIgnoreCase(this.trueString)) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString != null && input.equalsIgnoreCase(this.falseString)) {
			setValue(Boolean.FALSE);
		}
		else if (this.trueString == null &&
				(input.equalsIgnoreCase(VALUE_TRUE) || input.equalsIgnoreCase(VALUE_ON) ||
				input.equalsIgnoreCase(VALUE_YES) || input.equals(VALUE_1))) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString == null &&
				(input.equalsIgnoreCase(VALUE_FALSE) || input.equalsIgnoreCase(VALUE_OFF) ||
				input.equalsIgnoreCase(VALUE_NO) || input.equals(VALUE_0))) {
			setValue(Boolean.FALSE);
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
		}
	}

	@Override
	public String getAsText() {
		if (Boolean.TRUE.equals(getValue())) {
			return (this.trueString != null ? this.trueString : VALUE_TRUE);
		}
		else if (Boolean.FALSE.equals(getValue())) {
			return (this.falseString != null ? this.falseString : VALUE_FALSE);
		}
		else {
			return "";
		}
	}

}
