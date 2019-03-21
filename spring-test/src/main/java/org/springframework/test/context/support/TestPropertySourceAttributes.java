/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@code TestPropertySourceAttributes} encapsulates the attributes declared
 * via {@link TestPropertySource @TestPropertySource}.
 *
 * <p>In addition to encapsulating declared attributes,
 * {@code TestPropertySourceAttributes} also enforces configuration rules
 * and detects default properties files.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 * @see MergedTestPropertySources
 */
class TestPropertySourceAttributes {

	private static final Log logger = LogFactory.getLog(TestPropertySourceAttributes.class);

	private final Class<?> declaringClass;

	private final String[] locations;

	private final boolean inheritLocations;

	private final String[] properties;

	private final boolean inheritProperties;


	/**
	 * Create a new {@code TestPropertySourceAttributes} instance for the
	 * supplied {@link TestPropertySource @TestPropertySource} annotation and
	 * the {@linkplain Class test class} that declared it, enforcing
	 * configuration rules and detecting a default properties file if
	 * necessary.
	 * @param declaringClass the class that declared {@code @TestPropertySource}
	 * @param testPropertySource the annotation from which to retrieve the attributes
	 * @since 4.2
	 */
	TestPropertySourceAttributes(Class<?> declaringClass, TestPropertySource testPropertySource) {
		this(declaringClass, testPropertySource.locations(), testPropertySource.inheritLocations(),
			testPropertySource.properties(), testPropertySource.inheritProperties());
	}

	private TestPropertySourceAttributes(Class<?> declaringClass, String[] locations, boolean inheritLocations,
			String[] properties, boolean inheritProperties) {
		Assert.notNull(declaringClass, "declaringClass must not be null");

		if (ObjectUtils.isEmpty(locations) && ObjectUtils.isEmpty(properties)) {
			locations = new String[] { detectDefaultPropertiesFile(declaringClass) };
		}

		this.declaringClass = declaringClass;
		this.locations = locations;
		this.inheritLocations = inheritLocations;
		this.properties = properties;
		this.inheritProperties = inheritProperties;
	}

	/**
	 * Get the {@linkplain Class class} that declared {@code @TestPropertySource}.
	 *
	 * @return the declaring class; never {@code null}
	 */
	Class<?> getDeclaringClass() {
		return declaringClass;
	}

	/**
	 * Get the resource locations that were declared via {@code @TestPropertySource}.
	 *
	 * <p>Note: The returned value may represent a <em>detected default</em>
	 * that does not match the original value declared via {@code @TestPropertySource}.
	 *
	 * @return the resource locations; potentially {@code null} or <em>empty</em>
	 * @see TestPropertySource#value
	 * @see TestPropertySource#locations
	 * @see #setLocations(String[])
	 */
	String[] getLocations() {
		return locations;
	}

	/**
	 * Get the {@code inheritLocations} flag that was declared via {@code @TestPropertySource}.
	 *
	 * @return the {@code inheritLocations} flag
	 * @see TestPropertySource#inheritLocations
	 */
	boolean isInheritLocations() {
		return inheritLocations;
	}

	/**
	 * Get the inlined properties that were declared via {@code @TestPropertySource}.
	 *
	 * @return the inlined properties; potentially {@code null} or <em>empty</em>
	 * @see TestPropertySource#properties
	 */
	String[] getProperties() {
		return this.properties;
	}

	/**
	 * Get the {@code inheritProperties} flag that was declared via {@code @TestPropertySource}.
	 *
	 * @return the {@code inheritProperties} flag
	 * @see TestPropertySource#inheritProperties
	 */
	boolean isInheritProperties() {
		return this.inheritProperties;
	}

	/**
	 * Provide a String representation of the {@code @TestPropertySource}
	 * attributes and declaring class.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("declaringClass", declaringClass.getName())//
		.append("locations", ObjectUtils.nullSafeToString(locations))//
		.append("inheritLocations", inheritLocations)//
		.append("properties", ObjectUtils.nullSafeToString(properties))//
		.append("inheritProperties", inheritProperties)//
		.toString();
	}

	/**
	 * Detect a default properties file for the supplied class, as specified
	 * in the class-level Javadoc for {@link TestPropertySource}.
	 */
	private static String detectDefaultPropertiesFile(Class<?> testClass) {
		String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);

		if (classPathResource.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Detected default properties file \"%s\" for test class [%s]",
					prefixedResourcePath, testClass.getName()));
			}
			return prefixedResourcePath;
		}
		else {
			String msg = String.format("Could not detect default properties file for test [%s]: "
					+ "%s does not exist. Either declare the 'locations' or 'properties' attributes "
					+ "of @TestPropertySource or make the default properties file available.", testClass.getName(),
				classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
