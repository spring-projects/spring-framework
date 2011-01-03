/*
 * Copyright 2002-2011 the original author or authors.
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

import static java.lang.String.format;

import java.util.List;
import java.util.Properties;

/**
 * {@link PropertyResolver} implementation that resolves property values against
 * an underlying set of {@link PropertySources}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

	private final PropertySources propertySources;

	/**
	 * Create a new resolver against the given property sources.
	 * @param propertySources the set of {@link PropertySource} objects to use
	 */
	public PropertySourcesPropertyResolver(PropertySources propertySources) {
		this.propertySources = propertySources;
	}


	public boolean containsProperty(String key) {
		for (PropertySource<?> propertySource : this.propertySources.asList()) {
			if (propertySource.containsProperty(key)) {
				return true;
			}
		}
		return false;
	}

	public String getProperty(String key) {
		if (logger.isTraceEnabled()) {
			logger.trace(format("getProperty(\"%s\") (implicit targetType [String])", key));
		}
		return this.getProperty(key, String.class);
	}

	public <T> T getProperty(String key, Class<T> targetValueType) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (logger.isTraceEnabled()) {
			logger.trace(format("getProperty(\"%s\", %s)", key, targetValueType.getSimpleName()));
		}
	
		for (PropertySource<?> propertySource : this.propertySources.asList()) {
			if (debugEnabled) {
				logger.debug(format("Searching for key '%s' in [%s]", key, propertySource.getName()));
			}
			if (propertySource.containsProperty(key)) {
				Object value = propertySource.getProperty(key);
				Class<?> valueType = value == null ? null : value.getClass();
				if (debugEnabled) {
					logger.debug(
							format("Found key '%s' in [%s] with type [%s] and value '%s'",
									key, propertySource.getName(),
									valueType == null ? "" : valueType.getSimpleName(), value));
				}
				if (value == null) {
					return null;
				}
				if (!this.conversionService.canConvert(valueType, targetValueType)) {
					throw new IllegalArgumentException(
							format("Cannot convert value [%s] from source type [%s] to target type [%s]",
									value, valueType.getSimpleName(), targetValueType.getSimpleName()));
				}
				return conversionService.convert(value, targetValueType);
			}
		}
	
		if (debugEnabled) {
			logger.debug(format("Could not find key '%s' in any property source. Returning [null]", key));
		}
		return null;
	}

	public Properties asProperties() {
		Properties mergedProps = new Properties();
		List<PropertySource<?>> propertySourcesList = this.propertySources.asList();
		for (int i = propertySourcesList.size() -1; i >= 0; i--) {
			PropertySource<?> source = propertySourcesList.get(i);
			for (String key : source.getPropertyNames()) {
				mergedProps.put(key, source.getProperty(key));
			}
		}
		return mergedProps;
	}

}
