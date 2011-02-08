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

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment:
 * <ol>
 *   <li>profiles</li>
 *   <li>properties</li>
 * </ol>
 *
 * A <em>profile</em> is a named, logical group of bean definitions to be registered with the
 * container only if the given profile is <em>active</em>. Beans may be assigned to a profile
 * whether defined in XML or annotations; see the spring-beans 3.1 schema or the {@link
 * org.springframework.context.annotation.Profile @Profile} annotation for syntax details.
 * The role of the Environment object with relation to profiles is in determining which profiles
 * (if any) are currently {@linkplain #getActiveProfiles active}, and which profiles (if any)
 * should be {@linkplain #getDefaultProfiles active by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may originate
 * from a variety of sources: properties files, JVM system properties, system environment
 * variables, JNDI, servlet context parameters, ad-hoc Properties objects, Maps, and so on.
 * The role of the environment object with relation to properties is to provide the user with a
 * convenient service interface for configuring property sources and resolving properties from them.
 *
 * <p>Beans managed within an ApplicationContext may register to be {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware}, where they can query profile state
 * or resolve properties directly.
 *
 * <p>More commonly, beans will not interact with the Environment directly, but will have ${...}
 * property values replaced by a property placeholder configurer such as {@link
 * org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is EnvironmentAware, and as of Spring 3.1 is
 * registered by default when using {@code <context:property-placeholder/>}.
 *
 * <p>Configuration of the environment object must be done through the {@link ConfigurableEnvironment}
 * interface, returned from all AbstractApplicationContext subclass getEnvironment() methods. See
 * {@link DefaultEnvironment} for several examples of using the ConfigurableEnvironment interface
 * to manipulate property sources prior to application context refresh().
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see DefaultEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 */
public interface Environment extends PropertyResolver {

	/**
	 * Return the set of profiles explicitly made active for this environment. Profiles are used for
	 * creating logical groupings of bean definitions to be registered conditionally, often based on
	 * deployment environment.  Profiles can be activated by setting {@linkplain
	 * AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME "spring.profile.active"} as a system property
	 * or by calling {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
	 *
	 * <p>If no profiles have explicitly been specified as active, then any 'default' profiles will implicitly
	 * be considered active.
	 *
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * Return the set of profiles to be active by default when no active profiles have been set explicitly.
	 *
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	String[] getDefaultProfiles();

	/**
	 * @return whether one or more of the given profiles is active, or in the case of no explicit active
	 * profiles, whether one or more of the given profiles is included in the set of default profiles
	 * @throws IllegalArgumentException unless at least one profile has been specified
	 * @throws IllegalArgumentException if any profile is the empty string or consists only of whitespace
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 */
	boolean acceptsProfiles(String... profiles);

}
