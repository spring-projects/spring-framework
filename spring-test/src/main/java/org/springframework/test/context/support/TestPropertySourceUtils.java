/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.test.util.MetaAnnotationUtils.*;
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
public abstract class TestPropertySourceUtils {

	/**
	 * The name of the {@link MapPropertySource} created from <em>inlined properties</em>.
	 * @since 4.1.5
	 * @see {@link #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])}
	 */
	public static final String INLINED_PROPERTIES_PROPERTY_SOURCE_NAME = "Inlined Test Properties";

	private static final Log logger = LogFactory.getLog(TestPropertySourceUtils.class);


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
			TestPropertySource testPropertySource = descriptor.synthesizeAnnotation();
			Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @TestPropertySource [%s] for declaring class [%s].",
					testPropertySource, rootDeclaringClass.getName()));
			}

			TestPropertySourceAttributes attributes = new TestPropertySourceAttributes(rootDeclaringClass,
				testPropertySource);
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
	 * Add the {@link Properties} files from the given resource {@code locations}
	 * to the {@link Environment} of the supplied {@code context}.
	 * <p>This method simply delegates to
	 * {@link #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)}.
	 * @param context the application context whose environment should be updated;
	 * never {@code null}
	 * @param locations the resource locations of {@code Properties} files to add
	 * to the environment; potentially empty but never {@code null}
	 * @since 4.1.5
	 * @see ResourcePropertySource
	 * @see TestPropertySource#locations
	 * @see #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)
	 * @throws IllegalStateException if an error occurs while processing a properties file
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableApplicationContext context, String... locations) {
		Assert.notNull(context, "context must not be null");
		Assert.notNull(locations, "locations must not be null");
		addPropertiesFilesToEnvironment(context.getEnvironment(), context, locations);
	}

	/**
	 * Add the {@link Properties} files from the given resource {@code locations}
	 * to the supplied {@link ConfigurableEnvironment environment}.
	 * <p>Property placeholders in resource locations (i.e., <code>${...}</code>)
	 * will be {@linkplain Environment#resolveRequiredPlaceholders(String) resolved}
	 * against the {@code Environment}.
	 * <p>Each properties file will be converted to a {@link ResourcePropertySource}
	 * that will be added to the {@link PropertySources} of the environment with
	 * highest precedence.
	 * @param environment the environment to update; never {@code null}
	 * @param resourceLoader the {@code ResourceLoader} to use to load each resource;
	 * never {@code null}
	 * @param locations the resource locations of {@code Properties} files to add
	 * to the environment; potentially empty but never {@code null}
	 * @since 4.3
	 * @see ResourcePropertySource
	 * @see TestPropertySource#locations
	 * @see #addPropertiesFilesToEnvironment(ConfigurableApplicationContext, String...)
	 * @throws IllegalStateException if an error occurs while processing a properties file
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String... locations) {

		Assert.notNull(environment, "environment must not be null");
		Assert.notNull(resourceLoader, "resourceLoader must not be null");
		Assert.notNull(locations, "locations must not be null");
		try {
			for (String location : locations) {
				String resolvedLocation = environment.resolveRequiredPlaceholders(location);
				Resource resource = resourceLoader.getResource(resolvedLocation);
				environment.getPropertySources().addFirst(new ResourcePropertySource(resource));
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to add PropertySource to Environment", ex);
		}
	}

	/**
	 * Add the given <em>inlined properties</em> to the {@link Environment} of the
	 * supplied {@code context}.
	 * <p>This method simply delegates to
	 * {@link #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])}.
	 * @param context the application context whose environment should be updated;
	 * never {@code null}
	 * @param inlinedProperties the inlined properties to add to the environment;
	 * potentially empty but never {@code null}
	 * @since 4.1.5
	 * @see TestPropertySource#properties
	 * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context,
			String... inlinedProperties) {
		Assert.notNull(context, "context must not be null");
		Assert.notNull(inlinedProperties, "inlinedProperties must not be null");
		addInlinedPropertiesToEnvironment(context.getEnvironment(), inlinedProperties);
	}

	/**
	 * Add the given <em>inlined properties</em> (in the form of <em>key-value</em>
	 * pairs) to the supplied {@link ConfigurableEnvironment environment}.
	 * <p>All key-value pairs will be added to the {@code Environment} as a
	 * single {@link MapPropertySource} with the highest precedence.
	 * <p>For details on the parsing of <em>inlined properties</em>, consult the
	 * Javadoc for {@link #convertInlinedPropertiesToMap}.
	 * @param environment the environment to update; never {@code null}
	 * @param inlinedProperties the inlined properties to add to the environment;
	 * potentially empty but never {@code null}
	 * @since 4.1.5
	 * @see MapPropertySource
	 * @see #INLINED_PROPERTIES_PROPERTY_SOURCE_NAME
	 * @see TestPropertySource#properties
	 * @see #convertInlinedPropertiesToMap
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String... inlinedProperties) {
		Assert.notNull(environment, "environment must not be null");
		Assert.notNull(inlinedProperties, "inlinedProperties must not be null");
		if (!ObjectUtils.isEmpty(inlinedProperties)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Adding inlined properties to environment: "
						+ ObjectUtils.nullSafeToString(inlinedProperties));
			}
			MapPropertySource ps = (MapPropertySource) environment.getPropertySources().get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
			if (ps == null) {
				ps = new MapPropertySource(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME,
						new LinkedHashMap<String, Object>());
				environment.getPropertySources().addFirst(ps);
			}
			ps.getSource().putAll(convertInlinedPropertiesToMap(inlinedProperties));
		}
	}

	/**
	 * Convert the supplied <em>inlined properties</em> (in the form of <em>key-value</em>
	 * pairs) into a map keyed by property name, preserving the ordering of property names
	 * in the returned map.
	 * <p>Parsing of the key-value pairs is achieved by converting all pairs
	 * into <em>virtual</em> properties files in memory and delegating to
	 * {@link Properties#load(java.io.Reader)} to parse each virtual file.
	 * <p>For a full discussion of <em>inlined properties</em>, consult the Javadoc
	 * for {@link TestPropertySource#properties}.
	 * @param inlinedProperties the inlined properties to convert; potentially empty
	 * but never {@code null}
	 * @return a new, ordered map containing the converted properties
	 * @since 4.1.5
	 * @throws IllegalStateException if a given key-value pair cannot be parsed, or if
	 * a given inlined property contains multiple key-value pairs
	 * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
	 */
	public static Map<String, Object> convertInlinedPropertiesToMap(String... inlinedProperties) {
		Assert.notNull(inlinedProperties, "inlinedProperties must not be null");
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		Properties props = new Properties();
		for (String pair : inlinedProperties) {
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
