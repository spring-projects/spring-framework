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

import org.springframework.ui.binding.UserValue;
import org.springframework.ui.binding.UserValues;

/**
 * A binder designed for use in HTTP (web) environments.
 * Suited for binding user-provided HTTP query parameters to model properties.
 * @author Keith Donald
 * @see #setDefaultPrefix(String)
 * @see #setPresentPrefix(String)
 */
public class WebBinder extends GenericBinder {

	private String defaultPrefix = "!";

	private String presentPrefix = "_";

	/**
	 * Creates a new web binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public WebBinder(Object model) {
		super(model);
	}

	/**
	 * Configure the prefix to detect the default user value for a property when no value was submitted.
	 * This is used to configure a <i>default</i> {@link UserValue} for binding when no value is submitted by the client.
	 * Default is '!'.
	 */
	public void setDefaultPrefix(String defaultPrefix) {
		this.defaultPrefix = defaultPrefix;
	}

	/**
	 * Configure the prefix to detect the presence of a property on the web UI when no user value for the property was actually submitted.
	 * This is used to configure a <i>empty</i> {@link UserValue} for binding when no other {@link #setDefaultPrefix(String) default value} is specified by the client.
	 * Default is '_'.
	 */
	public void setPresentPrefix(String presentPrefix) {
		this.presentPrefix = presentPrefix;
	}

	@Override
	public UserValues createUserValues(Map<String, ? extends Object> userMap) {
		UserValues values = new UserValues();
		for (Map.Entry<String, ? extends Object> entry : userMap.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (field.startsWith(defaultPrefix)) {
				field = field.substring(defaultPrefix.length());
				if (!userMap.containsKey(field)) {
					values.add(field, value);
				}
			} else if (field.startsWith(presentPrefix)) {
				field = field.substring(presentPrefix.length());
				if (!userMap.containsKey(field) && !userMap.containsKey(defaultPrefix + field)) {
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
