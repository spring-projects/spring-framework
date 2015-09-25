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

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.context.support.TestPropertySourceUtils.*;

/**
 * Unit tests for {@link TestPropertySourceUtils}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class TestPropertySourceUtilsTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String[] KEY_VALUE_PAIR = new String[] { "key = value" };

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
		expectedException.expect(AnnotationConfigurationException.class);
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
			"classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/baz.properties" }, KEY_VALUE_PAIR);
	}

	@Test
	public void overriddenLocationsAndProperties() {
		assertMergedTestPropertySources(OverriddenLocationsAndPropertiesPropertySources.class,
			new String[] { "classpath:/baz.properties" }, KEY_VALUE_PAIR);
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithNullContext() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("context");
		addInlinedPropertiesToEnvironment((ConfigurableApplicationContext) null, KEY_VALUE_PAIR);
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithContextAndNullInlinedProperties() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("inlined");
		addInlinedPropertiesToEnvironment(mock(ConfigurableApplicationContext.class), null);
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithNullEnvironment() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("environment");
		addInlinedPropertiesToEnvironment((ConfigurableEnvironment) null, KEY_VALUE_PAIR);
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithEnvironmentAndNullInlinedProperties() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("inlined");
		addInlinedPropertiesToEnvironment(new MockEnvironment(), null);
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithMalformedUnicodeInValue() {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("Failed to load test environment property");
		addInlinedPropertiesToEnvironment(new MockEnvironment(), new String[] { "key = \\uZZZZ" });
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	public void addInlinedPropertiesToEnvironmentWithMultipleKeyValuePairsInSingleInlinedProperty() {
		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("Failed to load exactly one test environment property");
		addInlinedPropertiesToEnvironment(new MockEnvironment(), new String[] { "a=b\nx=y" });
	}

	/**
	 * @since 4.1.5
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void addInlinedPropertiesToEnvironmentWithEmptyProperty() {
		ConfigurableEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertEquals(0, propertySources.size());
		addInlinedPropertiesToEnvironment(environment, new String[] { "  " });
		assertEquals(1, propertySources.size());
		assertEquals(0, ((Map) propertySources.iterator().next().getSource()).size());
	}

	@Test
	public void convertInlinedPropertiesToMapWithNullInlinedProperties() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("inlined");
		convertInlinedPropertiesToMap(null);
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
