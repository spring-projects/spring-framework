/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.GenericPropertiesContextLoader;
import org.springframework.test.context.support.GenericXmlContextLoader;

/**
 * Unit tests for {@link ContextLoaderUtils}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[] {};


	private void assertAttributes(ContextConfigurationAttributes attributes, Class<?> expectedDeclaringClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass, boolean expectedInheritLocations) {
		assertEquals(expectedDeclaringClass, attributes.getDeclaringClass());
		assertArrayEquals(expectedLocations, attributes.getLocations());
		assertArrayEquals(expectedClasses, attributes.getClasses());
		assertEquals(expectedInheritLocations, attributes.isInheritLocations());
		assertEquals(expectedContextLoaderClass, attributes.getContextLoader());
	}

	private void assertFooAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, Foo.class, new String[] { "/foo.xml" }, new Class<?>[] { FooConfig.class },
			ContextLoader.class, false);
	}

	private void assertBarAttributes(ContextConfigurationAttributes attributes) {
		assertAttributes(attributes, Bar.class, new String[] { "/bar.xml" }, new Class<?>[] { BarConfig.class },
			AnnotationConfigContextLoader.class, true);
	}

	private void assertMergedContextConfiguration(MergedContextConfiguration mergedConfig, Class<?> expectedTestClass,
			String[] expectedLocations, Class<? extends ContextLoader> expectedContextLoaderClass) {
		assertNotNull(mergedConfig);
		assertEquals(expectedTestClass, mergedConfig.getTestClass());
		assertNotNull(mergedConfig.getLocations());
		assertArrayEquals(expectedLocations, mergedConfig.getLocations());
		assertNotNull(mergedConfig.getClasses());
		assertNotNull(mergedConfig.getActiveProfiles());
		assertEquals(expectedContextLoaderClass, mergedConfig.getContextLoader().getClass());
	}

	@Test(expected = IllegalStateException.class)
	public void resolveContextConfigurationAttributesWithConflictingLocations() {
		ContextLoaderUtils.resolveContextConfigurationAttributes(ConflictingLocations.class);
	}

	@Test
	public void resolveContextConfigurationAttributesWithBareAnnotations() {
		List<ContextConfigurationAttributes> attributesList = ContextLoaderUtils.resolveContextConfigurationAttributes(BareAnnotations.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertAttributes(attributesList.get(0), BareAnnotations.class, EMPTY_STRING_ARRAY, new Class<?>[] {},
			ContextLoader.class, true);
	}

	@Test
	public void resolveContextConfigurationAttributesWithLocalAnnotation() {
		List<ContextConfigurationAttributes> attributesList = ContextLoaderUtils.resolveContextConfigurationAttributes(Foo.class);
		assertNotNull(attributesList);
		assertEquals(1, attributesList.size());
		assertFooAttributes(attributesList.get(0));
	}

	@Test
	public void resolveContextConfigurationAttributesWithLocalAndInheritedAnnotations() {
		List<ContextConfigurationAttributes> attributesList = ContextLoaderUtils.resolveContextConfigurationAttributes(Bar.class);
		assertNotNull(attributesList);
		assertEquals(2, attributesList.size());
		assertFooAttributes(attributesList.get(0));
		assertBarAttributes(attributesList.get(1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildMergedContextConfigurationWithoutAnnotation() {
		ContextLoaderUtils.buildMergedContextConfiguration(Enigma.class, null);
	}

	@Test
	public void buildMergedContextConfigurationWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = ContextLoaderUtils.buildMergedContextConfiguration(testClass, null);

		assertMergedContextConfiguration(
			mergedConfig,
			testClass,
			new String[] { "classpath:/org/springframework/test/context/ContextLoaderUtilsTests$BareAnnotations-context.xml" },
			GenericXmlContextLoader.class);
	}

	@Test
	public void buildMergedContextConfigurationWithLocalAnnotation() {
		Class<?> testClass = Foo.class;
		MergedContextConfiguration mergedConfig = ContextLoaderUtils.buildMergedContextConfiguration(testClass, null);

		assertMergedContextConfiguration(mergedConfig, testClass, new String[] { "classpath:/foo.xml" },
			GenericXmlContextLoader.class);
	}

	@Test
	public void buildMergedContextConfigurationWithLocalAnnotationAndOverriddenContexLoader() {
		Class<?> testClass = Foo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = ContextLoaderUtils.buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName());

		assertMergedContextConfiguration(mergedConfig, testClass, new String[] { "classpath:/foo.xml" },
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedContextConfigurationWithLocalAndInheritedAnnotations() {
		Class<?> testClass = Bar.class;
		MergedContextConfiguration mergedConfig = ContextLoaderUtils.buildMergedContextConfiguration(testClass, null);

		// TODO Assert @Configuration classes instead of locations
		String[] expectedLocations = new String[] {
			"org.springframework.test.context.ContextLoaderUtilsTests$FooConfig",
			"org.springframework.test.context.ContextLoaderUtilsTests$BarConfig" };

		assertMergedContextConfiguration(mergedConfig, testClass, expectedLocations,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void resolveActiveProfilesWithoutAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Enigma.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithNoProfilesDeclared() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(BareAnnotations.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithEmptyProfiles() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(EmptyProfiles.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithDuplicatedProfiles() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(DuplicatedProfiles.class);
		assertNotNull(profiles);
		assertEquals(3, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("baz"));
	}

	@Test
	public void resolveActiveProfilesWithLocalAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Foo.class);
		assertNotNull(profiles);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(InheritedFoo.class);
		assertNotNull(profiles);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	@Test
	public void resolveActiveProfilesWithLocalAndInheritedAnnotations() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Bar.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
	}

	@Test
	public void resolveActiveProfilesWithOverriddenAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Animals.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("dog"));
		assertTrue(list.contains("cat"));
	}


	private static class Enigma {
	}

	@ContextConfiguration(value = "x", locations = "y")
	private static class ConflictingLocations {
	}

	@ContextConfiguration
	@ActiveProfiles
	private static class BareAnnotations {
	}

	@ActiveProfiles({ "    ", "\t" })
	private static class EmptyProfiles {
	}

	@ActiveProfiles({ "foo", "bar", "  foo", "bar  ", "baz" })
	private static class DuplicatedProfiles {
	}

	@Configuration
	private static class FooConfig {
	}

	@ContextConfiguration(locations = "/foo.xml", classes = FooConfig.class, inheritLocations = false)
	@ActiveProfiles(profiles = "foo")
	private static class Foo {
	}

	private static class InheritedFoo extends Foo {
	}

	@Configuration
	private static class BarConfig {
	}

	@ContextConfiguration(locations = "/bar.xml", classes = BarConfig.class, inheritLocations = true, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	private static class Bar extends Foo {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends Bar {
	}

}
