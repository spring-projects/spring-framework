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

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the spring-beans 3.1 schema
 * or the {@link org.springframework.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the {@code Environment} object with relation to properties
 * is to provide the user with a convenient service interface for configuring property
 * sources and resolving properties from them.
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 *
 * <p>In most cases, however, application-level beans should not need to interact with the
 * {@code Environment} directly but instead may request to have {@code ${...}} property
 * values replaced by a property placeholder configurer such as
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is {@code EnvironmentAware} and
 * registered by default when using {@code <context:property-placeholder/>}.
 *
 * <p>Configuration of the {@code Environment} object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 */
public interface Environment extends PropertyResolver {

	/**
	 * Return the set of profiles explicitly made active for this environment. Profiles
	 * are used for creating logical groupings of bean definitions to be registered
	 * conditionally, for example based on deployment environment. Profiles can be
	 * activated by setting {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 * "spring.profiles.active"} as a system property or by calling
	 * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
	 * <p>If no profiles have explicitly been specified as active, then any
	 * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	String[] getActiveProfiles();

	/**
	 * Return the set of profiles to be active by default when no active profiles have
	 * been set explicitly.
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	String[] getDefaultProfiles();

	/**
	 * Determine whether one of the given profile expressions matches the
	 * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
	 * of no explicit active profiles, whether one of the given profile expressions
	 * matches the {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>Profile expressions allow for complex, boolean profile logic to be
	 * expressed &mdash; for example {@code "p1 & p2"}, {@code "(p1 & p2) | p3"},
	 * etc. See {@link Profiles#of(String...)} for details on the supported
	 * expression syntax.
	 * <p>This method is a convenient shortcut for
	 * {@code env.acceptsProfiles(Profiles.of(profileExpressions))}.
	 * @since 5.3.28
	 * @see Profiles#of(String...)
	 * @see #acceptsProfiles(Profiles)
	 */
	default boolean matchesProfiles(String... profileExpressions) {
		return acceptsProfiles(Profiles.of(profileExpressions));
	}

	/**
	 * Determine whether one or more of the given profiles is active &mdash; or
	 * in the case of no explicit {@linkplain #getActiveProfiles() active profiles},
	 * whether one or more of the given profiles is included in the set of
	 * {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>If a profile begins with '!' the logic is inverted, meaning this method
	 * will return {@code true} if the given profile is <em>not</em> active. For
	 * example, {@code env.acceptsProfiles("p1", "!p2")} will return {@code true}
	 * if profile 'p1' is active or 'p2' is not active.
	 * @throws IllegalArgumentException if called with a {@code null} array, an
	 * empty array, zero arguments or if any profile is {@code null}, empty, or
	 * whitespace only
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 * @see #matchesProfiles(String...)
	 * @see #acceptsProfiles(Profiles)
	 * @deprecated in favor of {@link #acceptsProfiles(Profiles)} or {@link #matchesProfiles(String...)}
	 */
	@Deprecated(since = "5.1")
	boolean acceptsProfiles(String... profiles);

	/**
	 * Determine whether the given {@link Profiles} predicate matches the
	 * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
	 * of no explicit active profiles, whether the given {@code Profiles} predicate
	 * matches the {@linkplain #getDefaultProfiles() default profiles}.
	 * <p>If you wish provide profile expressions directly as strings, use
	 * {@link #matchesProfiles(String...)} instead.
	 * @since 5.1
	 * @see #matchesProfiles(String...)
	 * @see Profiles#of(String...)
	 */
	boolean acceptsProfiles(Profiles profiles);

}
