/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * Abstract base class for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

	/**
	 * JVM system property used to change the <em>default</em> escape character
	 * for property placeholder support: {@value}.
	 * <p>To configure a custom escape character, supply a string containing a
	 * single character (other than {@link Character#MIN_VALUE}). For example,
	 * supplying the following JVM system property via the command line sets the
	 * default escape character to {@code '@'}.
	 * <pre style="code">-Dspring.placeholder.escapeCharacter.default=@</pre>
	 * <p>To disable escape character support, set the value to an empty string
	 * &mdash; for example, by supplying the following JVM system property via
	 * the command line.
	 * <pre style="code">-Dspring.placeholder.escapeCharacter.default=</pre>
	 * <p>If the property is not set, {@code '\'} will be used as the default
	 * escape character.
	 * <p>May alternatively be configured via a
	 * {@link org.springframework.core.SpringProperties spring.properties} file
	 * in the root of the classpath.
	 * @since 6.2.7
	 * @see #getDefaultEscapeCharacter()
	 */
	public static final String DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME =
			"spring.placeholder.escapeCharacter.default";

	/**
	 * Since {@code null} is a valid value for {@link #defaultEscapeCharacter},
	 * this constant provides a way to represent an undefined (or not yet set)
	 * value. Consequently, {@link #getDefaultEscapeCharacter()} prevents the use
	 * of {@link Character#MIN_VALUE} as the actual escape character.
	 * @since 6.2.7
	 */
	static final Character UNDEFINED_ESCAPE_CHARACTER = Character.MIN_VALUE;


	/**
	 * Cached value for the default escape character.
	 * @since 6.2.7
	 */
	static volatile @Nullable Character defaultEscapeCharacter = UNDEFINED_ESCAPE_CHARACTER;


	protected final Log logger = LogFactory.getLog(getClass());

	private volatile @Nullable ConfigurableConversionService conversionService;

	private @Nullable PropertyPlaceholderHelper nonStrictHelper;

	private @Nullable PropertyPlaceholderHelper strictHelper;

	private boolean ignoreUnresolvableNestedPlaceholders = false;

	private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

	private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

	private @Nullable String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

	private @Nullable Character escapeCharacter = getDefaultEscapeCharacter();

	private final Set<String> requiredProperties = new LinkedHashSet<>();


	@Override
	public ConfigurableConversionService getConversionService() {
		// Need to provide an independent DefaultConversionService, not the
		// shared DefaultConversionService used by PropertySourcesPropertyResolver.
		ConfigurableConversionService cs = this.conversionService;
		if (cs == null) {
			synchronized (this) {
				cs = this.conversionService;
				if (cs == null) {
					cs = new DefaultConversionService();
					this.conversionService = cs;
				}
			}
		}
		return cs;
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * {@inheritDoc}
	 * <p>The default is <code>"${"</code>.
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 */
	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * {@inheritDoc}
	 * <p>The default is <code>"}"</code>.
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 */
	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * {@inheritDoc}
	 * <p>The default is {@code ":"}.
	 * @see SystemPropertyUtils#VALUE_SEPARATOR
	 */
	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * {@inheritDoc}
	 * <p>The default is determined by {@link #getDefaultEscapeCharacter()}.
	 * @since 6.2
	 */
	@Override
	public void setEscapeCharacter(@Nullable Character escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	/**
	 * Set whether to throw an exception when encountering an unresolvable placeholder
	 * nested within the value of a given property. A {@code false} value indicates strict
	 * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
	 * that unresolvable nested placeholders should be passed through in their unresolved
	 * ${...} form.
	 * <p>The default is {@code false}.
	 * @since 3.2
	 */
	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		Collections.addAll(this.requiredProperties, requiredProperties);
	}

	@Override
	public void validateRequiredProperties() {
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		for (String key : this.requiredProperties) {
			if (getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

	@Override
	public boolean containsProperty(String key) {
		return (getProperty(key) != null);
	}

	@Override
	public @Nullable String getProperty(String key) {
		return getProperty(key, String.class);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value != null ? value : defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		T value = getProperty(key, targetType);
		return (value != null ? value : defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public String resolvePlaceholders(String text) {
		if (this.nonStrictHelper == null) {
			this.nonStrictHelper = createPlaceholderHelper(true);
		}
		return doResolvePlaceholders(text, this.nonStrictHelper);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		if (this.strictHelper == null) {
			this.strictHelper = createPlaceholderHelper(false);
		}
		return doResolvePlaceholders(text, this.strictHelper);
	}

	/**
	 * Resolve placeholders within the given string, deferring to the value of
	 * {@link #setIgnoreUnresolvableNestedPlaceholders} to determine whether any
	 * unresolvable placeholders should raise an exception or be ignored.
	 * <p>Invoked from {@link #getProperty} and its variants, implicitly resolving
	 * nested placeholders. In contrast, {@link #resolvePlaceholders} and
	 * {@link #resolveRequiredPlaceholders} do <i>not</i> delegate
	 * to this method but rather perform their own handling of unresolvable
	 * placeholders, as specified by each of those methods.
	 * @since 3.2
	 * @see #setIgnoreUnresolvableNestedPlaceholders
	 */
	protected String resolveNestedPlaceholders(String value) {
		if (value.isEmpty()) {
			return value;
		}
		return (this.ignoreUnresolvableNestedPlaceholders ?
				resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
	}

	private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
		return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
				this.valueSeparator, this.escapeCharacter, ignoreUnresolvablePlaceholders);
	}

	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		return helper.replacePlaceholders(text, this::getPropertyAsRawString);
	}

	/**
	 * Convert the given value to the specified target type, if necessary.
	 * @param value the original property value
	 * @param targetType the specified target type for property retrieval
	 * @return the converted value, or the original value if no conversion
	 * is necessary
	 * @since 4.3.5
	 */
	@SuppressWarnings("unchecked")
	protected <T> @Nullable T convertValueIfNecessary(Object value, @Nullable Class<T> targetType) {
		if (targetType == null) {
			return (T) value;
		}
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			// Avoid initialization of shared DefaultConversionService if
			// no standard type conversion is needed in the first place...
			if (ClassUtils.isAssignableValue(targetType, value)) {
				return (T) value;
			}
			conversionServiceToUse = DefaultConversionService.getSharedInstance();
		}
		return conversionServiceToUse.convert(value, targetType);
	}


	/**
	 * Retrieve the specified property as a raw String,
	 * i.e. without resolution of nested placeholders.
	 * @param key the property name to resolve
	 * @return the property value or {@code null} if none found
	 */
	protected abstract @Nullable String getPropertyAsRawString(String key);


	/**
	 * Get the default {@linkplain #setEscapeCharacter(Character) escape character}
	 * to use when parsing strings for property placeholder resolution.
	 * <p>This method attempts to retrieve the default escape character configured
	 * via the {@value #DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME} JVM system
	 * property or Spring property.
	 * <p>Falls back to {@code '\'} if the property has not been set.
	 * @return the configured default escape character, {@code null} if escape character
	 * support has been disabled, or {@code '\'} if the property has not been set
	 * @throws IllegalArgumentException if the property is configured with an
	 * invalid value, such as {@link Character#MIN_VALUE} or a string containing
	 * more than one character
	 * @since 6.2.7
	 * @see #DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME
	 * @see SystemPropertyUtils#ESCAPE_CHARACTER
	 * @see SpringProperties
	 */
	public static @Nullable Character getDefaultEscapeCharacter() throws IllegalArgumentException {
		Character escapeCharacter = defaultEscapeCharacter;
		if (UNDEFINED_ESCAPE_CHARACTER.equals(escapeCharacter)) {
			String value = SpringProperties.getProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);
			if (value != null) {
				if (value.isEmpty()) {
					// Disable escape character support by default.
					escapeCharacter = null;
				}
				else if (value.length() == 1) {
					try {
						// Use custom default escape character.
						escapeCharacter = value.charAt(0);
					}
					catch (Exception ex) {
						throw new IllegalArgumentException("Failed to process value [%s] for property [%s]: %s"
								.formatted(value, DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, ex.getMessage()), ex);
					}
					Assert.isTrue(!escapeCharacter.equals(Character.MIN_VALUE),
							() -> "Value for property [%s] must not be Character.MIN_VALUE"
									.formatted(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME));
				}
				else {
					throw new IllegalArgumentException(
							"Value [%s] for property [%s] must be a single character or an empty string"
									.formatted(value, DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME));
				}
			}
			else {
				// Use standard default value for the escape character.
				escapeCharacter = SystemPropertyUtils.ESCAPE_CHARACTER;
			}
			defaultEscapeCharacter = escapeCharacter;
		}
		return escapeCharacter;
	}

}
