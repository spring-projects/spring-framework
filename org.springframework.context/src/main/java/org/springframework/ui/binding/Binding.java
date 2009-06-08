/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.binding;

/**
 * A binding between a user interface element and a model property.
 * @author Keith Donald
 */
public interface Binding {

	// single-value properties
	
	/**
	 * The formatted value to display in the user interface.
	 */
	String getValue();
	
	/**
	 * Sets the model property value a from user-entered value.
	 * @param formatted the value entered by the user
	 */
	void setValue(String formatted);

	/**
	 * Formats a candidate model property value for display in the user interface.
	 * @param selectableValue a possible value
	 * @return the formatted value to display in the user interface
	 */
	String format(Object selectableValue);

	// multi-value properties
	
	/**
	 * Is this binding associated with a collection or array property?
	 * If so, a client should call {@link #getValues()} to display property values in the user interface.
	 * A client should call {@link #setValues(String[])} to set model property values from user-entered/selected values.
	 */
	boolean isCollection();

	/**
	 * When a collection binding, the formatted values to display in the user interface.
	 */
	String[] getValues();

	/**
	 * When a collection binding, sets the model property values a from user-entered/selected values.
	 * @param formattedValues the values entered by the user
	 */
	void setValues(String[] formattedValues);
	
}