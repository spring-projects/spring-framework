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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import kotlin.Metadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Base class for {@link AnnotationMetadata} tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Brian Clozel
 */
public abstract class AbstractAnnotationMetadataTests {

	protected abstract AnnotationMetadata get(Class<?> source);

	@Nested
	class TypeTests {

		@Test
		void classEquals() {
			AnnotationMetadata testClass1 = get(TestClass.class);
			AnnotationMetadata testClass2 = get(TestClass.class);

			assertThat(testClass1).isEqualTo(testClass1);
			assertThat(testClass2).isEqualTo(testClass2);
			assertThat(testClass1).isEqualTo(testClass2);
			assertThat(testClass2).isEqualTo(testClass1);
		}

		@Test
		void memberClassEquals() {
			AnnotationMetadata testMemberClass1 = get(TestMemberClass.class);
			AnnotationMetadata testMemberClass2 = get(TestMemberClass.class);

			assertThat(testMemberClass1).isEqualTo(testMemberClass1);
			assertThat(testMemberClass2).isEqualTo(testMemberClass2);
			assertThat(testMemberClass1).isEqualTo(testMemberClass2);
			assertThat(testMemberClass2).isEqualTo(testMemberClass1);
		}

		@Test
		void classHashCode() {
			AnnotationMetadata testClass1 = get(TestClass.class);
			AnnotationMetadata testClass2 = get(TestClass.class);

			assertThat(testClass1).hasSameHashCodeAs(testClass2);
		}

		@Test
		void memberClassHashCode() {
			AnnotationMetadata testMemberClass1 = get(TestMemberClass.class);
			AnnotationMetadata testMemberClass2 = get(TestMemberClass.class);

			assertThat(testMemberClass1).hasSameHashCodeAs(testMemberClass2);
		}

		@Test
		void classToString() {
			assertThat(get(TestClass.class).toString()).isEqualTo(TestClass.class.getName());
		}

		@Test
		void getClassNameReturnsClassName() {
			assertThat(get(TestClass.class).getClassName()).isEqualTo(TestClass.class.getName());
		}

		@Test
		void isInterfaceWhenInterfaceReturnsTrue() {
			assertThat(get(TestInterface.class).isInterface()).isTrue();
			assertThat(get(TestAnnotation.class).isInterface()).isTrue();
		}

		@Test
		void isInterfaceWhenNotInterfaceReturnsFalse() {
			assertThat(get(TestClass.class).isInterface()).isFalse();
		}

		@Test
		void isAnnotationWhenAnnotationReturnsTrue() {
			assertThat(get(TestAnnotation.class).isAnnotation()).isTrue();
		}

		@Test
		void isAnnotationWhenNotAnnotationReturnsFalse() {
			assertThat(get(TestClass.class).isAnnotation()).isFalse();
			assertThat(get(TestInterface.class).isAnnotation()).isFalse();
		}

		@Test
		void isFinalWhenFinalReturnsTrue() {
			assertThat(get(TestFinalClass.class).isFinal()).isTrue();
		}

		@Test
		void isFinalWhenNonFinalReturnsFalse() {
			assertThat(get(TestClass.class).isFinal()).isFalse();
		}

		@Test
		void isIndependentWhenIndependentReturnsTrue() {
			assertThat(get(AbstractAnnotationMetadataTests.class).isIndependent()).isTrue();
			assertThat(get(TestClass.class).isIndependent()).isTrue();
		}

		@Test
		void isIndependentWhenNotIndependentReturnsFalse() {
			assertThat(get(TestNonStaticInnerClass.class).isIndependent()).isFalse();
		}

		@Test
		void getEnclosingClassNameWhenHasEnclosingClassReturnsEnclosingClass() {
			assertThat(get(TestClass.class).getEnclosingClassName()).isEqualTo(
					AbstractAnnotationMetadataTests.TypeTests.class.getName());
		}

		@Test
		void getEnclosingClassNameWhenHasNoEnclosingClassReturnsNull() {
			assertThat(get(AbstractAnnotationMetadataTests.class).getEnclosingClassName()).isNull();
		}

		@Test
		void getSuperClassNameWhenHasSuperClassReturnsName() {
			assertThat(get(TestSubclass.class).getSuperClassName()).isEqualTo(TestClass.class.getName());
			assertThat(get(TestClass.class).getSuperClassName()).isEqualTo(Object.class.getName());
		}

		@Test
		void getSuperClassNameWhenHasNoSuperClassReturnsNull() {
			assertThat(get(Object.class).getSuperClassName()).isNull();
			assertThat(get(TestInterface.class).getSuperClassName()).isIn(null, "java.lang.Object");
			assertThat(get(TestSubInterface.class).getSuperClassName()).isIn(null, "java.lang.Object");
		}

		@Test
		void getSuperClassNameWhenPackageInfoReturnsNull() throws Exception {
			Class<?> packageClass = Class.forName(getClass().getPackageName() + ".package-info");
			assertThat(get(packageClass).getSuperClassName()).isNull();
		}

		@Test
		void getInterfaceNamesWhenHasInterfacesReturnsNames() {
			assertThat(get(TestSubclass.class).getInterfaceNames()).containsExactly(TestInterface.class.getName());
			assertThat(get(TestSubInterface.class).getInterfaceNames()).containsExactly(TestInterface.class.getName());
		}

		@Test
		void getInterfaceNamesWhenHasNoInterfacesReturnsEmptyArray() {
			assertThat(get(TestClass.class).getInterfaceNames()).isEmpty();
		}

		@Test
		void getMemberClassNamesWhenHasMemberClassesReturnsNames() {
			assertThat(get(TestMemberClass.class).getMemberClassNames()).containsExactlyInAnyOrder(
					TestMemberClass.TestMemberClassInnerClass.class.getName(), TestMemberClass.TestMemberClassInnerInterface.class.getName());
		}

		@Test
		void getMemberClassNamesWhenHasNestedMemberClassesReturnsOnlyFirstLevel() {
			assertThat(get(TestNestedMemberClass.class).getMemberClassNames()).containsOnly(
					TestNestedMemberClass.TestMemberClassInnerClassA.class.getName(),
					TestNestedMemberClass.TestMemberClassInnerClassB.class.getName());
		}

		@Test
		void getMemberClassNamesWhenHasNoMemberClassesReturnsEmptyArray() {
			assertThat(get(TestClass.class).getMemberClassNames()).isEmpty();
		}

		public static class TestClass {
		}

		public interface TestInterface {
		}

		public interface TestSubInterface extends TestInterface {
		}

		public @interface TestAnnotation {
		}

		public static final class TestFinalClass {
		}

		public class TestNonStaticInnerClass {
		}

		public static class TestSubclass extends TestClass implements TestInterface {
		}

		public static class TestMemberClass {

			public static class TestMemberClassInnerClass {
			}

			interface TestMemberClassInnerInterface {
			}

		}

		public static class TestNestedMemberClass {

			public static class TestMemberClassInnerClassA {

				public static class TestMemberClassInnerClassAA {

				}

			}

			public static class TestMemberClassInnerClassB {

			}

		}

	}

	@Nested
	class AnnotationTests {

		@Test
		void getAnnotationsReturnsDirectAnnotations() {
			assertThat(get(WithDirectAnnotations.class).getAnnotations().stream())
					.filteredOn(MergedAnnotation::isDirectlyPresent)
					.extracting(a -> a.getType().getName())
					.containsExactlyInAnyOrder(DirectAnnotation1.class.getName(), DirectAnnotation2.class.getName());
		}

		@Test
		void isAnnotatedWhenMatchesDirectAnnotationReturnsTrue() {
			assertThat(get(WithDirectAnnotations.class).isAnnotated(DirectAnnotation1.class.getName())).isTrue();
		}

		@Test
		void isAnnotatedWhenMatchesMetaAnnotationReturnsTrue() {
			assertThat(get(WithMetaAnnotations.class).isAnnotated(MetaAnnotation2.class.getName())).isTrue();
		}

		@Test
		void isAnnotatedWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
			assertThat(get(NoAnnotationClass.class).isAnnotated(DirectAnnotation1.class.getName())).isFalse();
		}

		@Test
		void getAnnotationAttributesReturnsAttributes() {
			assertThat(get(WithAnnotationAttributes.class).getAnnotationAttributes(AnnotationAttributes.class.getName()))
					.containsOnly(entry("name", "test"), entry("size", 1));
		}

		@Test
		void getAllAnnotationAttributesReturnsAllAttributes() {
			MultiValueMap<String, Object> attributes =
					get(WithMetaAnnotationAttributes.class).getAllAnnotationAttributes(AnnotationAttributes.class.getName());
			assertThat(attributes).containsOnlyKeys("name", "size");
			assertThat(attributes.get("name")).containsExactlyInAnyOrder("m1", "m2");
			assertThat(attributes.get("size")).containsExactlyInAnyOrder(1, 2);
		}

		@Test
		void getComplexAttributeTypesReturnsAll() {
			MultiValueMap<String, Object> attributes =
					get(WithComplexAttributeTypes.class).getAllAnnotationAttributes(ComplexAttributes.class.getName());
			assertThat(attributes).containsOnlyKeys("names", "count", "types", "subAnnotation");
			assertThat(attributes.get("names")).hasSize(1);
			assertThat(attributes.get("names").get(0)).isEqualTo(new String[]{"first", "second"});
			assertThat(attributes.get("count").get(0)).isEqualTo(new TestEnum[]{TestEnum.ONE, TestEnum.TWO});
			assertThat(attributes.get("types").get(0)).isEqualTo(new Class[]{TestEnum.class});
			assertThat(attributes.get("subAnnotation")).hasSize(1);
		}

		@Test
		void getComplexAttributeTypesReturnsAllWithKotlinMetadata() {
			MultiValueMap<String, Object> attributes =
					get(WithComplexAttributeTypes.class).getAllAnnotationAttributes(Metadata.class.getName());
			assertThat(attributes).containsKeys("k", "mv");
			int[] values = {42};
			assertThat(attributes.get("mv")).hasSize(1);
			assertThat(attributes.get("mv").get(0)).isEqualTo(values);
		}

		@Test
		void getAnnotationAttributeIntType() {
			MultiValueMap<String, Object> attributes =
					get(WithIntType.class).getAllAnnotationAttributes(ComplexAttributes.class.getName());
			assertThat(attributes).containsOnlyKeys("names", "count", "types", "subAnnotation");
			assertThat(attributes.get("types").get(0)).isEqualTo(new Class[]{int.class});
		}

		@Test
		void getRepeatableReturnsAttributes() {
			MultiValueMap<String, Object> attributes =
					get(WithRepeatableAnnotations.class).getAllAnnotationAttributes(RepeatableAnnotations.class.getName());
			assertThat(attributes).containsKeys("value");
			assertThat(attributes.get("value")).hasSize(1);
		}

		@Test
		void getAnnotationTypesReturnsDirectAnnotations() {
			AnnotationMetadata metadata = get(WithDirectAnnotations.class);
			assertThat(metadata.getAnnotationTypes()).containsExactlyInAnyOrder(
					DirectAnnotation1.class.getName(), DirectAnnotation2.class.getName());
		}

		@Test
		void getMetaAnnotationTypesReturnsMetaAnnotations() {
			AnnotationMetadata metadata = get(WithMetaAnnotations.class);
			assertThat(metadata.getMetaAnnotationTypes(MetaAnnotationRoot.class.getName()))
					.containsExactlyInAnyOrder(MetaAnnotation1.class.getName(), MetaAnnotation2.class.getName());
		}

		@Test
		void hasAnnotationWhenMatchesDirectAnnotationReturnsTrue() {
			assertThat(get(WithDirectAnnotations.class).hasAnnotation(DirectAnnotation1.class.getName())).isTrue();
		}

		@Test
		void hasAnnotationWhenMatchesMetaAnnotationReturnsFalse() {
			assertThat(get(WithMetaAnnotations.class).hasAnnotation(MetaAnnotation1.class.getName())).isFalse();
			assertThat(get(WithMetaAnnotations.class).hasAnnotation(MetaAnnotation2.class.getName())).isFalse();
		}

		@Test
		void hasAnnotationWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
			assertThat(get(NoAnnotationClass.class).hasAnnotation(DirectAnnotation1.class.getName())).isFalse();
		}

		@Test
		void hasMetaAnnotationWhenMatchesDirectReturnsFalse() {
			assertThat(get(WithDirectAnnotations.class).hasMetaAnnotation(DirectAnnotation1.class.getName())).isFalse();
		}

		@Test
		void hasMetaAnnotationWhenMatchesMetaAnnotationReturnsTrue() {
			assertThat(get(WithMetaAnnotations.class).hasMetaAnnotation(MetaAnnotation1.class.getName())).isTrue();
			assertThat(get(WithMetaAnnotations.class).hasMetaAnnotation(MetaAnnotation2.class.getName())).isTrue();
		}

		@Test
		void hasMetaAnnotationWhenDoesNotMatchDirectOrMetaAnnotationReturnsFalse() {
			assertThat(get(NoAnnotationClass.class).hasMetaAnnotation(MetaAnnotation1.class.getName())).isFalse();
		}

		@Test
		void hasAnnotatedMethodsWhenMatchesDirectAnnotationReturnsTrue() {
			assertThat(get(WithAnnotatedMethod.class).hasAnnotatedMethods(DirectAnnotation1.class.getName())).isTrue();
		}

		@Test
		void hasAnnotatedMethodsWhenMatchesMetaAnnotationReturnsTrue() {
			assertThat(get(WithMetaAnnotatedMethod.class).hasAnnotatedMethods(MetaAnnotation2.class.getName())).isTrue();
		}

		@Test
		void hasAnnotatedMethodsWhenDoesNotMatchAnyAnnotationReturnsFalse() {
			assertThat(get(WithAnnotatedMethod.class).hasAnnotatedMethods(MetaAnnotation2.class.getName())).isFalse();
			assertThat(get(WithNonAnnotatedMethod.class).hasAnnotatedMethods(DirectAnnotation1.class.getName())).isFalse();
		}

		@Test
		void getAnnotatedMethodsReturnsMatchingAnnotatedAndMetaAnnotatedMethods() {
			assertThat(get(WithDirectAndMetaAnnotatedMethods.class).getAnnotatedMethods(MetaAnnotation2.class.getName()))
					.extracting(MethodMetadata::getMethodName)
					.containsExactlyInAnyOrder("direct", "meta");
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
		public @interface MetaAnnotationAttributes1 {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@AnnotationAttributes(name = "m2", size = 2)
		public @interface MetaAnnotationAttributes2 {
		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface AnnotationAttributes {

			String name();

			int size();

		}


		@ComplexAttributes(names = {"first", "second"}, count = {TestEnum.ONE, TestEnum.TWO},
				types = {TestEnum.class}, subAnnotation = @SubAnnotation(name="spring"))
		@Metadata(mv = {42})
		public static class WithComplexAttributeTypes {
		}

		@ComplexAttributes(names = "void", count = TestEnum.ONE, types = int.class,
				subAnnotation = @SubAnnotation(name="spring"))
		public static class WithIntType {

		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface ComplexAttributes {

			String[] names();

			TestEnum[] count();

			Class<?>[] types();

			SubAnnotation subAnnotation();
		}

		public @interface SubAnnotation {

			String name();
		}

		public enum TestEnum {
			ONE {

			},
			TWO {

			},
			THREE {

			}
		}

		@RepeatableAnnotation(name = "first")
		@RepeatableAnnotation(name = "second")
		public static class WithRepeatableAnnotations {

		}

		@Retention(RetentionPolicy.RUNTIME)
		@Repeatable(RepeatableAnnotations.class)
		public @interface RepeatableAnnotation {

			String name();

		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface RepeatableAnnotations {

			RepeatableAnnotation[] value();

		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface DirectAnnotation1 {
		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface DirectAnnotation2 {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@MetaAnnotation1
		public @interface MetaAnnotationRoot {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@MetaAnnotation2
		public @interface MetaAnnotation1 {
		}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface MetaAnnotation2 {
		}

		@DirectAnnotation1
		@DirectAnnotation2
		public static class WithDirectAnnotations {
		}

		@MetaAnnotationRoot
		public static class WithMetaAnnotations {
		}

		static class NoAnnotationClass {

		}

	}

	@Nested
	class MethodTests {

		@Test
		void declaredMethodsToString() {
			List<String> methods = get(TestMethods.class).getDeclaredMethods().stream().map(Object::toString).toList();
			List<String> expected = Arrays.stream(TestMethods.class.getDeclaredMethods()).map(Object::toString).toList();
			assertThat(methods).containsExactlyInAnyOrderElementsOf(expected);
		}

		static class TestMethods {
			public String test1(String argument) {
				return "test";
			}

			public String test2(String argument) {
				return "test";
			}

			public String test3(String argument) {
				return "test";
			}
		}

	}

}
