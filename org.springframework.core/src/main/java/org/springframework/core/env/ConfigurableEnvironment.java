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

import java.util.Map;

/**
 * Configuration interface to be implemented by most if not all {@link Environment
 * Environments}. Provides facilities for setting active and default profiles as well
 * as accessing the {@linkplain #getPropertySources() property sources}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see DefaultEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * Specify the set of profiles active for this Environment. Profiles are
	 * evaluated during container bootstrap to determine whether bean definitions
	 * should be registered with the container.
	 *
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * Return the {@link PropertySources} for this environment in mutable form
	 */
	MutablePropertySources getPropertySources();

	/**
	 * Return the value of {@link System#getenv()} if allowed by the current {@link SecurityManager},
	 * otherwise return a map implementation that will attempt to access individual keys using calls to
	 * {@link System#getenv(String)}.
	 *
	 * <p>Note that most {@link Environment} implementations will include this system environment map as
	 * a default {@link PropertySource} to be searched. Therefore, it is recommended that this method not be
	 * used directly unless bypassing other property sources is expressly intended.
	 *
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw {@link IllegalAccessException};
	 * in cases where the SecurityManager forbids access to a property, {@code null} will be returned and an
	 * INFO-level log message will be issued noting the exception.
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * Return the value of {@link System#getProperties()} if allowed by the current {@link SecurityManager},
	 * otherwise return a map implementation that will attempt to access individual keys using calls to
	 * {@link System#getProperty(String)}.
	 *
	 * <p>Note that most {@code Environment} implementations will include this system properties map as a
	 * default {@link PropertySource} to be searched. Therefore, it is recommended that this method not be
	 * used directly unless bypassing other property sources is expressly intended.
	 *
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw {@link IllegalAccessException};
	 * in cases where the SecurityManager forbids access to a property, {@code null} will be returned and an
	 * INFO-level log message will be issued noting the exception.
	 */
	Map<String, Object> getSystemProperties();

}
