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

import org.springframework.ui.binding.BindingConfiguration;
import org.springframework.ui.format.Formatter;

final class DefaultBindingConfiguration implements BindingConfiguration {
	
	private String propertyPath;
	
	private Formatter<?> formatter;
	
	private boolean required;
	
	/**
	 * Creates a new Binding configuration.
	 * @param property the property to bind to
	 * @param formatter the formatter to use to format property values
	 */
	public DefaultBindingConfiguration(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	// implementing BindingConfiguration
	
	public BindingConfiguration formatWith(Formatter<?> formatter) {
		this.formatter = formatter;
		return this;
	}

	public BindingConfiguration required() {
		this.required = true;
		return this;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public Formatter<?> getFormatter() {
		return formatter;
	}

	public boolean isRequired() {
		return required;
	}	
	
}
