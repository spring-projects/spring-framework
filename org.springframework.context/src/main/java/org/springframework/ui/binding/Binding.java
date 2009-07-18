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

import org.springframework.ui.alert.Alert;

/**
 * A binding between a source element and a model property.
 * @author Keith Donald
 * @since 3.0
 */
public interface Binding {

	/**
	 * The bound value to display in the UI.
	 * Is the formatted model value if not dirty.
	 * Is the buffered value if dirty.
	 */
	Object getValue();

	/**
	 * If this Binding is read-only.
	 * A read-only Binding cannot have source values applied and cannot be committed.
	 */
	boolean isReadOnly();
	
	/**
	 * Apply the source value to this binding.
	 * The source value is parsed, validated, and stored in the binding's value buffer.
	 * Sets 'dirty' status to true.
	 * Sets 'valid' status to false if the source value is not valid.
	 * @param sourceValue
	 * @throws IllegalStateException if read only
	 */
	void applySourceValue(Object sourceValue);
	
	/**
	 * True if there is an uncommitted value in the binding buffer.
	 * Set to true after applying a source value.
	 * Set to false after a commit.
	 */
	boolean isDirty();
	
	/**
	 * False if dirty and the buffered value is invalid.
	 * False if dirty and the buffered value appears valid but could not be committed.
	 * True otherwise.
	 */
	boolean isValid();
	
	/**
	 * Commit the buffered value to the model.
	 * @throws IllegalStateException if not dirty, not valid, or read-only
	 */
	void commit();

	/**
	 * An Alert that communicates the current status of this Binding.
	 */
	Alert getStatusAlert();
	
	/**
	 * Access raw model values.
	 */
	Model getModel();
	
	/**
	 * Get a Binding to a nested property value.
	 * @param nestedProperty the nested property name, such as "foo"; should not be a property path like "foo.bar"
	 * @return the binding to the nested property
	 */
	Binding getBinding(String nestedProperty);

	/**
	 * If bound to an indexable Collection, either a {@link java.util.List} or an array.
	 */
	boolean isIndexable();

	/**
	 * If a List, get a Binding to a element in the List.
	 * @param index the element index
	 * @return the indexed binding
	 */
	Binding getIndexedBinding(int index);

	/**
	 * If bound to a {@link java.util.Map}.
	 */
	boolean isMap();

	/**
	 * If a Map, get a Binding to a value in the Map.
	 * @param key the map key
	 * @return the keyed binding
	 */
	Binding getKeyedBinding(Object key);

	/**
	 * Format a potential model value for display.
	 * If an indexable binding, expects the model value to be a potential collection element & uses the configured element formatter.
	 * If a map binding, expects the model value to be a potential map value & uses the configured map value formatter.
	 * @param potentialValue the potential value
	 * @return the formatted string
	 */
	String formatValue(Object potentialModelValue);
	
	/**
	 * For accessing the raw bound model object.
	 * @author Keith Donald
	 */
	public interface Model {
		
		/**
		 * Read the raw model value.
		 */
		Object getValue();
		
		/**
		 * The model value type.
		 */
		Class<?> getValueType();		

		/**
		 * Set the model value.
		 * @throws IllegalStateException if this binding is read-only
		 */
		void setValue(Object value);
	}
}