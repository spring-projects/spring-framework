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
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingFactory;
import org.springframework.ui.binding.Binding.BindingStatus;
import org.springframework.ui.binding.support.PropertyNotFoundException;
import org.springframework.util.Assert;

/**
 * A generic {@link Binder binder} suitable for use in most environments.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setTypeConverter(TypeConverter)
 * @see #bind(Map)
 */
public class GenericBinder implements Binder {

	private BindingFactory bindingFactory;
	
	private String[] requiredProperties;
	
	private MessageSource messageSource;

	public GenericBinder(BindingFactory bindingFactory) {
		Assert.notNull(bindingFactory, "The BindingFactory is required");
		this.bindingFactory = bindingFactory;
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
	 * Configure the properties for which source values must be present in each bind attempt.
	 * @param propertyPaths the property path expressions
	 * @see MissingSourceValuesException
	 */
	public void setRequired(String[] propertyPaths) {
		this.requiredProperties = propertyPaths;
	}
	
	public Object getModel() {
		return bindingFactory.getModel();
	}
	
	protected Binding getBinding(String property) {
		return bindingFactory.getBinding(property);
	}

	// implementing Binder

	public BindingResults bind(Map<String, ? extends Object> sourceValues) {
		sourceValues = filter(sourceValues);
		checkRequired(sourceValues);
		ArrayListBindingResults results = new ArrayListBindingResults(sourceValues.size());
		for (Map.Entry<String, ? extends Object> sourceValue : sourceValues.entrySet()) {
			try {
				Binding binding = getBinding(sourceValue.getKey());
				results.add(bind(sourceValue, binding));
			} catch (PropertyNotFoundException e) {
				results.add(new PropertyNotFoundResult(sourceValue.getKey(), sourceValue.getValue(), messageSource));
			}
		}
		return results;
	}

	// subclassing hooks

	/**
	 * Hook subclasses may use to filter the source values to bind.
	 * This hook allows the binder to pre-process the source values before binding occurs.
	 * For example, a Binder might insert empty or default values for fields that are not present.
	 * As another example, a Binder might collapse multiple source values into a single source value. 
	 * @param sourceValues the original source values map provided by the caller
	 * @return the filtered source values map that will be used to bind
	 */
	protected Map<String, ? extends Object> filter(Map<String, ? extends Object> sourceValues) {
		return sourceValues;
	}

	// internal helpers

	private void checkRequired(Map<String, ? extends Object> sourceValues) {
		if (requiredProperties == null) {
			return;
		}
		List<String> missingRequired = new ArrayList<String>();
		for (String required : requiredProperties) {
			boolean found = false;
			for (String property : sourceValues.keySet()) {
				if (property.equals(required)) {
					found = true;
				}
			}
			if (!found) {
				missingRequired.add(required);
			}
		}
		if (!missingRequired.isEmpty()) {
			throw new MissingSourceValuesException(missingRequired, sourceValues);
		}
	}

	private BindingResult bind(Map.Entry<String, ? extends Object> sourceValue, Binding binding) {
		String property = sourceValue.getKey();
		Object value = sourceValue.getValue();
		if (!binding.isEditable()) {
			return new PropertyNotEditableResult(property, value, messageSource);
		} else {
			binding.applySourceValue(value);
			if (binding.getStatus() == BindingStatus.DIRTY) {
				binding.commit();
			}
			return new BindingStatusResult(property, value, binding.getStatusAlert());
		}
	}
	
}