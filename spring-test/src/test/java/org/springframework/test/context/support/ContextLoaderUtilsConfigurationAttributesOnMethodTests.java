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

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.CombinableMatcher.either;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.context.support.ContextLoaderUtils.resolveContextConfigurationAttributes;

/**
 * Unit tests for {@link ContextLoaderUtils} involving {@link ContextConfigurationAttributes} parsed from method-level
 * annotations.
 *
 * @author Sergei Ustimenko
 * @since 5.0
 */
public class ContextLoaderUtilsConfigurationAttributesOnMethodTests extends AbstractContextConfigurationUtilsTests {
	@Rule
	public final ExpectedException exception = ExpectedException.none();


	private void assertLocationsMethodFooAttributes(ContextConfigurationAttributes attributes) throws Exception {
		assertAttributes(attributes,
				LocationsMethodFoo.class,
				LocationsMethodFoo.class.getDeclaredMethod("something"),
				new String[] {"/foo.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				false);
	}

	private void assertClassesMethodFooAttributes(ContextConfigurationAttributes attributes) throws Exception {
		assertAttributes(attributes,
				ClassesMethodFoo.class,
				ClassesMethodFoo.class.getDeclaredMethod("something"),
				EMPTY_STRING_ARRAY,
				new Class<?>[] {FooConfig.class},
				ContextLoader.class,
				false);
	}

	private void assertLocationsMethodBarAttributes(ContextConfigurationAttributes attributes) throws Exception {
		assertAttributes(attributes,
				LocationsMethodBar.class,
				LocationsMethodBar.class.getDeclaredMethod("something"),
				new String[] {"/bar.xml"},
				EMPTY_CLASS_ARRAY,
				AnnotationConfigContextLoader.class,
				true);
	}

	private void assertClassesMethodBarAttributes(ContextConfigurationAttributes attributes) throws Exception {
		assertAttributes(attributes,
				ClassesMethodBar.class,
				ClassesMethodBar.class.getDeclaredMethod("something"),
				EMPTY_STRING_ARRAY,
				new Class<?>[] {BarConfig.class},
				AnnotationConfigContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithConflictingLocations() throws Exception {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(containsString(ConflictingLocations.class.getName()));
		exception.expectMessage(either(containsString("attribute 'value' and its alias 'locations'"))
				.or(containsString("attribute 'locations' and its alias 'value'")));
		exception.expectMessage(either(containsString("values of [{x}] and [{y}]")).or(containsString(
				"values of [{y}] and [{x}]")));
		exception.expectMessage(containsString("but only one is permitted"));
		resolveContextConfigurationAttributes(ConflictingLocations.class.getDeclaredMethod("something"));
	}

	@Test
	public void resolveConfigAttributesWithBareAnnotations() throws Exception {
		Class<BareMethodAnnotations> testClass = BareMethodAnnotations.class;
		Method something = testClass.getDeclaredMethod("something");
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(something);

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass,
				something,
				EMPTY_STRING_ARRAY,
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithLocalAnnotationAndLocations() throws Exception {
		List<ContextConfigurationAttributes> attributesList =
				resolveContextConfigurationAttributes(LocationsMethodFoo.class.getDeclaredMethod("something"));

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertLocationsMethodFooAttributes(attributesList.get(0));
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocations() throws Exception {
		Class<MetaLocationsMethodFoo> testClass = MetaLocationsMethodFoo.class;
		Method something = testClass.getDeclaredMethod("something");
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(something);

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass,
				something,
				new String[] {"/foo.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverrides() throws Exception {
		Class<MetaLocationsMethodFooWithOverrides> testClass = MetaLocationsMethodFooWithOverrides.class;
		Method something = testClass.getDeclaredMethod("something");
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(something);

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass,
				something,
				new String[] {"/foo.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsAndOverriddenAttributes() throws Exception {
		Class<MetaLocationsMethodFooWithOverriddenAttributes> testClass
				= MetaLocationsMethodFooWithOverriddenAttributes.class;
		Method something = testClass.getDeclaredMethod("something");
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(something);

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass,
				something,
				new String[] {"foo1.xml", "foo2.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithMetaAnnotationAndLocationsInClassHierarchy() throws Exception {
		Class<MetaLocationsMethodBar> testClass = MetaLocationsMethodBar.class;
		Method something = testClass.getDeclaredMethod("something");
		List<ContextConfigurationAttributes> attributesList = resolveContextConfigurationAttributes(something);

		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertAttributes(attributesList.get(0),
				testClass,
				something,
				new String[] {"/bar.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
		assertAttributes(attributesList.get(1),
				MetaLocationsMethodFoo.class,
				MetaLocationsMethodFoo.class.getDeclaredMethod("something"),
				new String[] {"/foo.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveConfigAttributesWithLocalAnnotationAndClasses() throws Exception {
		List<ContextConfigurationAttributes> attributesList =
				resolveContextConfigurationAttributes(ClassesMethodFoo.class.getDeclaredMethod("something"));

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertClassesMethodFooAttributes(attributesList.get(0));
	}

	@Test
	public void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndLocations() throws Exception {
		List<ContextConfigurationAttributes> attributesList =
				resolveContextConfigurationAttributes(LocationsMethodBar.class.getDeclaredMethod("something"));

		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertLocationsMethodBarAttributes(attributesList.get(0));
		assertLocationsMethodFooAttributes(attributesList.get(1));
	}

	@Test
	public void resolveConfigAttributesWithLocalAndInheritedAnnotationsAndClasses() throws Exception {
		List<ContextConfigurationAttributes> attributesList =
				resolveContextConfigurationAttributes(ClassesMethodBar.class.getDeclaredMethod("something"));

		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertClassesMethodBarAttributes(attributesList.get(0));
		assertClassesMethodFooAttributes(attributesList.get(1));
	}

	@Test
	public void resolveConfigAttributesWithLocationsAndClasses() throws Exception {
		List<ContextConfigurationAttributes> attributesList =
				resolveContextConfigurationAttributes(LocationsAndClasses.class.getDeclaredMethod("something"));

		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
	}


	// -------------------------------------------------------------------------

	private static class ConflictingLocations {
		@ContextConfiguration(value = "x", locations = "y")
		public void something() {
			// for test purposes
		}
	}

	private static class LocationsAndClasses {
		@ContextConfiguration(locations = "x", classes = Object.class)
		public void something() {
			// for test purposes
		}
	}
}
