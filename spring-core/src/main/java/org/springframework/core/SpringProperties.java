/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

/**
 * Static holder for local Spring properties, i.e. defined at the Spring library level.
 *
 * <p>Reads a {@code spring.properties} file from the root of the classpath and
 * also allows for programmatically setting properties via {@link #setProperty}.
 * When retrieving properties, local entries are checked first, with JVM-level
 * system properties checked next as a fallback via {@link System#getProperty}.
 *
 * <p>This is an alternative way to set Spring-related system properties such as
 * {@code spring.getenv.ignore} and {@code spring.beaninfo.ignore}, in particular
 * for scenarios where JVM system properties are locked on the target platform
 * (for example, WebSphere). See {@link #setFlag} for a convenient way to locally
 * set such flags to {@code "true"}.
 *
 * @author Juergen Hoeller
 * @since 3.2.7
 * @see org.springframework.aot.AotDetector#AOT_ENABLED
 * @see org.springframework.beans.StandardBeanInfoFactory#IGNORE_BEANINFO_PROPERTY_NAME
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#STRICT_LOCKING_PROPERTY_NAME
 * @see org.springframework.core.env.AbstractEnvironment#IGNORE_GETENV_PROPERTY_NAME
 * @see org.springframework.expression.spel.SpelParserConfiguration#SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
 * @see org.springframework.jdbc.core.StatementCreatorUtils#IGNORE_GETPARAMETERTYPE_PROPERTY_NAME
 * @see org.springframework.jndi.JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME
 * @see org.springframework.objenesis.SpringObjenesis#IGNORE_OBJENESIS_PROPERTY_NAME
 * @see org.springframework.test.context.NestedTestConfiguration#ENCLOSING_CONFIGURATION_PROPERTY_NAME
 * @see org.springframework.test.context.TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
 * @see org.springframework.test.context.cache.ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
 */
public final class SpringProperties {

	private static final String PROPERTIES_RESOURCE_LOCATION = "spring.properties";

	private static final Properties localProperties = new Properties();


	static {
		try {
			ClassLoader cl = SpringProperties.class.getClassLoader();
			URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
			if (url != null) {
				try (InputStream is = url.openStream()) {
					localProperties.load(is);
				}
			}
		}
		catch (IOException ex) {
			System.err.println("Could not load 'spring.properties' file from local classpath: " + ex);
		}
	}


	private SpringProperties() {
	}


	/**
	 * Programmatically set a local property, overriding an entry in the
	 * {@code spring.properties} file (if any).
	 * @param key the property key
	 * @param value the associated property value, or {@code null} to reset it
	 */
	public static void setProperty(String key, @Nullable String value) {
		if (value != null) {
			localProperties.setProperty(key, value);
		}
		else {
			localProperties.remove(key);
		}
	}

	/**
	 * Retrieve the property value for the given key, checking local Spring
	 * properties first and falling back to JVM-level system properties.
	 * @param key the property key
	 * @return the associated property value, or {@code null} if none found
	 */
	public static @Nullable String getProperty(String key) {
		String value = localProperties.getProperty(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			}
			catch (Throwable ex) {
				System.err.println("Could not retrieve system property '" + key + "': " + ex);
			}
		}
		return value;
	}

	/**
	 * Programmatically set a local flag to "true", overriding an
	 * entry in the {@code spring.properties} file (if any).
	 * @param key the property key
	 */
	public static void setFlag(String key) {
		localProperties.setProperty(key, Boolean.TRUE.toString());
	}

	/**
	 * Programmatically set a local flag to the given value, overriding
	 * an entry in the {@code spring.properties} file (if any).
	 * @param key the property key
	 * @param value the associated boolean value
	 * @since 6.2.6
	 */
	public static void setFlag(String key, boolean value) {
		localProperties.setProperty(key, Boolean.toString(value));
	}

	/**
	 * Retrieve the flag for the given property key.
	 * @param key the property key
	 * @return {@code true} if the property is set to the string "true"
	 * (ignoring case), {@code false} otherwise
	 */
	public static boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

	/**
	 * Retrieve the flag for the given property key, returning {@code null}
	 * instead of {@code false} in case of no actual flag set.
	 * @param key the property key
	 * @return {@code true} if the property is set to the string "true"
	 * (ignoring case), {@code} false if it is set to any other value,
	 * {@code null} if it is not set at all
	 * @since 6.2.6
	 */
	public static @Nullable Boolean checkFlag(String key) {
		String flag = getProperty(key);
		return (flag != null ? Boolean.valueOf(flag) : null);
	}

}
