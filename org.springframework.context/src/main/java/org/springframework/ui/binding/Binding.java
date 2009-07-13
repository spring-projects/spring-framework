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
 * @since 3.0
 */
public interface Binding {

	/**
	 * The name of the bound model property.
	 */
	String getProperty();

	/**
	 * The type of the underlying property associated with this binding.
	 */
	Class<?> getType();

	/**
	 * The formatted property value to display in the user interface.
	 */
	String getValue();

	/**
	 * Set the property to the value provided.
	 * The value may be a formatted String, a formatted String[] if a collection binding, or an Object of a type that can be coersed to the underlying property type.
	 * @param value the new value to bind
	 * @return a summary of the result of the binding
	 * @throws BindException if an unrecoverable exception occurs 
	 */
	BindingResult setValue(Object value);
	
	/**
	 * Formats a candidate model property value for display in the user interface.
	 * @param potentialValue a possible value
	 * @return the formatted value to display in the user interface
	 */
	String format(Object potentialValue);

	/**
	 * Is this a collection binding?
	 * If so, a client may call {@link #getCollectionValues()} to get the collection element values for display in the user interface.
	 * In this case, the client typically allocates one indexed field to each value.
	 * A client may then call {@link #setValues(String[])} to update model property values from those fields.
	 * Alternatively, a client may call {@link #getValue()} to render the collection as a single value for display in a single field, such as a large text area.
	 * The client would then call {@link #setValue(Object)} to update that single value from the field.
	 */
	boolean isCollection();

	/**
	 * When a collection binding, the formatted values to display in the user interface.
	 * If not a collection binding, throws an IllegalStateException.
	 * @throws IllegalStateException
	 */
	String[] getCollectionValues();

}