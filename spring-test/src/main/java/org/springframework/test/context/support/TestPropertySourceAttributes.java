/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.log.LogMessage;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@code TestPropertySourceAttributes} encapsulates attributes declared
 * via {@link TestPropertySource @TestPropertySource} annotations.
 *
 * <p>In addition to encapsulating declared attributes,
 * {@code TestPropertySourceAttributes} also enforces configuration rules.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 * @see MergedTestPropertySources
 */
class TestPropertySourceAttributes {

	private static final Log logger = LogFactory.getLog(TestPropertySourceAttributes.class);


	private final int aggregateIndex;

	private final Class<?> declaringClass;

	private final List<String> locations;

	private final boolean inheritLocations;

	private final List<String> properties;

	private final boolean inheritProperties;


	TestPropertySourceAttributes(MergedAnnotation<TestPropertySource> annotation) {
		this.aggregateIndex = annotation.getAggregateIndex();
		this.declaringClass = (Class<?>) annotation.getSource();
		this.inheritLocations = annotation.getBoolean("inheritLocations");
		this.inheritProperties = annotation.getBoolean("inheritProperties");
		this.properties = new ArrayList<>();
		this.locations = new ArrayList<>();
		mergePropertiesAndLocations(annotation);
	}


	boolean canMerge(MergedAnnotation<TestPropertySource> annotation) {
		return annotation.getAggregateIndex() == this.aggregateIndex;
	}

	void merge(MergedAnnotation<TestPropertySource> annotation) {
		Assert.state((Class<?>) annotation.getSource() == this.declaringClass,
				() -> "Detected @TestPropertySource declarations within an aggregate index "
						+ "with different source: " + this.declaringClass + " and "
						+ annotation.getSource());
		logger.trace(LogMessage.format("Retrieved %s for declaring class [%s].",
				annotation, this.declaringClass.getName()));
		assertSameBooleanAttribute(this.inheritLocations, annotation, "inheritLocations");
		assertSameBooleanAttribute(this.inheritProperties, annotation, "inheritProperties");
		mergePropertiesAndLocations(annotation);
	}

	private void assertSameBooleanAttribute(boolean expected,
			MergedAnnotation<TestPropertySource> annotation, String attribute) {
		Assert.isTrue(expected == annotation.getBoolean(attribute), () -> String.format(
				"Classes %s and %s must declare the same value for '%s' as other directly " +
				"present or meta-present @TestPropertySource annotations", this.declaringClass.getName(),
				((Class<?>) annotation.getSource()).getName(), attribute));
	}

	private void mergePropertiesAndLocations(
			MergedAnnotation<TestPropertySource> annotation) {
		String[] locations = annotation.getStringArray("locations");
		String[] properties = annotation.getStringArray("properties");
		boolean prepend = annotation.getDistance() > 0;
		if (ObjectUtils.isEmpty(locations) && ObjectUtils.isEmpty(properties)) {
			addAll(prepend, this.locations, detectDefaultPropertiesFile(annotation));
		}
		else {
			addAll(prepend, this.locations, locations);
			addAll(prepend, this.properties, properties);
		}
	}

	private void addAll(boolean prepend, List<String> list, String... additions) {
		list.addAll(prepend ? 0 : list.size(), Arrays.asList(additions));
	}

	private String detectDefaultPropertiesFile(
			MergedAnnotation<TestPropertySource> annotation) {
		Class<?> testClass = (Class<?>) annotation.getSource();
		String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);
		if (!classPathResource.exists()) {
			String msg = String.format(
					"Could not detect default properties file for test class [%s]: "
							+ "%s does not exist. Either declare the 'locations' or 'properties' attributes "
							+ "of @TestPropertySource or make the default properties file available.",
					testClass.getName(), classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Detected default properties file \"%s\" for test class [%s]",
					prefixedResourcePath, testClass.getName()));
		}
		return prefixedResourcePath;
	}

	/**
	 * Get the {@linkplain Class class} that declared {@code @TestPropertySource}.
	 * @return the declaring class; never {@code null}
	 */
	Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * Get the resource locations that were declared via {@code @TestPropertySource}.
	 * <p>Note: The returned value may represent a <em>detected default</em>
	 * or merged locations that do not match the original value declared via a
	 * single {@code @TestPropertySource} annotation.
	 * @return the resource locations; potentially <em>empty</em>
	 * @see TestPropertySource#value
	 * @see TestPropertySource#locations
	 */
	String[] getLocations() {
		return this.locations.toArray(new String[0]);
	}

	/**
	 * Get the {@code inheritLocations} flag that was declared via {@code @TestPropertySource}.
	 * @return the {@code inheritLocations} flag
	 * @see TestPropertySource#inheritLocations
	 */
	boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * Get the inlined properties that were declared via {@code @TestPropertySource}.
	 * <p>Note: The returned value may represent merged properties that do not
	 * match the original value declared via a single {@code @TestPropertySource}
	 * annotation.
	 * @return the inlined properties; potentially <em>empty</em>
	 * @see TestPropertySource#properties
	 */
	String[] getProperties() {
		return this.properties.toArray(new String[0]);
	}

	/**
	 * Get the {@code inheritProperties} flag that was declared via {@code @TestPropertySource}.
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
				.append("declaringClass", this.declaringClass.getName())
				.append("locations", this.locations)
				.append("inheritLocations", this.inheritLocations)
				.append("properties", this.properties)
				.append("inheritProperties", this.inheritProperties)
				.toString();
	}

}
