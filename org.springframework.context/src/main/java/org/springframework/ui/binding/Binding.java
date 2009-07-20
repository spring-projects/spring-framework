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
import org.springframework.ui.alert.Severity;

/**
 * A binding between a source element and a model property.
 * @author Keith Donald
 * @since 3.0
 */
public interface Binding {

	/**
	 * The value to display in the UI.
	 * Is the formatted model value if {@link BindingStatus#CLEAN} or {@link BindingStatus#COMMITTED}.
	 * Is the formatted buffered value if {@link BindingStatus#DIRTY} or {@link BindingStatus#COMMIT_FAILURE}.
	 * Is the source value if {@link BindingStatus#INVALID_SOURCE_VALUE}.
	 */
	Object getValue();

	/**
	 * If this Binding is editable.
	 * Used to determine if the user can edit the field value.
	 * A Binding that is not editable cannot have source values applied and cannot be committed.
	 */
	boolean isEditable();
	
	/**
	 * If this Binding is enabled.
	 * Used to determine if the user can interact with the field.
	 * A Binding that is not enabled cannot have source values applied and cannot be committed.
	 */
	boolean isEnabled();
	
	/**
	 * If this Binding is visible.
	 * Used to determine if the user can see the field.
	 * A Binding that is not visible cannot have source values applied and cannot be committed. 
	 */
	boolean isVisible();
	
	/**
	 * Apply the source value to this binding.
	 * The source value is parsed and stored in the binding's value buffer.
	 * Sets to {@link BindingStatus#DIRTY} if succeeds.
	 * Sets to {@link BindingStatus#INVALID_SOURCE_VALUE} if fails.
	 * @param sourceValue
	 * @throws IllegalStateException if {@link #isEditable()}
	 */
	void applySourceValue(Object sourceValue);
	
	/**
	 * The current binding status.
	 * Initially {@link BindingStatus#CLEAN clean}.
	 * Is {@link BindingStatus#DIRTY} after applying a source value to the value buffer.
	 * Is {@link BindingStatus#COMMITTED} after successfully committing the buffered value.
	 * Is {@link BindingStatus#INVALID_SOURCE_VALUE} if a source value could not be applied.
	 * Is {@link BindingStatus#COMMIT_FAILURE} if a buffered value could not be committed.
	 */
	BindingStatus getStatus();
	
	/**
	 * An alert that communicates current status to the user.
	 * Returns <code>null</code> if {@link BindingStatus#CLEAN} and {@link ValidationStatus#NOT_VALIDATED}.
	 * Returns a {@link Severity#INFO} Alert with code <code>bindSuccess</code> when {@link BindingStatus#COMMITTED}.
	 * Returns a {@link Severity#ERROR} Alert with code <code>typeMismatch</code> when {@link BindingStatus#INVALID_SOURCE_VALUE} or {@link BindingStatus#COMMIT_FAILURE} due to a value parse / type conversion error.
	 * Returns a {@link Severity#FATAL} Alert with code <code>internalError</code> when {@link BindingStatus#COMMIT_FAILURE} due to a unexpected runtime exception.
	 * Returns a {@link Severity#INFO} Alert describing results of validation if {@link ValidationStatus#VALID} or {@link ValidationStatus#INVALID}.
	 */
	Alert getStatusAlert();
	
	/**
	 * Commit the buffered value to the model.
	 * Sets to {@link BindingStatus#COMMITTED} if succeeds.
	 * Sets to {@link BindingStatus#COMMIT_FAILURE} if fails.
	 * @throws IllegalStateException if not {@link BindingStatus#DIRTY} or {@link #isEditable()}
	 */
	void commit();
	
	/**
	 * Clear the buffered value without committing.
	 * @throws IllegalStateException if BindingStatus is CLEAN or COMMITTED.
	 */
	void revert();
	
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
	boolean isList();

	/**
	 * If a List, get a Binding to a element in the List.
	 * @param index the element index
	 * @return the indexed binding
	 */
	Binding getListElementBinding(int index);

	/**
	 * If bound to a {@link java.util.Map}.
	 */
	boolean isMap();

	/**
	 * If a Map, get a Binding to a value in the Map.
	 * @param key the map key
	 * @return the keyed binding
	 */
	Binding getMapValueBinding(Object key);

	/**
	 * Format a potential model value for display.
	 * If a list binding, expects the model value to be a potential list element & uses the configured element formatter.
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
		 * The model value.
		 */
		Object getValue();
		
		/**
		 * The model value type.
		 */
		Class<?> getValueType();		

		/**
		 * Set the model value.
		 * @throws IllegalStateException if this binding is read only
		 */
		void setValue(Object value);
	}
	
	/**
	 * Binding states.
	 * @author Keith Donald
	 */
	public enum BindingStatus {
		
		/**
		 * Initial state: No value is buffered, and there is a direct channel to the model value.
		 */
		CLEAN,
		
		/**
		 * An invalid source value is applied.
		 */
		INVALID_SOURCE_VALUE,
		
		/**
		 * The binding buffer contains a valid value that has not been committed.
		 */
		DIRTY,

		/**
		 * The buffered value has been committed.
		 */
		COMMITTED,
		
		/**
		 * The buffered value failed to commit.
		 */
		COMMIT_FAILURE
	}
	
	/**
	 * Validation states.
	 * @author Keith Donald
	 */
	public enum ValidationStatus {

		/**
		 * Initial state: No validation has run.
		 */
		NOT_VALIDATED,
		
		/**
		 * Validation has succeeded.
		 */
		VALID,
		
		/**
		 * Validation has failed.
		 */
		INVALID
	}
}