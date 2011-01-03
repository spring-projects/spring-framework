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
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profile.active";

	/**
	 * Name of property to set to specify default profiles: {@value}. May be comma delimited.
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profile.default";

	protected final Log logger = LogFactory.getLog(getClass());

	private Set<String> activeProfiles = new LinkedHashSet<String>();
	private Set<String> defaultProfiles = new LinkedHashSet<String>();

	private MutablePropertySources propertySources = new MutablePropertySources();
	private ConfigurablePropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);


	public String[] getActiveProfiles() {
		return this.doGetActiveProfiles().toArray(new String[]{});
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
		return this.doGetDefaultProfiles().toArray(new String[]{});
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

	public Map<String, String> getSystemEnvironment() {
		Map<String,String> systemEnvironment;
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
		return systemEnvironment;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, String> getSystemProperties() {
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

	@Override
	public String toString() {
		return format("%s [activeProfiles=%s, defaultProfiles=%s, propertySources=%s]",
				getClass().getSimpleName(), this.activeProfiles, this.defaultProfiles, this.propertySources);
	}

}
