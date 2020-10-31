/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Exception thrown when required properties are not found.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
 * @see ConfigurablePropertyResolver#validateRequiredProperties()
 * @see org.springframework.context.support.AbstractApplicationContext#prepareRefresh()
 */
@SuppressWarnings("serial")
public class MissingRequiredPropertiesException extends IllegalStateException {

	private final Set<String> missingRequiredProperties = new LinkedHashSet<>();


	void addMissingRequiredProperty(String key) {
		this.missingRequiredProperties.add(key);
	}

	@Override
	public String getMessage() {
		return "The following properties were declared as required but could not be resolved: " +
				getMissingRequiredProperties();
	}

	/**
	 * Return the set of properties marked as required but not present
	 * upon validation.
	 * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
	 * @see ConfigurablePropertyResolver#validateRequiredProperties()
	 */
	public Set<String> getMissingRequiredProperties() {
		return this.missingRequiredProperties;
	}

}
