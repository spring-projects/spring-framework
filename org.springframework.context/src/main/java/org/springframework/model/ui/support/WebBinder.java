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
package org.springframework.model.ui.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.model.ui.FieldModel;
import org.springframework.model.ui.PresentationModel;

/**
 * A binder designed for use in HTTP (web) environments.
 * Suited for binding user-provided HTTP query parameters to model properties.
 * @author Keith Donald
 * @since 3.0
 * @see #setDefaultPrefix(String)
 * @see #setPresentPrefix(String)
 */
public class WebBinder extends PresentationModelBinder {

	private String defaultPrefix = "!";

	private String presentPrefix = "_";

	/**
	 * Creates a new web binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public WebBinder(PresentationModel bindingFactory) {
		super(bindingFactory);
	}

	/**
	 * Configure the prefix used to detect the default value for a field when no value is submitted.
	 * Default is '!'.
	 */
	public void setDefaultPrefix(String defaultPrefix) {
		this.defaultPrefix = defaultPrefix;
	}

	/**
	 * Configure the prefix used to detect the presence of a field on the web UI when no value was actually submitted.
	 * This is used to configure a <i>empty</i> field when no other {@link #setDefaultPrefix(String) default value} is specified by the client.
	 * Default is '_'.
	 */
	public void setPresentPrefix(String presentPrefix) {
		this.presentPrefix = presentPrefix;
	}

	@Override
	protected Map<String, ? extends Object> filter(Map<String, ? extends Object> fieldValues) {
		LinkedHashMap<String, Object> filteredValues = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, ? extends Object> entry : fieldValues.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (field.startsWith(defaultPrefix)) {
				field = field.substring(defaultPrefix.length());
				if (!fieldValues.containsKey(field)) {
					filteredValues.put(field, value);
				}
			} else if (field.startsWith(presentPrefix)) {
				field = field.substring(presentPrefix.length());
				if (!fieldValues.containsKey(field) && !fieldValues.containsKey(defaultPrefix + field)) {
					value = getEmptyValue(getFieldModel(field));
					filteredValues.put(field, value);
				}
			} else {
				filteredValues.put(entry.getKey(), entry.getValue());
			}
		}
		return filteredValues;
	}

	protected Object getEmptyValue(FieldModel binding) {
		Class<?> type = binding.getValueType();
		if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

}
