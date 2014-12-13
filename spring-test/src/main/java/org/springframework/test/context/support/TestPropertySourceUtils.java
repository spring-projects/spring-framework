/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.test.util.MetaAnnotationUtils.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * Utility methods for working with {@link TestPropertySource @TestPropertySource}.
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

}
