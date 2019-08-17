/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AbstractAnnotationMetadataTests.TestMemberClass.TestMemberClassInnerClass;
import org.springframework.core.type.AbstractAnnotationMetadataTests.TestMemberClass.TestMemberClassInnerInterface;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Base class for {@link AnnotationMetadata} tests.
 *
 * @author Phillip Webb
 */
public abstract class AbstractAnnotationMetadataTests {

	@Test
	public void getClassNameReturnsClassName() {
		assertThat(get(TestClass.class).getClassName()).isEqualTo(
				TestClass.class.getName());
	}

	@Test
	public void isInterfaceWhenInterfaceReturnsTrue() {
		assertThat(get(TestInterface.class).isInterface()).isTrue();
		assertThat(get(TestAnnotation.class).isInterface()).isTrue();
	}

	@Test
	public void isInterfaceWhenNotInterfaceReturnsFalse() {
		assertThat(get(TestClass.class).isInterface()).isFalse();
	}

	@Test
	public void isAnnotationWhenAnnotationReturnsTrue() {
		assertThat(get(TestAnnotation.class).isAnnotation()).isTrue();
	}

	@Test
	public void isAnnotationWhenNotAnnotationReturnsFalse() {
		assertThat(get(TestClass.class).isAnnotation()).isFalse();
		assertThat(get(TestInterface.class).isAnnotation()).isFalse();
	}

	@Test
	public void isFinalWhenFinalReturnsTrue() {
		assertThat(get(TestFinalClass.class).isFinal()).isTrue();
	}

	@Test
	public void isFinalWhenNonFinalReturnsFalse() {
		assertThat(get(TestClass.class).isFinal()).isFalse();
	}

	@Test
	public void isIndependentWhenIndependentReturnsTrue() {
		assertThat(get(AbstractAnnotationMetadataTests.class).isIndependent()).isTrue();
		assertThat(get(TestClass.class).isIndependent()).isTrue();
	}

	@Test
	public void isIndependentWhenNotIndependentReturnsFalse() {
		assertThat(get(TestNonStaticInnerClass.class).isIndependent()).isFalse();
	}

	@Test
	public void getEnclosingClassNameWhenHasEnclosingClassReturnsEnclosingClass() {
		assertThat(get(TestClass.class).getEnclosingClassName()).isEqualTo(
				AbstractAnnotationMetadataTests.class.getName());
	}

	@Test
	public void getEnclosingClassNameWhenHasNoEnclosingClassReturnsNull() {
		assertThat(get(
				AbstractAnnotationMetadataTests.class).getEnclosingClassName()).isNull();
	}

	@Test
	public void getSuperClassNameWhenHasSuperClassReturnsName() {
		assertThat(get(TestSubclass.class).getSuperClassName()).isEqualTo(
				TestClass.class.getName());
		assertThat(get(TestClass.class).getSuperClassName()).isEqualTo(
				Object.class.getName());
	}

	@Test
	public void getSuperClassNameWhenHasNoSuperClassReturnsNull() {
		assertThat(get(Object.class).getSuperClassName()).isNull();
		assertThat(get(TestInterface.class).getSuperClassName()).isNull();
		assertThat(get(TestSubInterface.class).getSuperClassName()).isNull();
	}

	@Test
	public void getInterfaceNamesWhenHasInterfacesReturnsNames() {
		assertThat(get(TestSubclass.class).getInterfaceNames()).containsExactlyInAnyOrder(
				TestInterface.class.getName());
		assertThat(get(
				TestSubInterface.class).getInterfaceNames()).containsExactlyInAnyOrder(
						TestInterface.class.getName());
	}

	@Test
	public void getInterfaceNamesWhenHasNoInterfacesReturnsEmptyArray() {
		assertThat(get(TestClass.class).getInterfaceNames()).isEmpty();
	}

	@Test
	public void getMemberClassNamesWhenHasMemberClassesReturnsNames() {
		assertThat(get(
				TestMemberClass.class).getMemberClassNames()).containsExactlyInAnyOrder(
						TestMemberClassInnerClass.class.getName(),
						TestMemberClassInnerInterface.class.getName());
	}

	@Test
	public void getMemberClassNamesWhenHasNoMemberClassesReturnsEmptyArray() {
		assertThat(get(TestClass.class).getMemberClassNames()).isEmpty();
	}

	@Test
	public void getAnnotationsReturnsDirectAnnotations() {
		AnnotationMetadata metadata = get(WithDirectAnnotations.class);
		assertThat(metadata.getAnnotations().stream().filter(
				MergedAnnotation::isDirectlyPresent).map(
						a -> a.getType().getName())).containsExactlyInAnyOrder(
								DirectAnnotation1.class.getName(),
								DirectAnnotation2.class.getName());
	}

	@Test
	public void isAnnotatedWhenMatchesDirectAnnotationReturnsTrue() {
		assertThat(get(WithDirectAnnotations.class).isAnnotated(
				DirectAnnotation1.class.getName())).isTrue();
	}

	@Test
	public void isAnnotatedWhenMatchesMetaAnnotationReturnsTrue() {
		assertThat(get(WithMetaAnnotations.class).isAnnotated(
				MetaAnnotation2.class.getName())).isTrue();
	}

	@Test
	public void isAnnotatedWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
		assertThat(get(TestClass.class).isAnnotated(
				DirectAnnotation1.class.getName())).isFalse();
	}

	@Test
	public void getAnnotationAttributesReturnsAttributes() {
		assertThat(get(WithAnnotationAttributes.class).getAnnotationAttributes(
				AnnotationAttributes.class.getName())).containsOnly(entry("name", "test"),
						entry("size", 1));
	}

	@Test
	public void getAllAnnotationAttributesReturnsAllAttributes() {
		MultiValueMap<String, Object> attributes = get(
				WithMetaAnnotationAttributes.class).getAllAnnotationAttributes(
						AnnotationAttributes.class.getName());
		assertThat(attributes).containsOnlyKeys("name", "size");
		assertThat(attributes.get("name")).containsExactlyInAnyOrder("m1", "m2");
		assertThat(attributes.get("size")).containsExactlyInAnyOrder(1, 2);
	}

	@Test
	public void getAnnotationTypesReturnsDirectAnnotations() {
		AnnotationMetadata metadata = get(WithDirectAnnotations.class);
		assertThat(metadata.getAnnotationTypes()).containsExactlyInAnyOrder(
				DirectAnnotation1.class.getName(), DirectAnnotation2.class.getName());
	}

	@Test
	public void getMetaAnnotationTypesReturnsMetaAnnotations() {
		AnnotationMetadata metadata = get(WithMetaAnnotations.class);
		assertThat(metadata.getMetaAnnotationTypes(
				MetaAnnotationRoot.class.getName())).containsExactlyInAnyOrder(
						MetaAnnotation1.class.getName(), MetaAnnotation2.class.getName());
	}

	@Test
	public void hasAnnotationWhenMatchesDirectAnnotationReturnsTrue() {
		assertThat(get(WithDirectAnnotations.class).hasAnnotation(
				DirectAnnotation1.class.getName())).isTrue();
	}

	@Test
	public void hasAnnotationWhenMatchesMetaAnnotationReturnsFalse() {
		assertThat(get(WithMetaAnnotations.class).hasAnnotation(
				MetaAnnotation1.class.getName())).isFalse();
		assertThat(get(WithMetaAnnotations.class).hasAnnotation(
				MetaAnnotation2.class.getName())).isFalse();
	}

	@Test
	public void hasAnnotationWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
		assertThat(get(TestClass.class).hasAnnotation(
				DirectAnnotation1.class.getName())).isFalse();
	}

	@Test
	public void hasMetaAnnotationWhenMatchesDirectReturnsFalse() {
		assertThat(get(WithDirectAnnotations.class).hasMetaAnnotation(
				DirectAnnotation1.class.getName())).isFalse();
	}

	@Test
	public void hasMetaAnnotationWhenMatchesMetaAnnotationReturnsTrue() {
		assertThat(get(WithMetaAnnotations.class).hasMetaAnnotation(
				MetaAnnotation1.class.getName())).isTrue();
		assertThat(get(WithMetaAnnotations.class).hasMetaAnnotation(
				MetaAnnotation2.class.getName())).isTrue();
	}

	@Test
	public void hasMetaAnnotationWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
		assertThat(get(TestClass.class).hasMetaAnnotation(
				MetaAnnotation1.class.getName())).isFalse();
	}

	@Test
	public void hasAnnotatedMethodsWhenMatchesDirectAnnotationReturnsTrue() {
		assertThat(get(WithAnnotatedMethod.class).hasAnnotatedMethods(
				DirectAnnotation1.class.getName())).isTrue();
	}

	@Test
	public void hasAnnotatedMethodsWhenMatchesMetaAnnotationReturnsTrue() {
		assertThat(get(WithMetaAnnotatedMethod.class).hasAnnotatedMethods(
				MetaAnnotation2.class.getName())).isTrue();
	}

	@Test
	public void hasAnnotatedMethodsWhenDoesNotMatchAnyAnnotationReturnsFalse() {
		assertThat(get(WithAnnotatedMethod.class).hasAnnotatedMethods(
				MetaAnnotation2.class.getName())).isFalse();
		assertThat(get(WithNonAnnotatedMethod.class).hasAnnotatedMethods(
				DirectAnnotation1.class.getName())).isFalse();
	}

	@Test
	public void getAnnotatedMethodsReturnsMatchingAnnotatedAndMetaAnnotatedMethods() {
		assertThat(get(WithDirectAndMetaAnnotatedMethods.class).getAnnotatedMethods(
				MetaAnnotation2.class.getName()).stream().map(
						MethodMetadata::getMethodName)).containsExactlyInAnyOrder(
								"direct", "meta");
	}

	protected abstract AnnotationMetadata get(Class<?> source);

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DirectAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DirectAnnotation2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAnnotation1
	public static @interface MetaAnnotationRoot {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAnnotation2
	public static @interface MetaAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MetaAnnotation2 {

	}

	public static class TestClass {

	}

	public static interface TestInterface {

	}

	public static interface TestSubInterface extends TestInterface {

	}

	public @interface TestAnnotation {

	}

	public static final class TestFinalClass {

	}

	public class TestNonStaticInnerClass {

	}

	public static class TestSubclass extends TestClass implements TestInterface {

	}

	@DirectAnnotation1
	@DirectAnnotation2
	public static class WithDirectAnnotations {

	}

	@MetaAnnotationRoot
	public static class WithMetaAnnotations {

	}

	public static class TestMemberClass {

		public static class TestMemberClassInnerClass {

		}

		interface TestMemberClassInnerInterface {

		}

	}

	public static class WithAnnotatedMethod {

		@DirectAnnotation1
		public void test() {
		}

	}

	public static class WithMetaAnnotatedMethod {

		@MetaAnnotationRoot
		public void test() {
		}

	}

	public static class WithNonAnnotatedMethod {

		public void test() {
		}

	}

	public static class WithDirectAndMetaAnnotatedMethods {

		@MetaAnnotation2
		public void direct() {
		}

		@MetaAnnotationRoot
		public void meta() {
		}

	}

	@AnnotationAttributes(name = "test", size = 1)
	public static class WithAnnotationAttributes {

	}

	@MetaAnnotationAttributes1
	@MetaAnnotationAttributes2
	public static class WithMetaAnnotationAttributes {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AnnotationAttributes(name = "m1", size = 1)
	public static @interface MetaAnnotationAttributes1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AnnotationAttributes(name = "m2", size = 2)
	public static @interface MetaAnnotationAttributes2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface AnnotationAttributes {

		String name();

		int size();

	}

}
