/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.*;
import static org.springframework.test.context.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils} involving {@link ContextConfigurationAttributes}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsConfigurationAttributesTests extends AbstractContextLoaderUtilsTests {

	private void assertAttributes(ContextConfigurationAttributes attributes, Class<?> expectedDeclaringClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass, boolean expectedInheritLocations) {
		assertEquals(expectedDeclaringClass, attributes.getDeclaringClass());
		assertArrayEquals(expectedLocations, attributes.getLocations());
		assertArrayEquals(expectedClasses, attributes.getClasses());
		assertEquals(expectedInheritLocations, attributes.isInheritLocations());
		assertEquals(expectedContextLoaderClass, attributes.getContextLoaderClass());
	}

	private void assertLocationsFooAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, LocationsFoo.class, new String[] { "/foo.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, false);
	}

	private void assertClassesFooAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, ClassesFoo.class, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			ContextLoader.class, false);
	}

	private void assertLocationsBarAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, LocationsBar.class, new String[] { "/bar.xml" }, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class, true);
	}

	private void assertClassesBarAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, ClassesBar.class, EMPTY_STRING_ARRAY, new Class<?>[] { BarConfig.class },
			AnnotationConfigContextLoader.class, true);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveConfigAttributesWithConflictingLocations() {
		resolveContextConfigurationAttributes(ConflictingLocations.class);
	}

	@Test
	public void resolveConfigAttributesWithBareAnnotations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(BareAnnotations.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0), BareAnnotations.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
	}

	@Test
	public void resolveConfigAttributesWithLocalAnnotationAndLocations() {
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(LocationsFoo.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertLocationsFooAttributes(attributesList.get(0));
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


	@ContextConfiguration(value = "x", locations = "y")
	private static class ConflictingLocations {
	}

}
