/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.context.support.ActiveProfilesUtils.resolveActiveProfiles;

/**
 * Tests for {@link ActiveProfilesUtils} involving resolution of active bean
 * definition profiles.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 3.1
 */
class ActiveProfilesUtilsTests extends AbstractContextConfigurationUtilsTests {

	private void assertResolvedProfiles(Class<?> testClass, String... expected) {
		assertThat(resolveActiveProfiles(testClass)).isEqualTo(expected);
	}

	@Test
	void resolveActiveProfilesWithoutAnnotation() {
		assertResolvedProfiles(Enigma.class, EMPTY_STRING_ARRAY);
	}

	@Test
	void resolveActiveProfilesWithNoProfilesDeclared() {
		assertResolvedProfiles(BareAnnotations.class, EMPTY_STRING_ARRAY);
	}

	@Test
	void resolveActiveProfilesWithEmptyProfiles() {
		assertResolvedProfiles(EmptyProfiles.class, EMPTY_STRING_ARRAY);
	}

	@Test
	void resolveActiveProfilesWithDuplicatedProfiles() {
		assertResolvedProfiles(DuplicatedProfiles.class, "foo", "bar", "baz");
	}

	@Test
	void resolveActiveProfilesWithLocalAndInheritedDuplicatedProfiles() {
		assertResolvedProfiles(ExtendedDuplicatedProfiles.class, "foo", "bar", "baz", "cat", "dog");
	}

	@Test
	void resolveActiveProfilesWithLocalAnnotation() {
		assertResolvedProfiles(LocationsFoo.class, "foo");
	}

	@Test
	void resolveActiveProfilesWithInheritedAnnotationAndLocations() {
		assertResolvedProfiles(InheritedLocationsFoo.class, "foo");
	}

	@Test
	void resolveActiveProfilesWithInheritedAnnotationAndClasses() {
		assertResolvedProfiles(InheritedClassesFoo.class, "foo");
	}

	@Test
	void resolveActiveProfilesWithLocalAndInheritedAnnotations() {
		assertResolvedProfiles(LocationsBar.class, "foo", "bar");
	}

	@Test
	void resolveActiveProfilesWithOverriddenAnnotation() {
		assertResolvedProfiles(Animals.class, "dog", "cat");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithMetaAnnotation() {
		assertResolvedProfiles(MetaLocationsFoo.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithMetaAnnotationAndOverrides() {
		assertResolvedProfiles(MetaLocationsFooWithOverrides.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithMetaAnnotationAndOverriddenAttributes() {
		assertResolvedProfiles(MetaLocationsFooWithOverriddenAttributes.class, "foo1", "foo2");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithLocalAndInheritedMetaAnnotations() {
		assertResolvedProfiles(MetaLocationsBar.class, "foo", "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithOverriddenMetaAnnotation() {
		assertResolvedProfiles(MetaAnimals.class, "dog", "cat");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithResolver() {
		assertResolvedProfiles(FooActiveProfilesResolverTestCase.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithInheritedResolver() {
		assertResolvedProfiles(InheritedFooActiveProfilesResolverTestCase.class, "foo");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithMergedInheritedResolver() {
		assertResolvedProfiles(MergedInheritedFooActiveProfilesResolverTestCase.class, "foo", "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithOverriddenInheritedResolver() {
		assertResolvedProfiles(OverriddenInheritedFooActiveProfilesResolverTestCase.class, "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithResolverAndProfiles() {
		assertResolvedProfiles(ResolverAndProfilesTestCase.class, "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithResolverAndValue() {
		assertResolvedProfiles(ResolverAndValueTestCase.class, "bar");
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithConflictingProfilesAndValue() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				resolveActiveProfiles(ConflictingProfilesAndValueTestCase.class));
	}

	/**
	 * @since 4.0
	 */
	@Test
	void resolveActiveProfilesWithResolverWithoutDefaultConstructor() {
		assertThatIllegalStateException().isThrownBy(() ->
				resolveActiveProfiles(NoDefaultConstructorActiveProfilesResolverTestCase.class));
	}

	/**
	 * @since 4.0
	 */
	void resolveActiveProfilesWithResolverThatReturnsNull() {
		assertResolvedProfiles(NullActiveProfilesResolverTestCase.class);
	}

	/**
	 * This test verifies that the actual test class, not the composed annotation,
	 * is passed to the resolver.
	 * @since 4.0.3
	 */
	@Test
	void resolveActiveProfilesWithMetaAnnotationAndTestClassVerifyingResolver() {
		Class<TestClassVerifyingActiveProfilesResolverTestCase> testClass = TestClassVerifyingActiveProfilesResolverTestCase.class;
		assertResolvedProfiles(testClass, testClass.getSimpleName());
	}

	/**
	 * This test verifies that {@link DefaultActiveProfilesResolver} can be declared explicitly.
	 * @since 4.1.5
	 */
	@Test
	void resolveActiveProfilesWithDefaultActiveProfilesResolver() {
		assertResolvedProfiles(DefaultActiveProfilesResolverTestCase.class, "default");
	}

	/**
	 * This test verifies that {@link DefaultActiveProfilesResolver} can be extended.
	 * @since 4.1.5
	 */
	@Test
	void resolveActiveProfilesWithExtendedDefaultActiveProfilesResolver() {
		assertResolvedProfiles(ExtendedDefaultActiveProfilesResolverTestCase.class, "default", "foo");
	}


	// -------------------------------------------------------------------------

	@ActiveProfiles({ "    ", "\t" })
	private static class EmptyProfiles {
	}

	@ActiveProfiles({ "foo", "bar", "  foo", "bar  ", "baz" })
	private static class DuplicatedProfiles {
	}

	@ActiveProfiles({ "cat", "dog", "  foo", "bar  ", "cat" })
	private static class ExtendedDuplicatedProfiles extends DuplicatedProfiles {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends LocationsBar {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private @interface MetaAnimalsConfig {
	}

	@ActiveProfiles(resolver = TestClassVerifyingActiveProfilesResolver.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private @interface MetaResolverConfig {
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
	private static class OverriddenInheritedFooActiveProfilesResolverTestCase extends
			InheritedFooActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, profiles = "ignored by custom resolver")
	private static class ResolverAndProfilesTestCase {
	}

	@ActiveProfiles(resolver = BarActiveProfilesResolver.class, value = "ignored by custom resolver")
	private static class ResolverAndValueTestCase {
	}

	@MetaResolverConfig
	private static class TestClassVerifyingActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(profiles = "default", resolver = DefaultActiveProfilesResolver.class)
	private static class DefaultActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(profiles = "default", resolver = ExtendedDefaultActiveProfilesResolver.class)
	private static class ExtendedDefaultActiveProfilesResolverTestCase {
	}

	@ActiveProfiles(profiles = "conflict 1", value = "conflict 2")
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
		NoDefaultConstructorActiveProfilesResolver(Object argument) {
		}

		@Override
		public String[] resolve(Class<?> testClass) {
			return null;
		}
	}

	private static class TestClassVerifyingActiveProfilesResolver implements ActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			return testClass.isAnnotation() ? new String[] { "@" + testClass.getSimpleName() } :
					new String[] { testClass.getSimpleName() };
		}
	}

	private static class ExtendedDefaultActiveProfilesResolver extends DefaultActiveProfilesResolver {

		@Override
		public String[] resolve(Class<?> testClass) {
			List<String> profiles = new ArrayList<>(Arrays.asList(super.resolve(testClass)));
			profiles.add("foo");
			return StringUtils.toStringArray(profiles);
		}
	}

}
