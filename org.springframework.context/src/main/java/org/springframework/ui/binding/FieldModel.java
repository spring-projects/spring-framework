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
 * A model for a single data field containing dynamic information to display in the view.
 * @author Keith Donald
 * @since 3.0
 */
public interface FieldModel {

	/**
	 * The model value formatted for display in a single field in the UI.
	 * Is the formatted model value if {@link BindingStatus#CLEAN} or {@link BindingStatus#COMMITTED}.
	 * Is the formatted buffered value if {@link BindingStatus#DIRTY} or {@link BindingStatus#COMMIT_FAILURE}.
	 */
	String getRenderValue();

	/**
	 * The field model value.
	 */
	Object getValue();
	
	/**
	 * The field model value type.
	 */
	Class<?> getValueType();	
	
	/**
	 * If editable.
	 * Used to determine if the user can edit the field value.
	 * A Binding that is not editable cannot have submitted values applied and cannot be committed.
	 */
	boolean isEditable();
	
	/**
	 * If enabled.
	 * Used to determine if the user can interact with the field at all.
	 * A Binding that is not enabled cannot have submitted values applied and cannot be committed.
	 */
	boolean isEnabled();
	
	/**
	 * If visible.
	 * Used to determine if the user can see the field.
	 */
	boolean isVisible();

	/**
	 * The current field binding status.
	 * Initially {@link BindingStatus#CLEAN clean}.
	 * Is {@link BindingStatus#DIRTY} after applying a submitted value to the value buffer.
	 * Is {@link BindingStatus#COMMITTED} after successfully committing the buffered value.
	 * Is {@link BindingStatus#INVALID_SUBMITTED_VALUE} if a submitted value could not be applied.
	 * Is {@link BindingStatus#COMMIT_FAILURE} if a buffered value could not be committed.
	 */
	BindingStatus getBindingStatus();

	/**
	 * The current field validation status.
	 * Initially {@link ValidationStatus#NOT_VALIDATED}.
	 * Is {@link ValidationStatus#VALID} after value is successfully validated.
	 * Is {@link ValidationStatus#INVALID} after value fails validation.
	 * Resets to {@value ValidationStatus#NOT_VALIDATED} when value changes.
	 */
	ValidationStatus getValidationStatus();
	
	/**
	 * An alert that communicates current FieldModel status to the user.
	 * Returns <code>null</code> if {@link BindingStatus#CLEAN} and {@link ValidationStatus#NOT_VALIDATED}.
	 * Returns a {@link Severity#INFO} Alert with code <code>bindSuccess</code> when {@link BindingStatus#COMMITTED}.
	 * Returns a {@link Severity#ERROR} Alert with code <code>typeMismatch</code> when {@link BindingStatus#INVALID_SUBMITTED_VALUE} or {@link BindingStatus#COMMIT_FAILURE} due to a value parse / type conversion error.
	 * Returns a {@link Severity#FATAL} Alert with code <code>internalError</code> when {@link BindingStatus#COMMIT_FAILURE} due to a unexpected runtime exception.
	 * Returns a {@link Severity#INFO} Alert describing results of validation if {@link ValidationStatus#VALID} or {@link ValidationStatus#INVALID}.
	 */
	Alert getStatusAlert();
	
	/**
	 * Apply a submitted value to this FieldModel.
	 * The submitted value is parsed and stored in the value buffer.
	 * Sets to {@link BindingStatus#DIRTY} if succeeds.
	 * Sets to {@link BindingStatus#INVALID_SUBMITTED_VALUE} if fails.
	 * @param submittedValue
	 * @throws IllegalStateException if not editable or not enabled
	 */
	void applySubmittedValue(Object submittedValue);
	
	/**
	 * If {@link BindingStatus#INVALID_SUBMITTED_VALUE}, returns the invalid submitted value.
	 * Returns null otherwise.
	 * @return the invalid submitted value
	 */
	Object getInvalidSubmittedValue();
	
	/**
	 * Validate the model value.
	 * Sets to {@link ValidationStatus#VALID} if succeeds.
	 * Sets to {@link ValidationStatus#INVALID} if fails.
	 */
	void validate();
	
	/**
	 * Commit the buffered value to the model.
	 * Sets to {@link BindingStatus#COMMITTED} if succeeds.
	 * Sets to {@link BindingStatus#COMMIT_FAILURE} if fails.
	 * @throws IllegalStateException if not editable, not enabled, or not dirty
	 */
	void commit();
	
	/**
	 * Clear the buffered value without committing.
	 * @throws IllegalStateException if BindingStatus is CLEAN or COMMITTED.
	 */
	void revert();

	/**
	 * Get a model for a nested field.
	 * @param fieldName the nested field name, such as "foo"; should not be a property path like "foo.bar"
	 * @return the nested field model
	 * @throws IllegalStateException if {@link #isList()}
	 * @throws FieldNotFoundException if no such nested field exists
	 */
	FieldModel getNested(String fieldName);

	/**
	 * If an indexable {@link java.util.List} or array.
	 */
	boolean isList();

	/**
	 * If {@link #isList()}, get a FieldModel for a element in the list..
	 * @param index the element index
	 * @return the indexed binding
	 * @throws IllegalStateException if not a list
	 */
	FieldModel getListElement(int index);

	/**
	 * If a Map.
	 */
	boolean isMap();

	/**
	 * If {@link #isMap()}, get FieldModel for a value in the Map.
	 * @param key the map key
	 * @return the keyed binding
	 * @throws IllegalStateException if not a map 
	 */
	FieldModel getMapValue(Object key);

	/**
	 * Format a potential model value for display.
	 * If {@link #isList()}, expects the value to be a potential list element & uses the configured element formatter.
	 * If {@link #isMap()}, expects the value to be a potential map value & uses the configured map value formatter.
	 * @param potentialValue the potential value
	 * @return the formatted string
	 */
	String formatValue(Object potentialValue);
}