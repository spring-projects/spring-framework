/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;
import static org.springframework.test.context.ContextLoaderUtils.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DelegatingSmartContextLoader;
import org.springframework.test.context.support.GenericPropertiesContextLoader;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Unit tests for {@link ContextLoaderUtils}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsTests {

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> EMPTY_INITIALIZER_CLASSES = //
	Collections.<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> emptySet();


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

	private void assertMergedConfig(MergedContextConfiguration mergedConfig, Class<?> expectedTestClass,
			String[] expectedLocations, Class<?>[] expectedClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass) {
		assertMergedConfig(mergedConfig, expectedTestClass, expectedLocations, expectedClasses,
			EMPTY_INITIALIZER_CLASSES, expectedContextLoaderClass);
	}

	private void assertMergedConfig(
			MergedContextConfiguration mergedConfig,
			Class<?> expectedTestClass,
			String[] expectedLocations,
			Class<?>[] expectedClasses,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses,
			Class<? extends ContextLoader> expectedContextLoaderClass) {
		assertNotNull(mergedConfig);
		assertEquals(expectedTestClass, mergedConfig.getTestClass());
		assertNotNull(mergedConfig.getLocations());
		assertArrayEquals(expectedLocations, mergedConfig.getLocations());
		assertNotNull(mergedConfig.getClasses());
		assertArrayEquals(expectedClasses, mergedConfig.getClasses());
		assertNotNull(mergedConfig.getActiveProfiles());
		assertEquals(expectedContextLoaderClass, mergedConfig.getContextLoader().getClass());
		assertNotNull(mergedConfig.getContextInitializerClasses());
		assertEquals(expectedInitializerClasses, mergedConfig.getContextInitializerClasses());
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

	@Test(expected = IllegalArgumentException.class)
	public void buildMergedConfigWithoutAnnotation() {
		buildMergedContextConfiguration(Enigma.class, null);
	}

	@Test
	public void buildMergedConfigWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);

		assertMergedConfig(
			mergedConfig,
			testClass,
			new String[] { "classpath:/org/springframework/test/context/ContextLoaderUtilsTests$BareAnnotations-context.xml" },
			EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName());

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName());

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndLocations() {
		Class<?> testClass = LocationsBar.class;
		String[] expectedLocations = new String[] { "/foo.xml", "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndClasses() {
		Class<?> testClass = ClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenLocations() {
		Class<?> testClass = OverriddenLocationsBar.class;
		String[] expectedLocations = new String[] { "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenClasses() {
		Class<?> testClass = OverriddenClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalInitializer() {
		Class<?> testClass = InitializersFoo.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(FooInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedInitializer() {
		Class<?> testClass = InitializersBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(FooInitializer.class);
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializers() {
		Class<?> testClass = OverriddenInitializersBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithOverriddenInitializersAndClasses() {
		Class<?> testClass = OverriddenInitializersAndClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> expectedInitializerClasses//
		= new HashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();
		expectedInitializerClasses.add(BarInitializer.class);

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses, expectedInitializerClasses,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void resolveActiveProfilesWithoutAnnotation() {
		String[] profiles = resolveActiveProfiles(Enigma.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithNoProfilesDeclared() {
		String[] profiles = resolveActiveProfiles(BareAnnotations.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithEmptyProfiles() {
		String[] profiles = resolveActiveProfiles(EmptyProfiles.class);
		assertArrayEquals(EMPTY_STRING_ARRAY, profiles);
	}

	@Test
	public void resolveActiveProfilesWithDuplicatedProfiles() {
		String[] profiles = resolveActiveProfiles(DuplicatedProfiles.class);
		assertNotNull(profiles);
		assertEquals(3, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("baz"));
	}

	@Test
	public void resolveActiveProfilesWithLocalAnnotation() {
		String[] profiles = resolveActiveProfiles(LocationsFoo.class);
		assertNotNull(profiles);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotationAndLocations() {
		String[] profiles = resolveActiveProfiles(InheritedLocationsFoo.class);
		assertNotNull(profiles);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotationAndClasses() {
		String[] profiles = resolveActiveProfiles(InheritedClassesFoo.class);
		assertNotNull(profiles);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	@Test
	public void resolveActiveProfilesWithLocalAndInheritedAnnotations() {
		String[] profiles = resolveActiveProfiles(LocationsBar.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
	}

	@Test
	public void resolveActiveProfilesWithOverriddenAnnotation() {
		String[] profiles = resolveActiveProfiles(Animals.class);
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

	@ContextConfiguration(locations = "/foo.xml", inheritLocations = false)
	@ActiveProfiles(profiles = "foo")
	private static class LocationsFoo {
	}

	@ContextConfiguration(classes = FooConfig.class, inheritLocations = false)
	@ActiveProfiles(profiles = "foo")
	private static class ClassesFoo {
	}

	private static class InheritedLocationsFoo extends LocationsFoo {
	}

	private static class InheritedClassesFoo extends ClassesFoo {
	}

	@Configuration
	private static class BarConfig {
	}

	@ContextConfiguration(locations = "/bar.xml", inheritLocations = true, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	private static class LocationsBar extends LocationsFoo {
	}

	@ContextConfiguration(locations = "/bar.xml", inheritLocations = false, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	private static class OverriddenLocationsBar extends LocationsFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = true, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	private static class ClassesBar extends ClassesFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = false, loader = AnnotationConfigContextLoader.class)
	@ActiveProfiles("bar")
	private static class OverriddenClassesBar extends ClassesFoo {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends LocationsBar {
	}

	private static class FooInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		public void initialize(GenericApplicationContext applicationContext) {
		}
	}

	private static class BarInitializer implements ApplicationContextInitializer<GenericWebApplicationContext> {

		public void initialize(GenericWebApplicationContext applicationContext) {
		}
	}

	@ContextConfiguration(classes = FooConfig.class, initializers = FooInitializer.class)
	private static class InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class)
	private static class InitializersBar extends InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, initializers = BarInitializer.class, inheritInitializers = false)
	private static class OverriddenInitializersBar extends InitializersFoo {
	}

	@ContextConfiguration(classes = BarConfig.class, inheritLocations = false, initializers = BarInitializer.class, inheritInitializers = false)
	private static class OverriddenInitializersAndClassesBar extends InitializersFoo {
	}

}
