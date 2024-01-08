/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.context.support.ContextLoaderUtils.resolveContextConfigurationAttributes;

/**
 * Tests for {@link ContextLoaderUtils} involving {@link ContextConfigurationAttributes}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
class ContextLoaderUtilsConfigurationAttributesTests extends AbstractContextConfigurationUtilsTests {

	private void assertLocationsFooAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, LocationsFoo.class, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY,
				ContextLoader.class, false);
	}

	private void assertClassesFooAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, ClassesFoo.class, EMPTY_STRING_ARRAY, new Class<?>[] {FooConfig.class},
				ContextLoader.class, false);
	}

	private void assertLocationsBarAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, LocationsBar.class, new String[] {"/bar.xml"}, EMPTY_CLASS_ARRAY,
				AnnotationConfigContextLoader.class, true);
	}

	private void assertClassesBarAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, ClassesBar.class, EMPTY_STRING_ARRAY, new Class<?>[] {BarConfig.class},
				AnnotationConfigContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithConflictingLocations() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				resolveContextConfigurationAttributes(ConflictingLocations.class))
			.withMessageStartingWith("Different @AliasFor mirror values")
			.withMessageContaining(ConflictingLocations.class.getName())
			.withMessageContaining("attribute 'locations' and its alias 'value'")
			.withMessageContaining("values of [{y}] and [{x}]");
	}

	@Test
	void resolveConfigAttributesWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertAttributes(attributesList.get(0),
				testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithLocalAnnotationAndLocations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsFoo.class);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertLocationsFooAttributes(attributesList.get(0));
	}

	@Test
	void resolveConfigAttributesWithMetaAnnotationAndLocations() {
		Class<MetaLocationsFoo> testClass = MetaLocationsFoo.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverrides() {
		Class<MetaLocationsFooWithOverrides> testClass = MetaLocationsFooWithOverrides.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverriddenAttributes() {
		Class<MetaLocationsFooWithOverriddenAttributes> testClass = MetaLocationsFooWithOverriddenAttributes.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"foo1.xml", "foo2.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithMetaAnnotationAndLocationsInClassHierarchy() {
		Class<MetaLocationsBar> testClass = MetaLocationsBar.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(2);
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/bar.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
		assertAttributes(attributesList.get(1),
				MetaLocationsFoo.class, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	void resolveConfigAttributesWithLocalAnnotationAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesFoo.class);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
		assertClassesFooAttributes(attributesList.get(0));
	}

	@Test
	void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndLocations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsBar.class);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(2);
		assertLocationsBarAttributes(attributesList.get(0));
		assertLocationsFooAttributes(attributesList.get(1));
	}

	@Test
	void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesBar.class);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(2);
		assertClassesBarAttributes(attributesList.get(0));
		assertClassesFooAttributes(attributesList.get(1));
	}

	/**
	 * Verifies change requested in <a href="https://jira.spring.io/browse/SPR-11634">SPR-11634</a>.
	 * @since 4.0.4
	 */
	@Test
	void resolveConfigAttributesWithLocationsAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsAndClasses.class);
		assertThat(attributesList).isNotNull();
		assertThat(attributesList).hasSize(1);
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration(value = "x", locations = "y")
	private static class ConflictingLocations {
	}

	@ContextConfiguration(locations = "x", classes = Object.class)
	private static class LocationsAndClasses {
	}

}
