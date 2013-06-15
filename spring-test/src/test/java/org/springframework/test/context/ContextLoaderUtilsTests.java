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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.context.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
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

	private void debugConfigAttributes(List<ContextConfigurationAttributes> configAttributesList) {
		// for (ContextConfigurationAttributes configAttributes : configAttributesList) {
		// System.err.println(configAttributes);
		// }
	}

	@Test(expected = IllegalStateException.class)
	public void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchy() {
		resolveContextHierarchyAttributes(SingleTestClassWithContextConfigurationAndContextHierarchy.class);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithImplicitSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(BareAnnotations.class);
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(SingleTestClassWithSingleLevelContextHierarchy.class);
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithTripleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(SingleTestClassWithTripleLevelContextHierarchy.class);
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(3, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithSingleLevelContextHierarchy.class);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertArrayEquals(new String[] { "two-A.xml", "two-B.xml" },
			configAttributesListClassLevel2.get(0).getLocations());

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertEquals(1, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("three.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSubclass() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSubclass.class);
		assertEquals(2, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("two.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSuperclass() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSuperclass.class);
		assertEquals(2, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("two.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithMultiLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithMultiLevelContextHierarchy.class);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(2, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("1-A.xml"));
		assertThat(configAttributesListClassLevel1.get(1).getLocations()[0], equalTo("1-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(2, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("2-A.xml"));
		assertThat(configAttributesListClassLevel2.get(1).getLocations()[0], equalTo("2-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertEquals(3, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("3-A.xml"));
		assertThat(configAttributesListClassLevel3.get(1).getLocations()[0], equalTo("3-B.xml"));
		assertThat(configAttributesListClassLevel3.get(2).getLocations()[0], equalTo("3-C.xml"));
	}

	private void assertContextConfigEntriesAreNotUnique(Class<?> testClass) {
		try {
			resolveContextHierarchyAttributes(testClass);
			fail("Should throw an IllegalStateException");
		}
		catch (IllegalStateException e) {
			String msg = String.format(
				"The @ContextConfiguration elements configured via @ContextHierarchy in test class [%s] must define unique contexts to load.",
				testClass.getName());
			assertEquals(msg, e.getMessage());
		}
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig.class);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig.class);
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchies() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchy.class);

		assertThat(map.size(), is(3));
		assertThat(map.keySet(), hasItems("alpha", "beta", "gamma"));

		List<ContextConfigurationAttributes> alphaConfig = map.get("alpha");
		assertThat(alphaConfig.size(), is(3));
		assertThat(alphaConfig.get(0).getLocations()[0], is("1-A.xml"));
		assertThat(alphaConfig.get(1).getLocations()[0], is("2-A.xml"));
		assertThat(alphaConfig.get(2).getLocations()[0], is("3-A.xml"));

		List<ContextConfigurationAttributes> betaConfig = map.get("beta");
		assertThat(betaConfig.size(), is(3));
		assertThat(betaConfig.get(0).getLocations()[0], is("1-B.xml"));
		assertThat(betaConfig.get(1).getLocations()[0], is("2-B.xml"));
		assertThat(betaConfig.get(2).getLocations()[0], is("3-B.xml"));

		List<ContextConfigurationAttributes> gammaConfig = map.get("gamma");
		assertThat(gammaConfig.size(), is(1));
		assertThat(gammaConfig.get(0).getLocations()[0], is("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndUnnamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig.class);

		String level1 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 1;
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;
		String level4 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 4;
		String level5 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 5;
		String level6 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 6;
		String level7 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 7;

		assertThat(map.size(), is(7));
		assertThat(map.keySet(), hasItems(level1, level2, level3, level4, level5, level6, level7));

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config.size(), is(1));
		assertThat(level1Config.get(0).getLocations()[0], is("1-A.xml"));

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config.size(), is(1));
		assertThat(level2Config.get(0).getLocations()[0], is("1-B.xml"));

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config.size(), is(1));
		assertThat(level3Config.get(0).getLocations()[0], is("2-A.xml"));

		// ...

		List<ContextConfigurationAttributes> level7Config = map.get(level7);
		assertThat(level7Config.size(), is(1));
		assertThat(level7Config.get(0).getLocations()[0], is("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndPartiallyNamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig.class);

		String level1 = "parent";
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;

		assertThat(map.size(), is(3));
		assertThat(map.keySet(), hasItems(level1, level2, level3));
		Iterator<String> levels = map.keySet().iterator();
		assertThat(levels.next(), is(level1));
		assertThat(levels.next(), is(level2));
		assertThat(levels.next(), is(level3));

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config.size(), is(2));
		assertThat(level1Config.get(0).getLocations()[0], is("1-A.xml"));
		assertThat(level1Config.get(1).getLocations()[0], is("2-A.xml"));

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config.size(), is(1));
		assertThat(level2Config.get(0).getLocations()[0], is("1-B.xml"));

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config.size(), is(1));
		assertThat(level3Config.get(0).getLocations()[0], is("2-C.xml"));
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
		buildMergedContextConfiguration(Enigma.class, null, null);
	}

	@Test
	public void buildMergedConfigWithBareAnnotations() {
		Class<BareAnnotations> testClass = BareAnnotations.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(
			mergedConfig,
			testClass,
			new String[] { "classpath:/org/springframework/test/context/ContextLoaderUtilsTests$BareAnnotations-context.xml" },
			EMPTY_CLASS_ARRAY, DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			DelegatingSmartContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndLocations() {
		Class<?> testClass = LocationsFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName(), null);

		assertMergedConfig(mergedConfig, testClass, new String[] { "classpath:/foo.xml" }, EMPTY_CLASS_ARRAY,
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAnnotationAndOverriddenContextLoaderAndClasses() {
		Class<?> testClass = ClassesFoo.class;
		Class<? extends ContextLoader> expectedContextLoaderClass = GenericPropertiesContextLoader.class;
		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass,
			expectedContextLoaderClass.getName(), null);

		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, new Class<?>[] { FooConfig.class },
			expectedContextLoaderClass);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndLocations() {
		Class<?> testClass = LocationsBar.class;
		String[] expectedLocations = new String[] { "/foo.xml", "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithLocalAndInheritedAnnotationsAndClasses() {
		Class<?> testClass = ClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { FooConfig.class, BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, EMPTY_STRING_ARRAY, expectedClasses,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenLocations() {
		Class<?> testClass = OverriddenLocationsBar.class;
		String[] expectedLocations = new String[] { "/bar.xml" };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
		assertMergedConfig(mergedConfig, testClass, expectedLocations, EMPTY_CLASS_ARRAY,
			AnnotationConfigContextLoader.class);
	}

	@Test
	public void buildMergedConfigWithAnnotationsAndOverriddenClasses() {
		Class<?> testClass = OverriddenClassesBar.class;
		Class<?>[] expectedClasses = new Class<?>[] { BarConfig.class };

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
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

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
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

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
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

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
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

		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass, null, null);
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

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithResolver() {
		String[] profiles = resolveActiveProfiles(FooActiveProfilesResolverTest.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithInheritedResolver() {
		String[] profiles = resolveActiveProfiles(InheritedFooActiveProfilesResolverTest.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertArrayEquals(new String[] { "foo" }, profiles);
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithMergedInheritedResolver() {
		String[] profiles = resolveActiveProfiles(MergedInheritedFooActiveProfilesResolverTest.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);
		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithOverridenInheritedResolver() {
		String[] profiles = resolveActiveProfiles(OverridenInheritedFooActiveProfilesResolverTest.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertArrayEquals(new String[] { "bar" }, profiles);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingResolverAndProfiles() {
		resolveActiveProfiles(ConflictingResolverAndProfilesTest.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingResolverAndValue() {
		resolveActiveProfiles(ConflictingResolverAndValueTest.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingProfilesAndValue() {
		resolveActiveProfiles(ConflictingProfilesAndValueTest.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithResolverWithoutDefaultConstructor() {
		resolveActiveProfiles(NoDefaultConstructorActiveProfilesResolverTest.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithResolverThatReturnsNull() {
		resolveActiveProfiles(NullActiveProfilesResolverTest.class);
	}


	// --- General Purpose Classes and Config ----------------------------------

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

	// --- ActiveProfilesResolver ----------------------------------------------

	public static class FooActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return new String[] { "foo" };
		}
	}

	public static class BarActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return new String[] { "bar" };
		}
	}

	public static class NullActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return null;
		}
	}

	public static class NoDefaultConstructorActiveProfilesResolver implements ActiveProfilesResolver {

		public NoDefaultConstructorActiveProfilesResolver(Object agument) {
		}

		@Override
		public String[] resolve(Class<?> testClass) {
			return null;
		}
	}

	@ActiveProfiles(resolver = NullActiveProfilesResolver.class)
	private static class NullActiveProfilesResolverTest {
	}

	@ActiveProfiles(resolver = NoDefaultConstructorActiveProfilesResolver.class)
	private static class NoDefaultConstructorActiveProfilesResolverTest {
	}

	@ActiveProfiles(resolver = FooActiveProfilesResolver.class)
	private static class FooActiveProfilesResolverTest {
	}

	private static class InheritedFooActiveProfilesResolverTest extends FooActiveProfilesResolverTest {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class)
	private static class MergedInheritedFooActiveProfilesResolverTest extends InheritedFooActiveProfilesResolverTest {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, inheritProfiles = false)
	private static class OverridenInheritedFooActiveProfilesResolverTest extends InheritedFooActiveProfilesResolverTest {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, profiles = "conflict")
	private static class ConflictingResolverAndProfilesTest {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, value = "conflict")
	private static class ConflictingResolverAndValueTest {
	}

	@ActiveProfiles(profiles = "conflict", value = "conflict")
	private static class ConflictingProfilesAndValueTest {
	}

	// --- ApplicationContextInitializer ---------------------------------------

	private static class FooInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
		}
	}

	private static class BarInitializer implements ApplicationContextInitializer<GenericWebApplicationContext> {

		@Override
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

	// --- @ContextHierarchy ---------------------------------------------------

	@ContextConfiguration("foo.xml")
	@ContextHierarchy(@ContextConfiguration("bar.xml"))
	private static class SingleTestClassWithContextConfigurationAndContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration("A.xml"))
	private static class SingleTestClassWithSingleLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration("A.xml"),//
		@ContextConfiguration("B.xml"),//
		@ContextConfiguration("C.xml") //
	})
	private static class SingleTestClassWithTripleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration("one.xml"))
	private static class TestClass1WithSingleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration({ "two-A.xml", "two-B.xml" }))
	private static class TestClass2WithSingleLevelContextHierarchy extends TestClass1WithSingleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration("three.xml"))
	private static class TestClass3WithSingleLevelContextHierarchy extends TestClass2WithSingleLevelContextHierarchy {
	}

	@ContextConfiguration("one.xml")
	private static class TestClass1WithBareContextConfigurationInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration("two.xml"))
	private static class TestClass2WithBareContextConfigurationInSuperclass extends
			TestClass1WithBareContextConfigurationInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration("one.xml"))
	private static class TestClass1WithBareContextConfigurationInSubclass {
	}

	@ContextConfiguration("two.xml")
	private static class TestClass2WithBareContextConfigurationInSubclass extends
			TestClass1WithBareContextConfigurationInSuperclass {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "1-B.xml", name = "beta") //
	})
	private static class TestClass1WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "2-B.xml", name = "beta") //
	})
	private static class TestClass2WithMultiLevelContextHierarchy extends TestClass1WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "3-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "3-B.xml", name = "beta"),//
		@ContextConfiguration(locations = "3-C.xml", name = "gamma") //
	})
	private static class TestClass3WithMultiLevelContextHierarchy extends TestClass2WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml"),//
		@ContextConfiguration(locations = "1-B.xml") //
	})
	private static class TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml"),//
		@ContextConfiguration(locations = "2-B.xml") //
	})
	private static class TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig extends
			TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "3-A.xml"),//
		@ContextConfiguration(locations = "3-B.xml"),//
		@ContextConfiguration(locations = "3-C.xml") //
	})
	private static class TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig extends
			TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml", name = "parent"),//
		@ContextConfiguration(locations = "1-B.xml") //
	})
	private static class TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml", name = "parent"),//
		@ContextConfiguration(locations = "2-C.xml") //
	})
	private static class TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig extends
			TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
	}

	@ContextHierarchy({
		//
		@ContextConfiguration,//
		@ContextConfiguration //
	})
	private static class SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig {
	}

	@ContextHierarchy({
		//
		@ContextConfiguration("foo.xml"),//
		@ContextConfiguration(classes = BarConfig.class),// duplicate!
		@ContextConfiguration("baz.xml"),//
		@ContextConfiguration(classes = BarConfig.class),// duplicate!
		@ContextConfiguration(loader = AnnotationConfigContextLoader.class) //
	})
	private static class SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig {
	}

}
