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

package org.springframework.core.annotation;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.core.annotation.MergedAnnotation.Adapt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

/**
 * Tests for {@link TypeMappedAnnotation}. See also {@link MergedAnnotationsTests}
 * for a much more extensive collection of tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class TypeMappedAnnotationTests {

	@Test
	void mappingWhenMirroredReturnsMirroredValues() {
		testExplicitMirror(WithExplicitMirrorA.class);
		testExplicitMirror(WithExplicitMirrorB.class);
	}

	private void testExplicitMirror(Class<?> annotatedClass) {
		TypeMappedAnnotation<ExplicitMirror> annotation = getTypeMappedAnnotation(
				annotatedClass, ExplicitMirror.class);
		assertThat(annotation.getString("a")).isEqualTo("test");
		assertThat(annotation.getString("b")).isEqualTo("test");
	}

	@Test
	void mappingExplicitAliasToMetaAnnotationReturnsMappedValues() {
		TypeMappedAnnotation<?> annotation = getTypeMappedAnnotation(
				WithExplicitAliasToMetaAnnotation.class,
				ExplicitAliasToMetaAnnotation.class,
				ExplicitAliasMetaAnnotationTarget.class);
		assertThat(annotation.getString("aliased")).isEqualTo("aliased");
		assertThat(annotation.getString("nonAliased")).isEqualTo("nonAliased");
	}

	@Test
	void mappingConventionAliasToMetaAnnotationReturnsMappedValues() {
		TypeMappedAnnotation<?> annotation = getTypeMappedAnnotation(
				WithConventionAliasToMetaAnnotation.class,
				ConventionAliasToMetaAnnotation.class);
		assertThat(annotation.getString("value")).isEqualTo("value");
		assertThat(annotation.getString("convention")).isEqualTo("convention");

		annotation = getTypeMappedAnnotation(
				WithConventionAliasToMetaAnnotation.class,
				ConventionAliasToMetaAnnotation.class,
				ConventionAliasMetaAnnotationTarget.class);
		assertThat(annotation.getString("value")).isEmpty();
		// Convention-based annotation attribute overrides are no longer supported as of
		// Spring Framework 7.0. Otherwise, we would expect "convention".
		assertThat(annotation.getString("convention")).isEmpty();
	}

	@Test
	void adaptFromEmptyArrayToAnyComponentType() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(ArrayTypes.class);
		Map<String, Object> attributes = new HashMap<>();
		for (int i = 0; i < methods.size(); i++) {
			attributes.put(methods.get(i).getName(), new Object[] {});
		}
		MergedAnnotation<ArrayTypes> annotation = TypeMappedAnnotation.of(null, null,
				ArrayTypes.class, attributes);
		assertThat(annotation.getValue("stringValue")).contains(new String[] {});
		assertThat(annotation.getValue("byteValue")).contains(new byte[] {});
		assertThat(annotation.getValue("shortValue")).contains(new short[] {});
		assertThat(annotation.getValue("intValue")).contains(new int[] {});
		assertThat(annotation.getValue("longValue")).contains(new long[] {});
		assertThat(annotation.getValue("booleanValue")).contains(new boolean[] {});
		assertThat(annotation.getValue("charValue")).contains(new char[] {});
		assertThat(annotation.getValue("doubleValue")).contains(new double[] {});
		assertThat(annotation.getValue("floatValue")).contains(new float[] {});
		assertThat(annotation.getValue("classValue")).contains(new Class<?>[] {});
		assertThat(annotation.getValue("annotationValue")).contains(new MergedAnnotation<?>[] {});
		assertThat(annotation.getValue("enumValue")).contains(new ExampleEnum[] {});
	}

	@Test
	void adaptFromNestedMergedAnnotation() {
		MergedAnnotation<NestedAnnotation> nested = MergedAnnotation.of(NestedAnnotation.class);
		MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null,
				NestedContainer.class, Collections.singletonMap("value", nested));
		assertThat(annotation.getAnnotation("value", NestedAnnotation.class)).isSameAs(nested);
	}

	@Test
	void adaptFromStringToClass() {
		MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null,
				ClassAttributes.class,
				Collections.singletonMap("classValue", InputStream.class.getName()));
		assertThat(annotation.getString("classValue")).isEqualTo(InputStream.class.getName());
		assertThat(annotation.getClass("classValue")).isEqualTo(InputStream.class);
	}

	@Test  // gh-36606
	void adaptFromStringToClassWithMemberSourceUsesMemberClassLoader() throws Exception {
		OverridingClassLoader classLoader = new OverridingClassLoader(getClass().getClassLoader()) {
			@Override
			protected boolean isEligibleForOverriding(String className) {
				return ClassAttributes.class.getName().equals(className);
			}
		};
		Class<?> sourceClass = classLoader.loadClass(ClassAttributes.class.getName());
		Method sourceMethod = sourceClass.getDeclaredMethod("classValue");

		MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, sourceMethod,
				ClassAttributes.class, Map.of("classValue", sourceClass.getName()));

		assertThat(annotation.getClass("classValue").getClassLoader()).isSameAs(classLoader);
	}

	@Test
	void adaptFromStringArrayToClassArray() {
		MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null, ClassAttributes.class,
				Collections.singletonMap("classArrayValue", new String[] { InputStream.class.getName() }));
		assertThat(annotation.getStringArray("classArrayValue")).containsExactly(InputStream.class.getName());
		assertThat(annotation.getClassArray("classArrayValue")).containsExactly(InputStream.class);
	}


	@Nested
	class AsMapTests {

		@Test  // gh-36586
		void fromStringToUnresolvableClass() {
			var attributeName = "classValue";
			var mergedAnnotation = MergedAnnotation.of(null, null, ClassAttributes.class,
					Map.of(attributeName, "com.example.DoesNotExist"));

			// 0) Sanity check MergedAnnotation.getClass() behavior.
			assertThatExceptionOfType(TypeNotPresentException.class)
					.isThrownBy(() -> mergedAnnotation.getClass(attributeName))
					.withMessageContaining("com.example.DoesNotExist")
					.withCauseExactlyInstanceOf(ClassNotFoundException.class);

			var map = mergedAnnotation.asMap(Adapt.values(false, true));

			// 1) Attribute should be present, even though its value is an exception.
			assertThat(map).containsKey(attributeName);
			assertThat(map.get(attributeName)).asInstanceOf(throwable(TypeNotPresentException.class))
					.hasMessageContaining("com.example.DoesNotExist");
		}

		@Test  // gh-36586
		void fromStringArrayToUnresolvableClass() {
			var attributeName = "classArrayValue";
			var mergedAnnotation = MergedAnnotation.of(null, null, ClassAttributes.class,
					Map.of(attributeName, new String[] { "com.example.DoesNotExist" }));

			// 0) Sanity check MergedAnnotation.getClassArray() behavior.
			assertThatExceptionOfType(TypeNotPresentException.class)
					.isThrownBy(() -> mergedAnnotation.getClassArray(attributeName))
					.withMessageContaining("com.example.DoesNotExist")
					.withCauseExactlyInstanceOf(ClassNotFoundException.class);

			var map = mergedAnnotation.asMap(Adapt.values(false, true));

			// 1) Attribute should be present, even though its value is an exception.
			assertThat(map).containsKey(attributeName);
			assertThat(map.get(attributeName)).asInstanceOf(throwable(TypeNotPresentException.class))
					.hasMessageContaining("com.example.DoesNotExist");
		}
	}


	@Nested
	class AsAnnotationAttributesTests {

		@Test  // gh-36586
		void fromStringToUnresolvableClass() {
			var attributeName = "classValue";
			var mergedAnnotation = MergedAnnotation.of(null, null, ClassAttributes.class,
					Map.of(attributeName, "com.example.DoesNotExist"));

			// 0) Sanity check MergedAnnotation.getClass() behavior.
			assertThatExceptionOfType(TypeNotPresentException.class)
					.isThrownBy(() -> mergedAnnotation.getClass(attributeName))
					.withMessageContaining("com.example.DoesNotExist")
					.withCauseExactlyInstanceOf(ClassNotFoundException.class);

			var attributes = mergedAnnotation.asAnnotationAttributes(Adapt.values(false, true));

			// 1) Attribute should be present, even though its value is an exception.
			assertThat(attributes).containsKey(attributeName);
			assertThat(attributes.get(attributeName)).asInstanceOf(throwable(TypeNotPresentException.class))
					.hasMessageContaining("com.example.DoesNotExist");

			// 2) Accessing the attribute via AnnotationAttributes.getClassArray() should throw an
			// IllegalArgumentException with the IllegalArgumentException from ClassUtils.resolveClassName()
			// as its cause.
			assertAttributeAccessException(attributeName, () -> attributes.getClass(attributeName));
		}

		@Test  // gh-36586
		void fromStringArrayToUnresolvableClass() {
			var attributeName = "classArrayValue";
			var mergedAnnotation = MergedAnnotation.of(null, null, ClassAttributes.class,
					Map.of(attributeName, new String[] { "com.example.DoesNotExist" }));

			// 0) Sanity check MergedAnnotation.getClassArray() behavior.
			assertThatExceptionOfType(TypeNotPresentException.class)
					.isThrownBy(() -> mergedAnnotation.getClassArray(attributeName))
					.withMessageContaining("com.example.DoesNotExist")
					.withCauseExactlyInstanceOf(ClassNotFoundException.class);

			var attributes = mergedAnnotation.asAnnotationAttributes(Adapt.values(false, true));

			// 1) Attribute should be present, even though its value is an exception.
			assertThat(attributes).containsKey(attributeName);
			assertThat(attributes.get(attributeName)).asInstanceOf(throwable(TypeNotPresentException.class))
					.hasMessageContaining("com.example.DoesNotExist");

			// 2) Accessing the attribute via AnnotationAttributes.getClassArray() should throw an
			// IllegalArgumentException with the IllegalArgumentException from ClassUtils.resolveClassName()
			// as its cause.
			assertAttributeAccessException(attributeName, () -> attributes.getClassArray(attributeName));
		}

		private static void assertAttributeAccessException(String attributeName, ThrowingCallable throwingCallable) {
			assertThatIllegalArgumentException()
					.isThrownBy(throwingCallable)
					.withMessageMatching("""
							Attribute '%s' for annotation \\[.+?\\] was not resolvable \
							due to exception \\[.+?TypeNotPresentException.+?\\]""".formatted(attributeName))
					.havingCause()
							.isExactlyInstanceOf(TypeNotPresentException.class)
							.withMessageContaining("com.example.DoesNotExist")
							.havingCause()
									.isExactlyInstanceOf(ClassNotFoundException.class)
									.withMessageContaining("com.example.DoesNotExist");
		}
	}


	private <A extends Annotation> TypeMappedAnnotation<A> getTypeMappedAnnotation(
			Class<?> source, Class<A> annotationType) {
		return getTypeMappedAnnotation(source, annotationType, annotationType);
	}

	private <A extends Annotation> TypeMappedAnnotation<A> getTypeMappedAnnotation(
			Class<?> source, Class<? extends Annotation> rootAnnotationType,
			Class<A> annotationType) {
		Annotation rootAnnotation = source.getAnnotation(rootAnnotationType);
		AnnotationTypeMapping mapping = getMapping(rootAnnotation, annotationType);
		return TypeMappedAnnotation.createIfPossible(mapping, source, rootAnnotation, 0, IntrospectionFailureLogger.INFO);
	}

	private AnnotationTypeMapping getMapping(Annotation annotation,
			Class<? extends Annotation> mappedAnnotationType) {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(annotation.annotationType());
		for (int i = 0; i < mappings.size(); i++) {
			AnnotationTypeMapping candidate = mappings.get(i);
			if (candidate.getAnnotationType().equals(mappedAnnotationType)) {
				return candidate;
			}
		}
		throw new IllegalStateException(
				"No mapping from " + annotation + " to " + mappedAnnotationType);
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface ExplicitMirror {

		@AliasFor("b")
		String a() default "";

		@AliasFor("a")
		String b() default "";
	}

	@ExplicitMirror(a = "test")
	static class WithExplicitMirrorA {
	}

	@ExplicitMirror(b = "test")
	static class WithExplicitMirrorB {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ExplicitAliasMetaAnnotationTarget(nonAliased = "nonAliased")
	@interface ExplicitAliasToMetaAnnotation {

		@AliasFor(annotation = ExplicitAliasMetaAnnotationTarget.class)
		String aliased() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExplicitAliasMetaAnnotationTarget {

		String aliased() default "";

		String nonAliased() default "";
	}

	@ExplicitAliasToMetaAnnotation(aliased = "aliased")
	private static class WithExplicitAliasToMetaAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ConventionAliasMetaAnnotationTarget {

		String value() default "";

		String convention() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ConventionAliasMetaAnnotationTarget
	@interface ConventionAliasToMetaAnnotation {

		String value() default "";

		// Do NOT use @AliasFor here
		String convention() default "";
	}

	@ConventionAliasToMetaAnnotation(value = "value", convention = "convention")
	private static class WithConventionAliasToMetaAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ArrayTypes {

		String[] stringValue();

		byte[] byteValue();

		short[] shortValue();

		int[] intValue();

		long[] longValue();

		boolean[] booleanValue();

		char[] charValue();

		double[] doubleValue();

		float[] floatValue();

		Class<?>[] classValue();

		ExplicitMirror[] annotationValue();

		ExampleEnum[] enumValue();
	}

	enum ExampleEnum {
		ONE, TWO, THREE
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedContainer {

		NestedAnnotation value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedAnnotation {

		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassAttributes {

		Class<?> classValue();

		Class<?>[] classArrayValue();
	}

}
