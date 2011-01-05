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

import org.springframework.core.convert.ConversionService;

/**
 * Default implementation of the {@link Environment} interface. Used throughout all non-Web*
 * ApplicationContext implementations.
 *
 * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as property
 * resolution and profile-related operations, this implementation configures two default property
 * sources, to be searched in the following order:
 * <ol>
 *   <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 *   <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ol>
 *
 * That is, if the key "xyz" is present both in the JVM system properties as well as in the
 * set of environment variables for the current process, the value of key "xyz" from system properties
 * will return from a call to {@code environment.getPropertyResolver().getProperty("xyz")}.
 * This ordering is chosen by default because system properties are per-JVM, while environment
 * variables may be the same across many JVMs on a given system.  Giving system properties
 * precedence allows for overriding of environment variables on a per-JVM basis.
 *
 * <p>These default property sources may be removed, reordered, or replaced; and additional
 * property sources may be added using the {@link MutablePropertySources} instance available
 * from {@link #getPropertySources()}.
 *
 * <h4>Example: adding a new property source with highest search priority</h4>
 * <pre class="code">
 *   ConfigurableEnvironment environment = new DefaultEnvironment();
 *   MutablePropertySources propertySources = environment.getPropertySources();
 *   Map<String, String> myMap = new HashMap<String, String>();
 *   myMap.put("xyz", "myValue");
 *   propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>Example: removing the default system properties property source</h4>
 * <pre class="code">
 *   MutablePropertySources propertySources = environment.getPropertySources();
 *   propertySources.remove(DefaultEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>Example: mocking the system environment for testing purposes</h4>
 * <pre class="code">
 *   MutablePropertySources propertySources = environment.getPropertySources();
 *   MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 *   propertySources.replace(DefaultEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * When an {@link Environment} is being used by an ApplicationContext, it is important
 * that any such PropertySource manipulations be performed <em>before</em> the context's {@link
 * org.springframework.context.support.AbstractApplicationContext#refresh() refresh()} method is
 * called. This ensures that all PropertySources are available during the container bootstrap process,
 * including use by {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * property placeholder configurers}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see org.springframework.web.context.support.DefaultWebEnvironment
 */
public class DefaultEnvironment extends AbstractEnvironment {

	/** System environment property source name: {@value} */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM system properties property source name: {@value} */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * Create a new {@code Environment} populated with property sources in the following order:
	 * <ul>
	 *   <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 *   <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 *
	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
	 */
	public DefaultEnvironment() {
		this.getPropertySources().addFirst(new MapPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, this.getSystemEnvironment()));
		this.getPropertySources().addFirst(new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, this.getSystemProperties()));
	}

}
