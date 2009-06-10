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
package org.springframework.ui.binding.support;

import java.util.Map;

import org.springframework.ui.binding.UserValues;

/**
 * A binder designed for use in HTTP (web) environments.
 * Suited for binding user-provided HTTP query parameters to model properties.
 * @author Keith Donald
 * @param <M> The type of model object this binder binds to
 * @see #setFieldDefaultPrefix(String)
 * @see #setFieldMarkerPrefix(String)
 */
public class WebBinder<M> extends GenericBinder<M> {

	private String fieldMarkerPrefix = "_";

	private String fieldDefaultPrefix = "!";

	/**
	 * Creates a new web binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public WebBinder(M model) {
		super(model);
	}

	/**
	 * Configure the prefix for determining default field values.
	 * Default is '!'.
	 */
	public void setFieldDefaultPrefix(String fieldDefaultPrefix) {
		this.fieldDefaultPrefix = fieldDefaultPrefix;
	}

	/**
	 * Configure the prefix for determining empty fields.
	 * Default is '_'.
	 */
	public void setFieldMarkerPrefix(String fieldMarkerPrefix) {
		this.fieldMarkerPrefix = fieldMarkerPrefix;
	}

	@Override
	public UserValues createUserValues(Map<String, ? extends Object> userMap) {
		UserValues values = new UserValues();
		for (Map.Entry<String, ? extends Object> entry : userMap.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (field.startsWith(fieldDefaultPrefix)) {
				field = field.substring(fieldDefaultPrefix.length());
				if (!userMap.containsKey(field)) {
					values.add(field, value);
				}
			} else if (field.startsWith(fieldMarkerPrefix)) {
				field = field.substring(fieldMarkerPrefix.length());
				if (!userMap.containsKey(field) && !userMap.containsKey(fieldDefaultPrefix + field)) {
					value = getEmptyValue((BindingImpl) getBinding(field));
					values.add(field, value);
				}
			} else {
				values.add(entry.getKey(), entry.getValue());
			}
		}
		return values;
	}

	protected Object getEmptyValue(BindingImpl binding) {
		Class<?> type = binding.getValueType();
		if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

}
