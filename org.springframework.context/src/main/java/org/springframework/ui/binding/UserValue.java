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

/**
 * Holds a user-entered value to bind to a model property.
 * @author Keith Donald
 * @see Binder#bind(List).
 */
public class UserValue {

	private String property;

	private Object value;
	
	/**
	 * Create a new user value
	 * @param property the property associated with the value
	 * @param value the actual user-entered value
	 */
	public UserValue(String property, Object value) {
		this.property = property;
		this.value = value;
	}
	
	/**
	 * The property the user-entered value should bind to.
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * The actual user-entered value.
	 */
	public Object getValue() {
		return value;
	}
	
}
