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
 * A base {@link Binder binder} implementation designed for subclassing.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setRequiredFields(String[])
 * @see #bind(Map)
 */
public abstract class AbstractBinder implements Binder {

	private String[] requiredFields;

	private MessageSource messageSource;

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

	// implementing Binder

	public BindingResults bind(Map<String, ? extends Object> fieldValues) {
		fieldValues = filter(fieldValues);
		checkRequired(fieldValues);
		ArrayListBindingResults results = new ArrayListBindingResults(fieldValues.size());
		for (Map.Entry<String, ? extends Object> fieldValue : fieldValues.entrySet()) {
			results.add(bind(fieldValue));
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

	/**
	 * The configured MessageSource that resolves binding result alert messages.
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Hook method subclasses should override to perform a single binding.
	 * @param fieldValue the field value to bind
	 * @return the binding result
	 */
	protected abstract BindingResult bind(Map.Entry<String, ? extends Object> fieldValue);

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