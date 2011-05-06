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

import static java.lang.String.*;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import static org.springframework.util.StringUtils.*;

/**
 * Abstract base class for {@link Environment} implementations.
 *
 * @author Chris Beams
 * @since 3.1
 * @see DefaultEnvironment
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * Name of property to set to specify active profiles: {@value}. May be comma delimited.
	 * @see ConfigurableEnvironment#setActiveProfiles
	 */
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * Name of property to set to specify default profiles: {@value}. May be comma delimited.
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	protected final Log logger = LogFactory.getLog(getClass());

	private Set<String> activeProfiles = new LinkedHashSet<String>();
	private Set<String> defaultProfiles = new LinkedHashSet<String>();

	private MutablePropertySources propertySources = new MutablePropertySources();
	private ConfigurablePropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	//---------------------------------------------------------------------

	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}

	protected Set<String> doGetActiveProfiles() {
		if (this.activeProfiles.isEmpty()) {
			String profiles = this.propertyResolver.getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
			if (StringUtils.hasText(profiles)) {
				this.activeProfiles = commaDelimitedListToSet(trimAllWhitespace(profiles));
			}
		}
		return this.activeProfiles;
	}

	public void setActiveProfiles(String... profiles) {
		this.activeProfiles.clear();
		this.activeProfiles.addAll(Arrays.asList(profiles));
	}

	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	protected Set<String> doGetDefaultProfiles() {
		if (this.defaultProfiles.isEmpty()) {
			String profiles = this.propertyResolver.getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
			if (StringUtils.hasText(profiles)) {
				this.defaultProfiles = commaDelimitedListToSet(trimAllWhitespace(profiles));
			}
		}
		return this.defaultProfiles;
	}

	public void setDefaultProfiles(String... profiles) {
		this.defaultProfiles.clear();
		this.defaultProfiles.addAll(Arrays.asList(profiles));
	}

	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		boolean activeProfileFound = false;
		Set<String> activeProfiles = this.doGetActiveProfiles();
		Set<String> defaultProfiles = this.doGetDefaultProfiles();
		for (String profile : profiles) {
			Assert.hasText(profile, "profile must not be empty");
			if (activeProfiles.contains(profile)
					|| (activeProfiles.isEmpty() && defaultProfiles.contains(profile))) {
				activeProfileFound = true;
				break;
			}
		}
		return activeProfileFound;
	}

	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	public ConfigurablePropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getSystemEnvironment() {
		Map<String, ?> systemEnvironment;
		try {
			systemEnvironment = System.getenv();
		}
		catch (AccessControlException ex) {
			systemEnvironment = new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String variableName) {
					try {
						return System.getenv(variableName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info(format("Caught AccessControlException when accessing system environment variable " +
									"[%s]; its value will be returned [null]. Reason: %s", variableName, ex.getMessage()));
						}
						return null;
					}
				}
			};
		}
		return (Map<String, Object>) systemEnvironment;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, Object> getSystemProperties() {
		Map systemProperties;
		try {
			systemProperties = System.getProperties();
		}
		catch (AccessControlException ex) {
			systemProperties = new ReadOnlySystemAttributesMap() {
				@Override
				protected String getSystemAttribute(String propertyName) {
					try {
						return System.getProperty(propertyName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info(format("Caught AccessControlException when accessing system property " +
									"[%s]; its value will be returned [null]. Reason: %s", propertyName, ex.getMessage()));
						}
						return null;
					}
				}
			};
		}
		return systemProperties;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	//---------------------------------------------------------------------

	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	public String getProperty(String key) {
		return this.propertyResolver.getProperty(key);
	}

	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetType) {
		return this.propertyResolver.getPropertyAsClass(key, targetType);
	}

	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	public String resolvePlaceholders(String text) {
		return this.propertyResolver.resolvePlaceholders(text);
	}

	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}

	public void setConversionService(ConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	public ConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}


	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}


	public void setValueSeparator(String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}


	@Override
	public String toString() {
		return format("%s [activeProfiles=%s, defaultProfiles=%s, propertySources=%s]",
				getClass().getSimpleName(), this.activeProfiles, this.defaultProfiles, this.propertySources);
	}

}
