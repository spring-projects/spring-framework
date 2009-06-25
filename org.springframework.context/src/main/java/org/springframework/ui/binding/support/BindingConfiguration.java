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

import org.springframework.ui.binding.Binding;
import org.springframework.ui.format.Formatter;

/**
 * Configuration used to create a new {@link Binding} registered with a {@link GenericBinder}.
 * @author Keith Donald
 * @since 3.0
 * @see GenericBinder#configureBinding(BindingConfiguration)
 */
public class BindingConfiguration {
	
	private String property;
	
	private Formatter<?> formatter;
	
	/**
	 * Creates a new Binding configuration.
	 * @param property the property to bind to
	 * @param formatter the formatter to use to format property values
	 */
	public BindingConfiguration(String property, Formatter<?> formatter) {
		this.property = property;
		this.formatter = formatter;
	}

	/**
	 * The name of the model property to bind to.
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * The Formatter to use to format bound property values.
	 */
	public Formatter<?> getFormatter() {
		return formatter;
	}

}
