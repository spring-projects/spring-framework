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

package org.springframework.core.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import example.type.AnnotatedComponent;
import example.type.EnclosingAnnotation;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Base class for {@link MethodMetadata} tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public abstract class AbstractMethodMetadataTests {

	@Test
	void verifyEquals() {
		MethodMetadata withMethod1 = getTagged(WithMethod.class);
		MethodMetadata withMethod2 = getTagged(WithMethod.class);
		MethodMetadata withMethodWithTwoArguments1 = getTagged(WithMethodWithTwoArguments.class);
		MethodMetadata withMethodWithTwoArguments2 = getTagged(WithMethodWithTwoArguments.class);

		assertThat(withMethod1).isNotEqualTo(null);

		assertThat(withMethod1).isEqualTo(withMethod1);
		assertThat(withMethod2).isEqualTo(withMethod2);
		assertThat(withMethod1).isEqualTo(withMethod2);
		assertThat(withMethod2).isEqualTo(withMethod1);

		assertThat(withMethodWithTwoArguments1).isEqualTo(withMethodWithTwoArguments1);
		assertThat(withMethodWithTwoArguments2).isEqualTo(withMethodWithTwoArguments2);
		assertThat(withMethodWithTwoArguments1).isEqualTo(withMethodWithTwoArguments2);
		assertThat(withMethodWithTwoArguments2).isEqualTo(withMethodWithTwoArguments1);

		assertThat(withMethod1).isNotEqualTo(withMethodWithTwoArguments1);
		assertThat(withMethodWithTwoArguments1).isNotEqualTo(withMethod1);
	}

	@Test
	void verifyHashCode() {
		MethodMetadata withMethod1 = getTagged(WithMethod.class);
		MethodMetadata withMethod2 = getTagged(WithMethod.class);
		MethodMetadata withMethodWithTwoArguments1 = getTagged(WithMethodWithTwoArguments.class);
		MethodMetadata withMethodWithTwoArguments2 = getTagged(WithMethodWithTwoArguments.class);

		assertThat(withMethod1).hasSameHashCodeAs(withMethod2);
		assertThat(withMethodWithTwoArguments1).hasSameHashCodeAs(withMethodWithTwoArguments2);

		assertThat(withMethod1).doesNotHaveSameHashCodeAs(withMethodWithTwoArguments1);
	}

	@Test
	void verifyToString() {
		assertThat(getTagged(WithMethod.class).toString())
			.endsWith(WithMethod.class.getName() + ".test()");

		assertThat(getTagged(WithMethodWithOneArgument.class).toString())
			.endsWith(WithMethodWithOneArgument.class.getName() + ".test(java.lang.String)");

		assertThat(getTagged(WithMethodWithTwoArguments.class).toString())
			.endsWith(WithMethodWithTwoArguments.class.getName() + ".test(java.lang.String,java.lang.Integer)");
	}

	@Test
	void getMethodNameReturnsMethodName() {
		assertThat(getTagged(WithMethod.class).getMethodName()).isEqualTo("test");
	}

	@Test
	void getDeclaringClassReturnsDeclaringClass() {
		assertThat(getTagged(WithMethod.class).getDeclaringClassName()).isEqualTo(
				WithMethod.class.getName());
	}

	@Test
	void getReturnTypeReturnsReturnType() {
		assertThat(getTagged(WithMethod.class).getReturnTypeName()).isEqualTo(
				String.class.getName());
	}

	@Test
	void isAbstractWhenAbstractReturnsTrue() {
		assertThat(getTagged(WithAbstractMethod.class).isAbstract()).isTrue();
	}

	@Test
	void isAbstractWhenNotAbstractReturnsFalse() {
		assertThat(getTagged(WithMethod.class).isAbstract()).isFalse();
	}

	@Test
	void isStatusWhenStaticReturnsTrue() {
		assertThat(getTagged(WithStaticMethod.class).isStatic()).isTrue();
	}

	@Test
	void isStaticWhenNotStaticReturnsFalse() {
		assertThat(getTagged(WithMethod.class).isStatic()).isFalse();
	}

	@Test
	void isFinalWhenFinalReturnsTrue() {
		assertThat(getTagged(WithFinalMethod.class).isFinal()).isTrue();
	}

	@Test
	void isFinalWhenNonFinalReturnsFalse() {
		assertThat(getTagged(WithMethod.class).isFinal()).isFalse();
	}

	@Test
	void isOverridableWhenOverridableReturnsTrue() {
		assertThat(getTagged(WithMethod.class).isOverridable()).isTrue();
	}

	@Test
	void isOverridableWhenNonOverridableReturnsFalse() {
		assertThat(getTagged(WithStaticMethod.class).isOverridable()).isFalse();
		assertThat(getTagged(WithFinalMethod.class).isOverridable()).isFalse();
		assertThat(getTagged(WithPrivateMethod.class).isOverridable()).isFalse();
	}

	@Test
	void getAnnotationsReturnsDirectAnnotations() {
		MethodMetadata metadata = getTagged(WithDirectAnnotation.class);
		assertThat(metadata.getAnnotations().stream().filter(
				MergedAnnotation::isDirectlyPresent).map(
						a -> a.getType().getName())).containsExactlyInAnyOrder(
								Tag.class.getName(),
								DirectAnnotation.class.getName());
	}

	@Test
	void isAnnotatedWhenMatchesDirectAnnotationReturnsTrue() {
		assertThat(getTagged(WithDirectAnnotation.class).isAnnotated(
				DirectAnnotation.class.getName())).isTrue();
	}

	@Test
	void isAnnotatedWhenMatchesMetaAnnotationReturnsTrue() {
		assertThat(getTagged(WithMetaAnnotation.class).isAnnotated(
				DirectAnnotation.class.getName())).isTrue();
	}

	@Test
	void isAnnotatedWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
		assertThat(getTagged(WithMethod.class).isAnnotated(
				DirectAnnotation.class.getName())).isFalse();
	}

	@Test
	void getAnnotationAttributesReturnsAttributes() {
		assertThat(getTagged(WithAnnotationAttributes.class).getAnnotationAttributes(
				AnnotationAttributes.class.getName())).containsOnly(entry("name", "test"),
						entry("size", 1));
	}

	@Test
	void getAllAnnotationAttributesReturnsAllAttributes() {
		MultiValueMap<String, Object> attributes = getTagged(WithMetaAnnotationAttributes.class)
				.getAllAnnotationAttributes(AnnotationAttributes.class.getName());
		assertThat(attributes).containsOnlyKeys("name", "size");
		assertThat(attributes.get("name")).containsExactlyInAnyOrder("m1", "m2");
		assertThat(attributes.get("size")).containsExactlyInAnyOrder(1, 2);
	}

	@Test // gh-24375
	public void metadataLoadsForNestedAnnotations() {
		AnnotationMetadata annotationMetadata = get(AnnotatedComponent.class);
		assertThat(annotationMetadata.getAnnotationTypes()).containsExactly(EnclosingAnnotation.class.getName());
	}

	protected MethodMetadata getTagged(Class<?> source) {
		return get(source, Tag.class.getName());
	}

	protected MethodMetadata get(Class<?> source, String annotationName) {
		return get(source).getAnnotatedMethods(annotationName).iterator().next();
	}

	protected abstract AnnotationMetadata get(Class<?> source);

	@Retention(RetentionPolicy.RUNTIME)
	@interface Tag {
	}

	public static class WithMethod {

		@Tag
		public String test() {
			return "";
		}

	}

	public static class WithMethodWithOneArgument {

		@Tag
		public String test(String text) {
			return "";
		}

	}

	public static class WithMethodWithTwoArguments {

		@Tag
		public String test(String text, Integer num) {
			return "";
		}

	}

	public abstract static class WithAbstractMethod {

		@Tag
		public abstract String test();

	}

	public static class WithStaticMethod {

		@Tag
		public static String test() {
			return "";
		}

	}

	public static class WithFinalMethod {

		@Tag
		public final String test() {
			return "";
		}

	}

	public static class WithPrivateMethod {

		@Tag
		private String test() {
			return "";
		}

	}

	public abstract static class WithDirectAnnotation {

		@Tag
		@DirectAnnotation
		public abstract String test();

	}

	public abstract static class WithMetaAnnotation {

		@Tag
		@MetaAnnotation
		public abstract String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface DirectAnnotation {
	}

	@DirectAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaAnnotation {
	}

	public abstract static class WithAnnotationAttributes {

		@Tag
		@AnnotationAttributes(name = "test", size = 1)
		public abstract String test();

	}

	public abstract static class WithMetaAnnotationAttributes {

		@Tag
		@MetaAnnotationAttributes1
		@MetaAnnotationAttributes2
		public abstract String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AnnotationAttributes(name = "m1", size = 1)
	@interface MetaAnnotationAttributes1 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@AnnotationAttributes(name = "m2", size = 2)
	@interface MetaAnnotationAttributes2 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationAttributes {

		String name();

		int size();

	}

}
