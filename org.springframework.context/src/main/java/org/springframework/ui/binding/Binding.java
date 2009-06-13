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

	/**
	 * The formatted value to display in the user interface.
	 */
	String getValue();

	/**
	 * Set the property associated with this binding to the value provided.
	 * The value may be a formatted String, a formatted String[] if a collection binding, or an Object of a type that can be coersed to the underlying property type.
	 * @param value the new value to bind
	 */
	BindingResult setValue(Object value);
	
	/**
	 * Formats a candidate model property value for display in the user interface.
	 * @param selectableValue a possible value
	 * @return the formatted value to display in the user interface
	 */
	String format(Object selectableValue);

	/**
	 * Is this binding associated with a collection or array property?
	 * If so, a client should call {@link #getCollectionValues()} to display property values in the user interface.
	 * A client should call {@link #setValues(String[])} to set model property values from user-entered/selected values.
	 */
	boolean isCollection();

	/**
	 * When a collection binding, the formatted values to display in the user interface.
	 */
	String[] getCollectionValues();

	/**
	 * The type of the underlying property associated with this binding.
	 */
	Class<?>getType();


}