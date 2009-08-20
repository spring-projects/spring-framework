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
package org.springframework.model.binder.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResult;
import org.springframework.model.binder.BindingResults;
import org.springframework.model.binder.MissingFieldException;
import org.springframework.util.Assert;

/**
 * Base Binder implementation that defines common structural elements.
 * Subclasses should be parameterized & implement {@link #bind(Map, Object)}.
 * @author Keith Donald
 * @since 3.0
 * @see #setRequiredFields(String[])
 * @see #setMessageSource(MessageSource)
 * @see #createFieldBinder()
 * @see #bind(Map, Object)
 */
public abstract class AbstractBinder<M> implements Binder<M> {

	private MessageSource messageSource;

	private String[] requiredFields;

	/**
	 * Configure the fields for which values must be present in each bind attempt.
	 * @param fieldNames the required field names
	 * @see MissingFieldException
	 */
	public void setRequiredFields(String[] fieldNames) {
		this.requiredFields = fieldNames;
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
	 * The configured MessageSource that resolves binding result alert messages.
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	// Binder implementation

	public final BindingResults bind(Map<String, ? extends Object> fieldValues, M model) {
		fieldValues = filter(fieldValues, model);
		checkRequired(fieldValues);
		FieldBinder fieldBinder = createFieldBinder(model);
		ArrayListBindingResults results = new ArrayListBindingResults(fieldValues.size());
		for (Map.Entry<String, ? extends Object> fieldValue : fieldValues.entrySet()) {
			results.add(fieldBinder.bind(fieldValue.getKey(), fieldValue.getValue()));
		}
		return results;
	}

	// subclass hooks

	/**
	 * Subclasses must implement this method to create the {@link FieldBinder}
	 * instance for the given model. 
	 */
	protected abstract FieldBinder createFieldBinder(M model);

	/**
	 * Filter the fields to bind.
	 * Allows for pre-processing the fieldValues Map before any binding occurs.
	 * For example, you might insert empty or default values for fields that are not present.
	 * As another example, you might collapse multiple fields into a single field.
	 * Default implementation simply returns the fieldValues Map unchanged. 
	 * @param fieldValues the original fieldValues Map provided by the caller
	 * @return the filtered fieldValues Map that will be used to bind
	 */
	protected Map<String, ? extends Object> filter(Map<String, ? extends Object> fieldValues, M model) {
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

}
