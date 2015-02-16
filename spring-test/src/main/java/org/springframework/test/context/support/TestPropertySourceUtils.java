/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * Utility methods for working with {@link TestPropertySource @TestPropertySource}
 * and adding test {@link PropertySource PropertySources} to the {@code Environment}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see TestPropertySource
 */
abstract class TestPropertySourceUtils {

	private static final Log logger = LogFactory.getLog(TestPropertySourceUtils.class);


	private TestPropertySourceUtils() {
		/* no-op */
	}

	static MergedTestPropertySources buildMergedTestPropertySources(Class<?> testClass) {
		Class<TestPropertySource> annotationType = TestPropertySource.class;
		AnnotationDescriptor<TestPropertySource> descriptor = findAnnotationDescriptor(testClass, annotationType);
		if (descriptor == null) {
			return new MergedTestPropertySources();
		}

		// else...
		List<TestPropertySourceAttributes> attributesList = resolveTestPropertySourceAttributes(testClass);
		String[] locations = mergeLocations(attributesList);
		String[] properties = mergeProperties(attributesList);
		return new MergedTestPropertySources(locations, properties);
	}

	private static List<TestPropertySourceAttributes> resolveTestPropertySourceAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final List<TestPropertySourceAttributes> attributesList = new ArrayList<TestPropertySourceAttributes>();
		final Class<TestPropertySource> annotationType = TestPropertySource.class;
		AnnotationDescriptor<TestPropertySource> descriptor = findAnnotationDescriptor(testClass, annotationType);
		Assert.notNull(descriptor, String.format(
			"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
			annotationType.getName(), testClass.getName()));

		while (descriptor != null) {
			AnnotationAttributes annAttrs = descriptor.getAnnotationAttributes();
			Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @TestPropertySource attributes [%s] for declaring class [%s].",
					annAttrs, rootDeclaringClass.getName()));
			}

			TestPropertySourceAttributes attributes = new TestPropertySourceAttributes(rootDeclaringClass, annAttrs);
			if (logger.isTraceEnabled()) {
				logger.trace("Resolved TestPropertySource attributes: " + attributes);
			}
			attributesList.add(attributes);

			descriptor = findAnnotationDescriptor(rootDeclaringClass.getSuperclass(), annotationType);
		}

		return attributesList;
	}

	private static String[] mergeLocations(List<TestPropertySourceAttributes> attributesList) {
		final List<String> locations = new ArrayList<String>();

		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations for TestPropertySource attributes %s", attrs));
			}

			String[] locationsArray = TestContextResourceUtils.convertToClasspathResourcePaths(
				attrs.getDeclaringClass(), attrs.getLocations());
			locations.addAll(0, Arrays.<String> asList(locationsArray));

			if (!attrs.isInheritLocations()) {
				break;
			}
		}

		return StringUtils.toStringArray(locations);
	}

	private static String[] mergeProperties(List<TestPropertySourceAttributes> attributesList) {
		final List<String> properties = new ArrayList<String>();

		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing inlined properties for TestPropertySource attributes %s", attrs));
			}

			properties.addAll(0, Arrays.<String> asList(attrs.getProperties()));

			if (!attrs.isInheritProperties()) {
				break;
			}
		}

		return StringUtils.toStringArray(properties);
	}

	/**
	 * @since 4.1.5
	 */
	static void addResourcePropertySourcesToEnvironment(ConfigurableApplicationContext context,
			String[] propertySourceLocations) {
		try {
			ConfigurableEnvironment environment = context.getEnvironment();
			String[] locations = propertySourceLocations;
			for (String location : locations) {
				String resolvedLocation = environment.resolveRequiredPlaceholders(location);
				Resource resource = context.getResource(resolvedLocation);
				ResourcePropertySource ps = new ResourcePropertySource(resource);
				environment.getPropertySources().addFirst(ps);
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to add PropertySource to Environment", e);
		}
	}

	/**
	 * @since 4.1.5
	 */
	static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context,
			String[] propertySourceProperties) {
		addInlinedPropertiesToEnvironment(context.getEnvironment(), propertySourceProperties);
	}

	/**
	 * @since 4.1.5
	 */
	static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String[] propertySourceProperties) {
		if (!ObjectUtils.isEmpty(propertySourceProperties)) {
			String name = "test properties " + ObjectUtils.nullSafeToString(propertySourceProperties);
			MapPropertySource ps = new MapPropertySource(name, extractEnvironmentProperties(propertySourceProperties));
			environment.getPropertySources().addFirst(ps);
		}
	}

	/**
	 * Extract environment properties from the supplied key/value pairs,
	 * preserving the ordering of property names in the returned map.
	 * <p>Parsing of the key/value pairs is achieved by converting all pairs
	 * into <em>virtual</em> properties files in memory and delegating to
	 * {@link Properties#load(java.io.Reader)} to parse each virtual file.
	 */
	private static Map<String, Object> extractEnvironmentProperties(String[] keyValuePairs) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		Properties props = new Properties();
		for (String pair : keyValuePairs) {
			if (!StringUtils.hasText(pair)) {
				continue;
			}

			try {
				props.load(new StringReader(pair));
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to load test environment property from [" + pair + "].", e);
			}
			Assert.state(props.size() == 1, "Failed to load exactly one test environment property from [" + pair + "].");

			for (String name : props.stringPropertyNames()) {
				map.put(name, props.getProperty(name));
			}
			props.clear();
		}

		return map;
	}

}
