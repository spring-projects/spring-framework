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
package org.springframework.model.ui;

/**
 * Represents the state and behavior of a presentation independently of the GUI controls used in the interface.
 * Pulls the state and behavior of a view out into a model class that is part of the presentation.
 * Coordinates with the domain layer and provides an interface to the view that minimizes decision making in the view.
 * @author Keith Donald
 * @since 3.0
 */
public interface PresentationModel {

	/**
	 * Get the model for the field.
	 * @param fieldName the field name.
	 * @throws FieldNotFoundException if no such field exists
	 */
	FieldModel getFieldModel(String fieldName);

	/**
	 * Validate all fields.
	 * Skips any fields with {@link BindingStatus#INVALID_SUBMITTED_VALUE invalid submitted values}.
	 */
	void validate();
	
	/**
	 * If errors are present on this PresentationModel.
	 * Returns true if at least one FieldModel has {@link BindingStatus#INVALID_SUBMITTED_VALUE invalid submitted values} or is {@link ValidationStatus#INVALID invalid}.
	 */
	boolean hasErrors();

	/**
	 * Commit any {@link BindingStatus#DIRTY dirty} fields.
	 * @throws IllegalStateException if there are field models that have {@link BindingStatus#INVALID_SUBMITTED_VALUE invalid submitted values} or are {@link ValidationStatus#INVALID invalid}.
	 */
	void commit();

}