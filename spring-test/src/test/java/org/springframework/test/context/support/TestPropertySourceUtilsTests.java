/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;
import static org.springframework.test.context.support.TestPropertySourceUtils.addPropertiesFilesToEnvironment;
import static org.springframework.test.context.support.TestPropertySourceUtils.buildMergedTestPropertySources;
import static org.springframework.test.context.support.TestPropertySourceUtils.convertInlinedPropertiesToMap;

/**
 * Unit tests for {@link TestPropertySourceUtils}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class TestPropertySourceUtilsTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final String[] KEY_VALUE_PAIR = new String[] {"key = value"};

	private static final String[] FOO_LOCATIONS = new String[] {"classpath:/foo.properties"};


	@Test
	void emptyAnnotation() {
		assertThatIllegalStateException()
			.isThrownBy(() -> buildMergedTestPropertySources(EmptyPropertySources.class))
			.withMessageStartingWith("Could not detect default properties file for test class")
			.withMessageContaining("class path resource")
			.withMessageContaining("does not exist")
			.withMessageContaining("EmptyPropertySources.properties");
	}

	@Test
	void extendedEmptyAnnotation() {
		assertThatIllegalStateException()
			.isThrownBy(() -> buildMergedTestPropertySources(ExtendedEmptyPropertySources.class))
			.withMessageStartingWith("Could not detect default properties file for test")
			.withMessageContaining("class path resource")
			.withMessageContaining("does not exist")
			.withMessageContaining("ExtendedEmptyPropertySources.properties");
	}

	@Test
	void repeatedTestPropertySourcesWithConflictingInheritLocationsFlags() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> buildMergedTestPropertySources(RepeatedPropertySourcesWithConflictingInheritLocationsFlags.class))
			.withMessage("@TestPropertySource on RepeatedPropertySourcesWithConflictingInheritLocationsFlags and " +
				"@InheritLocationsFalseTestProperty on RepeatedPropertySourcesWithConflictingInheritLocationsFlags " +
				"must declare the same value for 'inheritLocations' as other directly present or meta-present @TestPropertySource annotations");
	}

	@Test
	void repeatedTestPropertySourcesWithConflictingInheritPropertiesFlags() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> buildMergedTestPropertySources(RepeatedPropertySourcesWithConflictingInheritPropertiesFlags.class))
			.withMessage("@TestPropertySource on RepeatedPropertySourcesWithConflictingInheritPropertiesFlags and " +
				"@InheritPropertiesFalseTestProperty on RepeatedPropertySourcesWithConflictingInheritPropertiesFlags " +
				"must declare the same value for 'inheritProperties' as other directly present or meta-present @TestPropertySource annotations");
	}

	@Test
	void value() {
		assertMergedTestPropertySources(ValuePropertySources.class, asArray("classpath:/value.xml"),
				EMPTY_STRING_ARRAY);
	}

	@Test
	void locationsAndValueAttributes() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
			.isThrownBy(() -> buildMergedTestPropertySources(LocationsAndValuePropertySources.class));
	}

	@Test
	void locationsAndProperties() {
		assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	@Test
	void inheritedLocationsAndProperties() {
		assertMergedTestPropertySources(InheritedPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	/**
	 * @since 5.3
	 */
	@Test
	void locationsAndPropertiesDuplicatedLocally() {
		assertMergedTestPropertySources(LocallyDuplicatedLocationsAndProperties.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	/**
	 * @since 5.3
	 */
	@Test
	void locationsAndPropertiesDuplicatedOnSuperclass() {
		assertMergedTestPropertySources(DuplicatedLocationsAndPropertiesPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	/**
	 * @since 5.3
	 */
	@Test
	void locationsAndPropertiesDuplicatedOnEnclosingClass() {
		assertMergedTestPropertySources(LocationsAndPropertiesPropertySources.Nested.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml"), asArray("k1a=v1a", "k1b: v1b"));
	}

	@Test
	void extendedLocationsAndProperties() {
		assertMergedTestPropertySources(ExtendedPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/bar1.xml", "classpath:/bar2.xml"),
				asArray("k1a=v1a", "k1b: v1b", "k2a v2a", "k2b: v2b"));
	}

	@Test
	void overriddenLocations() {
		assertMergedTestPropertySources(OverriddenLocationsPropertySources.class,
				asArray("classpath:/baz.properties"), asArray("k1a=v1a", "k1b: v1b", "key = value"));
	}

	@Test
	void overriddenProperties() {
		assertMergedTestPropertySources(OverriddenPropertiesPropertySources.class,
				asArray("classpath:/foo1.xml", "classpath:/foo2.xml", "classpath:/baz.properties"), KEY_VALUE_PAIR);
	}

	@Test
	void overriddenLocationsAndProperties() {
		assertMergedTestPropertySources(OverriddenLocationsAndPropertiesPropertySources.class,
				asArray("classpath:/baz.properties"), KEY_VALUE_PAIR);
	}

	@Test
	void addPropertiesFilesToEnvironmentWithNullContext() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addPropertiesFilesToEnvironment((ConfigurableApplicationContext) null, FOO_LOCATIONS))
			.withMessageContaining("'context' must not be null");
	}

	@Test
	void addPropertiesFilesToEnvironmentWithContextAndNullLocations() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addPropertiesFilesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null))
			.withMessageContaining("'locations' must not be null");
	}

	@Test
	void addPropertiesFilesToEnvironmentWithNullEnvironment() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addPropertiesFilesToEnvironment((ConfigurableEnvironment) null, mock(), FOO_LOCATIONS))
			.withMessageContaining("'environment' must not be null");
	}

	@Test
	void addPropertiesFilesToEnvironmentWithEnvironmentLocationsAndNullResourceLoader() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addPropertiesFilesToEnvironment(new MockEnvironment(), null, FOO_LOCATIONS))
			.withMessageContaining("'resourceLoader' must not be null");
	}

	@Test
	void addPropertiesFilesToEnvironmentWithEnvironmentAndNullLocations() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addPropertiesFilesToEnvironment(new MockEnvironment(), mock(), (String[]) null))
			.withMessageContaining("'locations' must not be null");
	}

	@Test
	void addPropertiesFilesToEnvironmentWithSinglePropertyFromVirtualFile() {
		ConfigurableEnvironment environment = new MockEnvironment();

		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertThat(propertySources).isEmpty();

		String pair = "key = value";
		ByteArrayResource resource = new ByteArrayResource(pair.getBytes(), "from inlined property: " + pair);
		ResourceLoader resourceLoader = mock();
		given(resourceLoader.getResource(anyString())).willReturn(resource);

		addPropertiesFilesToEnvironment(environment, resourceLoader, FOO_LOCATIONS);
		assertThat(propertySources).hasSize(1);
		assertThat(environment.getProperty("key")).isEqualTo("value");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithNullContext() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment((ConfigurableApplicationContext) null, KEY_VALUE_PAIR))
			.withMessageContaining("'context' must not be null");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithContextAndNullInlinedProperties() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment(mock(ConfigurableApplicationContext.class), (String[]) null))
			.withMessageContaining("'inlinedProperties' must not be null");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithNullEnvironment() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment((ConfigurableEnvironment) null, KEY_VALUE_PAIR))
			.withMessageContaining("'environment' must not be null");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithEnvironmentAndNullInlinedProperties() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment(new MockEnvironment(), (String[]) null))
			.withMessageContaining("'inlinedProperties' must not be null");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithMalformedUnicodeInValue() {
		assertThatIllegalStateException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment(new MockEnvironment(), asArray("key = \\uZZZZ")))
			.withMessageContaining("Failed to load test environment property");
	}

	@Test
	void addInlinedPropertiesToEnvironmentWithMultipleKeyValuePairsInSingleInlinedProperty() {
		assertThatIllegalStateException()
			.isThrownBy(() -> addInlinedPropertiesToEnvironment(new MockEnvironment(), asArray("a=b\nx=y")))
			.withMessageContaining("Failed to load exactly one test environment property");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void addInlinedPropertiesToEnvironmentWithEmptyProperty() {
		ConfigurableEnvironment environment = new MockEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertThat(propertySources).isEmpty();
		addInlinedPropertiesToEnvironment(environment, asArray("  "));
		assertThat(propertySources).hasSize(1);
		assertThat(((Map<?, ?>) propertySources.iterator().next().getSource())).isEmpty();
	}

	@Test
	void convertInlinedPropertiesToMapWithNullInlinedProperties() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> convertInlinedPropertiesToMap((String[]) null))
			.withMessageContaining("'inlinedProperties' must not be null");
	}


	private static void assertMergedTestPropertySources(Class<?> testClass, String[] expectedLocations,
			String[] expectedProperties) {

		MergedTestPropertySources mergedPropertySources = buildMergedTestPropertySources(testClass);
		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(mergedPropertySources).isNotNull();
			Stream<String> locations = mergedPropertySources.getPropertySourceDescriptors().stream()
					.map(PropertySourceDescriptor::locations).flatMap(List::stream);
			softly.assertThat(locations).containsExactly(expectedLocations);
			softly.assertThat(mergedPropertySources.getProperties()).isEqualTo(expectedProperties);
		});
	}


	@SafeVarargs
	private static <T> T[] asArray(T... arr) {
		return arr;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@TestPropertySource(locations = "foo.properties", inheritLocations = false)
	@interface InheritLocationsFalseTestProperty {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@TestPropertySource(properties = "a = b", inheritProperties = false)
	@interface InheritPropertiesFalseTestProperty {
	}

	@TestPropertySource
	static class EmptyPropertySources {
	}

	@TestPropertySource
	static class ExtendedEmptyPropertySources extends EmptyPropertySources {
	}

	@InheritLocationsFalseTestProperty
	@TestPropertySource(locations = "bar.properties", inheritLocations = true)
	static class RepeatedPropertySourcesWithConflictingInheritLocationsFlags {
	}

	@TestPropertySource(properties = "x = y", inheritProperties = true)
	@InheritPropertiesFalseTestProperty
	static class RepeatedPropertySourcesWithConflictingInheritPropertiesFlags {
	}

	@TestPropertySource(locations = "/foo", value = "/bar")
	static class LocationsAndValuePropertySources {
	}

	@TestPropertySource("/value.xml")
	static class ValuePropertySources {
	}

	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	static class LocationsAndPropertiesPropertySources {

		@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
		class Nested {
		}
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

	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	static class LocallyDuplicatedLocationsAndProperties {
	}

	@TestPropertySource(locations = { "/foo1.xml", "/foo2.xml" }, properties = { "k1a=v1a", "k1b: v1b" })
	static class DuplicatedLocationsAndPropertiesPropertySources extends LocationsAndPropertiesPropertySources {
	}

}
