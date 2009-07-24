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
package org.springframework.ui.binding.binder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.core.convert.TypeConverter;
import org.springframework.ui.binding.FieldModel;
import org.springframework.ui.binding.FieldNotFoundException;
import org.springframework.ui.binding.PresentationModel;
import org.springframework.ui.binding.BindingStatus;
import org.springframework.util.Assert;

/**
 * A generic {@link Binder binder} suitable for use in most environments.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setRequiredFields(String[])
 * @see #bind(Map)
 */
public class GenericBinder implements Binder {

	private PresentationModel presentationModel;
	
	private String[] requiredFields;
	
	private MessageSource messageSource;

	public GenericBinder(PresentationModel presentationModel) {
		Assert.notNull(presentationModel, "The PresentationModel is required");
		this.presentationModel = presentationModel;
	}

	/**
	 * Configure the MessageSource that resolves localized {@link BindingResult} alert messages.
	 * @param messageSource the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		Assert.notNull(messageSource, "The MessageSource is required");
		this.messageSource = messageSource;
	}
	
	/**
	 * Configure the fields for which values must be present in each bind attempt.
	 * @param fieldNames the field names
	 * @see MissingFieldException
	 */
	public void setRequiredFields(String[] fieldNames) {
		this.requiredFields = fieldNames;
	}
	
	// subclassing hooks
	
	/**
	 * Get the model for the field.
	 * @param fieldName
	 * @return the field model
	 * @throws NoSuchFieldException if no such field exists
	 */
	protected FieldModel getFieldModel(String fieldName) {
		return presentationModel.getFieldModel(fieldName);
	}

	// implementing Binder

	public BindingResults bind(Map<String, ? extends Object> fieldValues) {
		fieldValues = filter(fieldValues);
		checkRequired(fieldValues);
		ArrayListBindingResults results = new ArrayListBindingResults(fieldValues.size());
		for (Map.Entry<String, ? extends Object> fieldValue : fieldValues.entrySet()) {
			try {
				FieldModel field = getFieldModel(fieldValue.getKey());
				results.add(bind(fieldValue, field));
			} catch (FieldNotFoundException e) {
				results.add(new FieldNotFoundResult(fieldValue.getKey(), fieldValue.getValue(), messageSource));
			}
		}
		return results;
	}

	// subclassing hooks

	/**
	 * Hook subclasses may use to filter the source values to bind.
	 * This hook allows the binder to pre-process the field values before binding occurs.
	 * For example, a Binder might insert empty or default values for fields that are not present.
	 * As another example, a Binder might collapse multiple source values into a single source value. 
	 * @param fieldValues the original fieldValues map provided by the caller
	 * @return the filtered fieldValues map that will be used to bind
	 */
	protected Map<String, ? extends Object> filter(Map<String, ? extends Object> fieldValues) {
		return fieldValues;
	}

	// internal helpers

	private void checkRequired(Map<String, ? extends Object> fieldValues) {
		if (requiredFields == null) {
			return;
		}
		List<String> missingRequired = new ArrayList<String>();
		for (String required : requiredFields) {
			boolean found = false;
			for (String property : fieldValues.keySet()) {
				if (property.equals(required)) {
					found = true;
				}
			}
			if (!found) {
				missingRequired.add(required);
			}
		}
		if (!missingRequired.isEmpty()) {
			throw new MissingFieldException(missingRequired, fieldValues);
		}
	}

	private BindingResult bind(Map.Entry<String, ? extends Object> fieldValue, FieldModel field) {
		String fieldName = fieldValue.getKey();
		Object value = fieldValue.getValue();
		if (!field.isEditable()) {
			return new FieldNotEditableResult(fieldName, value, messageSource);
		} else {
			field.applySubmittedValue(value);
			if (field.getBindingStatus() == BindingStatus.DIRTY) {
				field.commit();
			}
			return new BindingStatusResult(fieldName, value, field.getStatusAlert());
		}
	}
	
}