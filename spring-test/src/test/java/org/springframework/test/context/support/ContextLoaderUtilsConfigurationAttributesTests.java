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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.context.support.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils} involving {@link ContextConfigurationAttributes}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsConfigurationAttributesTests extends AbstractContextConfigurationUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


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
	public void resolveConfigAttributesWithConflictingLocations() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString(ConflictingLocations.class.getName()));
		exception.expectMessage(either(
				containsString("attribute 'value' and its alias 'locations'")).or(
				containsString("attribute 'locations' and its alias 'value'")));
		exception.expectMessage(either(
				containsString("values of [{x}] and [{y}]")).or(
				containsString("values of [{y}] and [{x}]")));
		exception.expectMessage(containsString("but only one is permitted"));
		resolveContextConfigurationAttributes(ConflictingLocations.class);
	}

	@Test
	public void resolveConfigAttributesWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithLocalAnnotationAndLocations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsFoo.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertLocationsFooAttributes(attributesList.get(0));
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocations() {
		Class<MetaLocationsFoo> testClass = MetaLocationsFoo.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverrides() {
		Class<MetaLocationsFooWithOverrides> testClass = MetaLocationsFooWithOverrides.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverriddenAttributes() {
		Class<MetaLocationsFooWithOverriddenAttributes> testClass = MetaLocationsFooWithOverriddenAttributes.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"foo1.xml", "foo2.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsInClassHierarchy() {
		Class<MetaLocationsBar> testClass = MetaLocationsBar.class;
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(testClass);
		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass, new String[] {"/bar.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
		assertAttributes(attributesList.get(1),
				MetaLocationsFoo.class, new String[] {"/foo.xml"}, EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithLocalAnnotationAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesFoo.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertClassesFooAttributes(attributesList.get(0));
	}

	@Test
	public void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndLocations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsBar.class);
		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertLocationsBarAttributes(attributesList.get(0));
		assertLocationsFooAttributes(attributesList.get(1));
	}

	@Test
	public void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(ClassesBar.class);
		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertClassesBarAttributes(attributesList.get(0));
		assertClassesFooAttributes(attributesList.get(1));
	}

	/**
	 * Verifies change requested in <a href="https://jira.spring.io/browse/SPR-11634">SPR-11634</a>.
	 * @since 4.0.4
	 */
	@Test
	public void resolveConfigAttributesWithLocationsAndClasses() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsAndClasses.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration(value = "x", locations = "y")
	private static class ConflictingLocations {
	}

	@ContextConfiguration(locations = "x", classes = Object.class)
	private static class LocationsAndClasses {
	}

}
