/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.env;

import org.springframework.core.convert.ConversionException;
import org.springframework.util.ClassUtils;

/**
 * {@link PropertyResolver} implementation that resolves property values against
 * an underlying set of {@link PropertySources}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

	protected final PropertySources propertySources;


	/**
	 * Create a new resolver against the given property sources.
	 * @param propertySources the set of {@link PropertySource} objects to use
	 */
	public PropertySourcesPropertyResolver(PropertySources propertySources) {
		this.propertySources = propertySources;
	}


	@Override
	public boolean containsProperty(String key) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (propertySource.containsProperty(key)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("getProperty(\"%s\", %s)", key, targetValueType.getSimpleName()));
		}
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (debugEnabled) {
					logger.debug(String.format("Searching for key '%s' in [%s]", key, propertySource.getName()));
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					Class<?> valueType = value.getClass();
					if (resolveNestedPlaceholders && value instanceof String) {
						value = resolveNestedPlaceholders((String) value);
					}
					if (debugEnabled) {
						logger.debug(String.format("Found key '%s' in [%s] with type [%s] and value '%s'",
								key, propertySource.getName(), valueType.getSimpleName(), value));
					}
					if (!this.conversionService.canConvert(valueType, targetValueType)) {
						throw new IllegalArgumentException(String.format(
								"Cannot convert value [%s] from source type [%s] to target type [%s]",
								value, valueType.getSimpleName(), targetValueType.getSimpleName()));
					}
					return this.conversionService.convert(value, targetValueType);
				}
			}
		}
		if (debugEnabled) {
			logger.debug(String.format("Could not find key '%s' in any property source. Returning [null]", key));
		}
		return null;
	}

	@Override
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetValueType) {
		boolean debugEnabled = logger.isDebugEnabled();
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("getPropertyAsClass(\"%s\", %s)", key, targetValueType.getSimpleName()));
		}
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (debugEnabled) {
					logger.debug(String.format("Searching for key '%s' in [%s]", key, propertySource.getName()));
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					if (debugEnabled) {
						logger.debug(String.format("Found key '%s' in [%s] with value '%s'", key, propertySource.getName(), value));
					}
					Class<?> clazz;
					if (value instanceof String) {
						try {
							clazz = ClassUtils.forName((String) value, null);
						}
						catch (Exception ex) {
							throw new ClassConversionException((String) value, targetValueType, ex);
						}
					}
					else if (value instanceof Class) {
						clazz = (Class<?>)value;
					}
					else {
						clazz = value.getClass();
					}
					if (!targetValueType.isAssignableFrom(clazz)) {
						throw new ClassConversionException(clazz, targetValueType);
					}
					@SuppressWarnings("unchecked")
					Class<T> targetClass = (Class<T>) clazz;
					return targetClass;
				}
			}
		}
		if (debugEnabled) {
			logger.debug(String.format("Could not find key '%s' in any property source. Returning [null]", key));
		}
		return null;
	}


	@SuppressWarnings("serial")
	private static class ClassConversionException extends ConversionException {

		public ClassConversionException(Class<?> actual, Class<?> expected) {
			super(String.format("Actual type %s is not assignable to expected type %s", actual.getName(), expected.getName()));
		}

		public ClassConversionException(String actual, Class<?> expected, Exception ex) {
			super(String.format("Could not find/load class %s during attempt to convert to %s", actual, expected.getName()), ex);
		}
	}

}
