/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env;

import org.jspecify.annotations.Nullable;

/**
 * {@link PropertyResolver} implementation that resolves property values against
 * an underlying set of {@link PropertySources}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

	private final @Nullable PropertySources propertySources;


	/**
	 * Create a new resolver against the given property sources.
	 * @param propertySources the set of {@link PropertySource} objects to use
	 */
	public PropertySourcesPropertyResolver(@Nullable PropertySources propertySources) {
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
	public @Nullable String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	public <T> @Nullable T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	protected @Nullable String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	protected <T> @Nullable T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for key '" + key + "' in PropertySource '" +
							propertySource.getName() + "'");
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					if (resolveNestedPlaceholders) {
						if (value instanceof String string) {
							value = resolveNestedPlaceholders(string);
						}
						else if ((value instanceof CharSequence cs) && (String.class.equals(targetValueType) ||
								CharSequence.class.equals(targetValueType))) {
							value = resolveNestedPlaceholders(cs.toString());
						}
					}
					logKeyFound(key, propertySource, value);
					return convertValueIfNecessary(value, targetValueType);
				}
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Could not find key '" + key + "' in any property source");
		}
		return null;
	}

	/**
	 * Log the given key as found in the given {@link PropertySource}, resulting in
	 * the given value.
	 * <p>The default implementation writes a debug log message with key and source.
	 * As of 4.3.3, this does not log the value anymore in order to avoid accidental
	 * logging of sensitive settings. Subclasses may override this method to change
	 * the log level and/or log message, including the property's value if desired.
	 * @param key the key found
	 * @param propertySource the {@code PropertySource} that the key has been found in
	 * @param value the corresponding value
	 * @since 4.3.1
	 */
	protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
		if (logger.isDebugEnabled()) {
			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
					"' with value of type " + value.getClass().getSimpleName());
		}
	}

}
