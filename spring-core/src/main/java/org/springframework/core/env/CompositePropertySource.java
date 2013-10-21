/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.env;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite {@link PropertySource} implementation that iterates over a list of
 * {@link PropertySource} instances. Necessary in cases where multiple property sources
 * share the same name, e.g. when multiple values are supplied to {@code @PropertySource}.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
public class CompositePropertySource extends PropertySource<Object> {

	private List<PropertySource<?>> propertySources = new ArrayList<PropertySource<?>>();


	/**
	 * Create a new {@code CompositePropertySource}.
	 *
	 * @param name the name of the property source
	 */
	public CompositePropertySource(String name) {
		super(name);
	}


	@Override
	public Object getProperty(String name) {
		for (PropertySource<?> propertySource : this.propertySources) {
			Object candidate = propertySource.getProperty(name);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}

	public void addPropertySource(PropertySource<?> propertySource) {
		this.propertySources.add(0, propertySource);
	}

	@Override
	public String toString() {
		return String.format("%s [name='%s', propertySources=%s]",
				this.getClass().getSimpleName(), this.name, this.propertySources);
	}
}
