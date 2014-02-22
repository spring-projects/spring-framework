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

package org.springframework.test.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.test.context.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils} involving resolution of active bean
 * definition profiles.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 3.1
 */
public class ContextLoaderUtilsActiveProfilesTests extends AbstractContextLoaderUtilsTests {

	private void assertResolvedProfiles(Class<?> testClass, String... expected) {
		assertNotNull(testClass);
		assertNotNull(expected);
		String[] actual = resolveActiveProfiles(testClass);
		Set<String> expectedSet = new HashSet<String>(Arrays.asList(expected));
		Set<String> actualSet = new HashSet<String>(Arrays.asList(actual));
		assertEquals(expectedSet, actualSet);
	}

	@Test
	public void resolveActiveProfilesWithoutAnnotation() {
		assertArrayEquals(EMPTY_STRING_ARRAY, resolveActiveProfiles(Enigma.class));
	}

	@Test
	public void resolveActiveProfilesWithNoProfilesDeclared() {
		assertArrayEquals(EMPTY_STRING_ARRAY, resolveActiveProfiles(BareAnnotations.class));
	}

	@Test
	public void resolveActiveProfilesWithEmptyProfiles() {
		assertArrayEquals(EMPTY_STRING_ARRAY, resolveActiveProfiles(EmptyProfiles.class));
	}

	@Test
	public void resolveActiveProfilesWithDuplicatedProfiles() {
		assertResolvedProfiles(DuplicatedProfiles.class, "foo", "bar", "baz");
	}

	@Test
	public void resolveActiveProfilesWithLocalAnnotation() {
		assertResolvedProfiles(LocationsFoo.class, "foo");
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotationAndLocations() {
		assertResolvedProfiles(InheritedLocationsFoo.class, "foo");
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotationAndClasses() {
		assertResolvedProfiles(InheritedClassesFoo.class, "foo");
	}

	@Test
	public void resolveActiveProfilesWithLocalAndInheritedAnnotations() {
		assertResolvedProfiles(LocationsBar.class, "foo", "bar");
	}

	@Test
	public void resolveActiveProfilesWithOverriddenAnnotation() {
		assertResolvedProfiles(Animals.class, "dog", "cat");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithMetaAnnotation() {
		assertResolvedProfiles(MetaLocationsFoo.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithMetaAnnotationAndOverrides() {
		assertResolvedProfiles(MetaLocationsFooWithOverrides.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithMetaAnnotationAndOverriddenAttributes() {
		assertResolvedProfiles(MetaLocationsFooWithOverriddenAttributes.class, "foo1", "foo2");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithLocalAndInheritedMetaAnnotations() {
		assertResolvedProfiles(MetaLocationsBar.class, "foo", "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithOverriddenMetaAnnotation() {
		assertResolvedProfiles(MetaAnimals.class, "dog", "cat");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithResolver() {
		assertResolvedProfiles(FooActiveProfilesResolverTestCase.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithInheritedResolver() {
		assertResolvedProfiles(InheritedFooActiveProfilesResolverTestCase.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithMergedInheritedResolver() {
		assertResolvedProfiles(MergedInheritedFooActiveProfilesResolverTestCase.class, "foo", "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	public void resolveActiveProfilesWithOverridenInheritedResolver() {
		assertResolvedProfiles(OverridenInheritedFooActiveProfilesResolverTestCase.class, "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingResolverAndProfiles() {
		resolveActiveProfiles(ConflictingResolverAndProfilesTestCase.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingResolverAndValue() {
		resolveActiveProfiles(ConflictingResolverAndValueTestCase.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithConflictingProfilesAndValue() {
		resolveActiveProfiles(ConflictingProfilesAndValueTestCase.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithResolverWithoutDefaultConstructor() {
		resolveActiveProfiles(NoDefaultConstructorActiveProfilesResolverTestCase.class);
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void resolveActiveProfilesWithResolverThatReturnsNull() {
		resolveActiveProfiles(NullActiveProfilesResolverTestCase.class);
	}

	/**
	 * This test verifies that the actual test class, not the composed annotation,
	 * is passed to the resolver.
	 *
	 * @since 4.0.3
	 */
	@Test
	public void resolveActiveProfilesWithMetaAnnotationAndTestClassVerifyingResolver() {
		Class<TestClassVerifyingActiveProfilesResolverTestCase> testClass = TestClassVerifyingActiveProfilesResolverTestCase.class;
		assertResolvedProfiles(testClass, testClass.getSimpleName());
	}


	// -------------------------------------------------------------------------

	@ActiveProfiles({ "    ", "\t" })
	private static class EmptyProfiles {
	}

	@ActiveProfiles({ "foo", "bar", "  foo", "bar  ", "baz" })
	private static class DuplicatedProfiles {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends LocationsBar {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private static @interface MetaAnimalsConfig {
	}

	@ActiveProfiles(resolver = TestClassVerifyingActiveProfilesResolver.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private static @interface MetaResolverConfig {
	}

	@MetaAnimalsConfig
	private static class MetaAnimals extends MetaLocationsBar {
	}

	private static class InheritedLocationsFoo extends LocationsFoo {
	}

	private static class InheritedClassesFoo extends ClassesFoo {
	}

	@ActiveProfiles(resolver = NullActiveProfilesResolver.class)
	private static class NullActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = NoDefaultConstructorActiveProfilesResolver.class)
	private static class NoDefaultConstructorActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = FooActiveProfilesResolver.class)
	private static class FooActiveProfilesResolverTestCase {
	}

	private static class InheritedFooActiveProfilesResolverTestCase extends FooActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class)
	private static class MergedInheritedFooActiveProfilesResolverTestCase extends
			InheritedFooActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, inheritProfiles = false)
	private static class OverridenInheritedFooActiveProfilesResolverTestCase extends
			InheritedFooActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, profiles = "conflict")
	private static class ConflictingResolverAndProfilesTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, value = "conflict")
	private static class ConflictingResolverAndValueTestCase {
	}

	@MetaResolverConfig
	private static class TestClassVerifyingActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(profiles = "conflict", value = "conflict")
	private static class ConflictingProfilesAndValueTestCase {
	}

	private static class FooActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return new String[] { "foo" };
		}
	}

	private static class BarActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return new String[] { "bar" };
		}
	}

	private static class NullActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return null;
		}
	}

	private static class NoDefaultConstructorActiveProfilesResolver implements ActiveProfilesResolver {

		@SuppressWarnings("unused")
		NoDefaultConstructorActiveProfilesResolver(Object agument) {
		}

		@Override
		public String[] resolve(Class<?> testClass) {
			return null;
		}
	}

	private static class TestClassVerifyingActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return testClass.isAnnotation() ? new String[] { "@" + testClass.getSimpleName() }
					: new String[] { testClass.getSimpleName() };
		}
	}

}
