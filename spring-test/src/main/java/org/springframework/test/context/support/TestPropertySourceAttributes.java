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
import org.springframework.util.StringUtils;

/**
 * {@code TestPropertySourceAttributes} encapsulates attributes declared
 * via {@link TestPropertySource @TestPropertySource} annotations.
 *
 * <p>In addition to encapsulating declared attributes,
 * {@code TestPropertySourceAttributes} also enforces configuration rules.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see TestPropertySource
 * @see MergedTestPropertySources
 */
class TestPropertySourceAttributes {

	private static final Log logger = LogFactory.getLog(TestPropertySourceAttributes.class);


	private final int aggregateIndex;

	private final Class<?> declaringClass;

	private final MergedAnnotation<?> rootAnnotation;

	private final List<String> locations = new ArrayList<>();

	private final boolean inheritLocations;

	private final List<String> properties = new ArrayList<>();

	private final boolean inheritProperties;


	TestPropertySourceAttributes(MergedAnnotation<TestPropertySource> annotation) {
		this.aggregateIndex = annotation.getAggregateIndex();
		this.declaringClass = declaringClass(annotation);
		this.rootAnnotation = annotation.getRoot();
		this.inheritLocations = annotation.getBoolean("inheritLocations");
		this.inheritProperties = annotation.getBoolean("inheritProperties");
		mergePropertiesAndLocations(annotation);
	}


	/**
	 * Determine if the annotation represented by this
	 * {@code TestPropertySourceAttributes} instance can be merged with the
	 * supplied {@code annotation}.
	 * <p>This method effectively checks that two annotations are declared at
	 * the same level in the type hierarchy (i.e., have the same
	 * {@linkplain MergedAnnotation#getAggregateIndex() aggregate index}).
	 * @since 5.2
	 * @see #mergeWith(MergedAnnotation)
	 */
	boolean canMergeWith(MergedAnnotation<TestPropertySource> annotation) {
		return annotation.getAggregateIndex() == this.aggregateIndex;
	}

	/**
	 * Merge this {@code TestPropertySourceAttributes} instance with the
	 * supplied {@code annotation}, asserting that the two sets of test property
	 * source attributes have identical values for the
	 * {@link TestPropertySource#inheritLocations} and
	 * {@link TestPropertySource#inheritProperties} flags and that the two
	 * underlying annotations were declared on the same class.
	 * <p>This method should only be invoked if {@link #canMergeWith(MergedAnnotation)}
	 * returns {@code true}.
	 * @since 5.2
	 * @see #canMergeWith(MergedAnnotation)
	 */
	void mergeWith(MergedAnnotation<TestPropertySource> annotation) {
		Class<?> source = declaringClass(annotation);
		Assert.state(source == this.declaringClass,
				() -> "Detected @TestPropertySource declarations within an aggregate index "
						+ "with different sources: " + this.declaringClass.getName() + " and "
						+ source.getName());
		logger.trace(LogMessage.format("Retrieved %s for declaring class [%s].",
				annotation, this.declaringClass.getName()));
		assertSameBooleanAttribute(this.inheritLocations, annotation, "inheritLocations");
		assertSameBooleanAttribute(this.inheritProperties, annotation, "inheritProperties");
		mergePropertiesAndLocations(annotation);
	}

	private void assertSameBooleanAttribute(boolean expected, MergedAnnotation<TestPropertySource> annotation,
			String attribute) {

		Assert.isTrue(expected == annotation.getBoolean(attribute), () -> String.format(
				"@%s on %s and @%s on %s must declare the same value for '%s' as other " +
				"directly present or meta-present @TestPropertySource annotations",
			this.rootAnnotation.getType().getSimpleName(), this.declaringClass.getSimpleName(),
			annotation.getRoot().getType().getSimpleName(), declaringClass(annotation).getSimpleName(),
			attribute));
	}

	private void mergePropertiesAndLocations(MergedAnnotation<TestPropertySource> annotation) {
		String[] locations = annotation.getStringArray("locations");
		String[] properties = annotation.getStringArray("properties");
		// If the meta-distance is positive, that means the annotation is
		// meta-present and should therefore have lower priority than directly
		// present annotations (i.e., it should be prepended to the list instead
		// of appended). This follows the rule of last-one-wins for overriding
		// properties.
		boolean prepend = annotation.getDistance() > 0;
		if (ObjectUtils.isEmpty(locations) && ObjectUtils.isEmpty(properties)) {
			addAll(prepend, this.locations, detectDefaultPropertiesFile(annotation));
		}
		else {
			addAll(prepend, this.locations, locations);
			addAll(prepend, this.properties, properties);
		}
	}

	/**
	 * Add all of the supplied elements to the provided list, honoring the
	 * {@code prepend} flag.
	 * <p>If the {@code prepend} flag is {@code false}, the elements will appended
	 * to the list.
	 * @param prepend whether the elements should be prepended to the list
	 * @param list the list to which to add the elements
	 * @param elements the elements to add to the list
	 */
	private void addAll(boolean prepend, List<String> list, String... elements) {
		list.addAll((prepend ? 0 : list.size()), Arrays.asList(elements));
	}

	private String detectDefaultPropertiesFile(MergedAnnotation<TestPropertySource> annotation) {
		Class<?> testClass = declaringClass(annotation);
		String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);
		if (!classPathResource.exists()) {
			String msg = String.format(
					"Could not detect default properties file for test class [%s]: " +
							"%s does not exist. Either declare the 'locations' or 'properties' attributes " +
							"of @TestPropertySource or make the default properties file available.",
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
		return StringUtils.toStringArray(this.locations);
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
		return StringUtils.toStringArray(this.properties);
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
		return new ToStringCreator(this)
				.append("declaringClass", this.declaringClass.getName())
				.append("locations", this.locations)
				.append("inheritLocations", this.inheritLocations)
				.append("properties", this.properties)
				.append("inheritProperties", this.inheritProperties)
				.toString();
	}

	private static Class<?> declaringClass(MergedAnnotation<?> mergedAnnotation) {
		Object source = mergedAnnotation.getSource();
		Assert.state(source instanceof Class, "No source class available");
		return (Class<?>) source;
	}

}
