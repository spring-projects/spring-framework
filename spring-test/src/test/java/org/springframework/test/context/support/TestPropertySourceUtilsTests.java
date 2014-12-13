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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.context.support.TestPropertySourceUtils.*;

/**
 * Unit tests for {@link TestPropertySourceUtils}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class TestPropertySourceUtilsTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	@Rule
	public ExpectedException expectedException = ExpectedException.none();


	private void assertMergedTestPropertySources(Class<?> testClass, String[] expectedLocations,
			String[] expectedProperties) {
		MergedTestPropertySources mergedPropertySources = buildMergedTestPropertySources(testClass);
		assertNotNull(mergedPropertySources);
		assertArrayEquals(expectedLocations, mergedPropertySources.getLocations());
		assertArrayEquals(expectedProperties, mergedPropertySources.getProperties());
	}

	@Test
	public void emptyAnnotation() {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage(startsWith("Could not detect default properties file for test"));
		expectedException.expectMessage(containsString("EmptyPropertySources.properties"));
		buildMergedTestPropertySources(EmptyPropertySources.class);
	}

	@Test
	public void extendedEmptyAnnotation() {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage(startsWith("Could not detect default properties file for test"));
		expectedException.expectMessage(containsString("ExtendedEmptyPropertySources.properties"));
		buildMergedTestPropertySources(ExtendedEmptyPropertySources.class);
	}

	@Test
	public void value() {
		assertMergedTestPropertySources(ValuePropertySources.class, new String[] { "classpath:/value.xml" },
			EMPTY_STRING_ARRAY);
	}

	@Test
	public void locationsAndValueAttributes() {
		expectedException.expect(IllegalStateException.class);
		buildMergedTestPropertySources(LocationsAndValuePropertySources.class);
	}

	@Test
	public void locationsAndProperties() {
		assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.class, new String[] {
			"classpath:/foo1.xml", "classpath:/foo2.xml" }, new String[] { "k1a=v1a", "k1b: v1b" });
	}

	@Test
	public void inheritedLocationsAndProperties() {
		assertMergedTestPropertySources(InheritedPropertySources.class, new String[] { "classpath:/foo1.xml",
			"classpath:/foo2.xml" }, new String[] { "k1a=v1a", "k1b: v1b" });
	}

	@Test
	public void extendedLocationsAndProperties() {
		assertMergedTestPropertySources(ExtendedPropertySources.class, new String[] { "classpath:/foo1.xml",
			"classpath:/foo2.xml", "classpath:/bar1.xml", "classpath:/bar2.xml" }, new String[] { "k1a=v1a",
			"k1b: v1b", "k2a v2a", "k2b: v2b" });
	}

	@Test
	public void overriddenLocations() {
		assertMergedTestPropertySources(OverriddenLocationsPropertySources.class,
			new String[] { "classpath:/baz.properties" }, new String[] { "k1a=v1a", "k1b: v1b", "key = value" });
	}

	@Test
	public void overriddenProperties() {
		assertMergedTestPropertySources(OverriddenPropertiesPropertySources.class, new String[] {
			"classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/baz.properties" }, new String[] { "key = value" });
	}

	@Test
	public void overriddenLocationsAndProperties() {
		assertMergedTestPropertySources(OverriddenLocationsAndPropertiesPropertySources.class,
			new String[] { "classpath:/baz.properties" }, new String[] { "key = value" });
	}


	// -------------------------------------------------------------------

	@TestPropertySource
	static class EmptyPropertySources {
	}

	@TestPropertySource
	static class ExtendedEmptyPropertySources extends EmptyPropertySources {
	}

	@TestPropertySource(locations = "/foo", value = "/bar")
	static class LocationsAndValuePropertySources {
	}

	@TestPropertySource("/value.xml")
	static class ValuePropertySources {
	}

	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	static class LocationsAndPropertiesPropertySources {
	}

	static class InheritedPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = { "/bar1.xml", "/bar2.xml" }, properties = { "k2a v2a", "k2b: v2b" })
	static class ExtendedPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false)
	static class OverriddenLocationsPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritProperties = false)
	static class OverriddenPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
	}

	@TestPropertySource(locations = "/baz.properties", properties = "key = value", inheritLocations = false, inheritProperties = false)
	static class OverriddenLocationsAndPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
	}

}
