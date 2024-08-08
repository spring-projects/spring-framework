/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.testfixture.stereotype.Component;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests demonstrating that the reflection-based {@link StandardAnnotationMetadata}
 * and ASM-based {@code SimpleAnnotationMetadata} produce <em>almost</em> identical output.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @see InheritedAnnotationsAnnotationMetadataTests
 */
class AnnotationMetadataTests {

	@Test
	void standardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(AnnotatedComponent.class);
		doTestAnnotationInfo(metadata);
		doTestMethodAnnotationInfo(metadata);
	}

	@Test
	void asmAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponent.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestAnnotationInfo(metadata);
		doTestMethodAnnotationInfo(metadata);
	}

	@Test
	void standardAnnotationMetadataForSubclass() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(AnnotatedComponentSubClass.class);
		doTestSubClassAnnotationInfo(metadata, false);
	}

	@Test
	void asmAnnotationMetadataForSubclass() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponentSubClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestSubClassAnnotationInfo(metadata, true);
	}

	private void doTestSubClassAnnotationInfo(AnnotationMetadata metadata, boolean asm) {
		assertThat(metadata.getClassName()).isEqualTo(AnnotatedComponentSubClass.class.getName());
		assertThat(metadata.isInterface()).isFalse();
		assertThat(metadata.isAnnotation()).isFalse();
		assertThat(metadata.isAbstract()).isFalse();
		assertThat(metadata.isConcrete()).isTrue();
		assertThat(metadata.hasSuperClass()).isTrue();
		assertThat(metadata.getSuperClassName()).isEqualTo(AnnotatedComponent.class.getName());
		assertThat(metadata.getInterfaceNames()).isEmpty();
		assertThat(metadata.isAnnotated(Component.class.getName())).isFalse();
		assertThat(metadata.isAnnotated(Scope.class.getName())).isFalse();
		assertThat(metadata.isAnnotated(SpecialAttr.class.getName())).isFalse();

		if (asm) {
			assertThat(metadata.isAnnotated(NamedComposedAnnotation.class.getName())).isFalse();
			assertThat(metadata.hasAnnotation(NamedComposedAnnotation.class.getName())).isFalse();
			assertThat(metadata.getAnnotationTypes()).isEmpty();
		}
		else {
			assertThat(metadata.isAnnotated(NamedComposedAnnotation.class.getName())).isTrue();
			assertThat(metadata.hasAnnotation(NamedComposedAnnotation.class.getName())).isTrue();
			assertThat(metadata.getAnnotationTypes()).containsExactly(NamedComposedAnnotation.class.getName());
		}

		assertThat(metadata.hasAnnotation(Component.class.getName())).isFalse();
		assertThat(metadata.hasAnnotation(Scope.class.getName())).isFalse();
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName())).isFalse();
		assertThat(metadata.hasMetaAnnotation(Component.class.getName())).isFalse();
		assertThat(metadata.hasMetaAnnotation(MetaAnnotation.class.getName())).isFalse();
		assertThat(metadata.getAnnotationAttributes(Component.class.getName())).isNull();
		assertThat(metadata.getAnnotationAttributes(MetaAnnotation.class.getName(), false)).isNull();
		assertThat(metadata.getAnnotationAttributes(MetaAnnotation.class.getName(), true)).isNull();
		assertThat(metadata.getAnnotatedMethods(DirectAnnotation.class.getName())).isEmpty();
		assertThat(metadata.isAnnotated(IsAnnotatedAnnotation.class.getName())).isFalse();
		assertThat(metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName())).isNull();
	}

	@Test
	void standardAnnotationMetadataForInterface() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(AnnotationMetadata.class);
		doTestMetadataForInterfaceClass(metadata);
	}

	@Test
	void asmAnnotationMetadataForInterface() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotationMetadata.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestMetadataForInterfaceClass(metadata);
	}

	private void doTestMetadataForInterfaceClass(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName()).isEqualTo(AnnotationMetadata.class.getName());
		assertThat(metadata.isInterface()).isTrue();
		assertThat(metadata.isAnnotation()).isFalse();
		assertThat(metadata.isAbstract()).isTrue();
		assertThat(metadata.isConcrete()).isFalse();
		assertThat(metadata.hasSuperClass()).isFalse();
		assertThat(metadata.getSuperClassName()).isNull();
		assertThat(metadata.getInterfaceNames()).hasSize(2);
		assertThat(metadata.getInterfaceNames()[0]).isEqualTo(ClassMetadata.class.getName());
		assertThat(metadata.getInterfaceNames()[1]).isEqualTo(AnnotatedTypeMetadata.class.getName());
		assertThat(metadata.getAnnotationTypes()).isEmpty();
	}

	@Test
	void standardAnnotationMetadataForAnnotation() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(Component.class);
		doTestMetadataForAnnotationClass(metadata);
	}

	@Test
	void asmAnnotationMetadataForAnnotation() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(Component.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestMetadataForAnnotationClass(metadata);
	}

	private void doTestMetadataForAnnotationClass(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName()).isEqualTo(Component.class.getName());
		assertThat(metadata.isInterface()).isTrue();
		assertThat(metadata.isAnnotation()).isTrue();
		assertThat(metadata.isAbstract()).isTrue();
		assertThat(metadata.isConcrete()).isFalse();
		assertThat(metadata.hasSuperClass()).isFalse();
		assertThat(metadata.getSuperClassName()).isNull();
		assertThat(metadata.getInterfaceNames()).hasSize(1);
		assertThat(metadata.getInterfaceNames()[0]).isEqualTo(Annotation.class.getName());
		assertThat(metadata.isAnnotated(Documented.class.getName())).isFalse();
		assertThat(metadata.isAnnotated(Scope.class.getName())).isFalse();
		assertThat(metadata.isAnnotated(SpecialAttr.class.getName())).isFalse();
		assertThat(metadata.hasAnnotation(Documented.class.getName())).isFalse();
		assertThat(metadata.hasAnnotation(Scope.class.getName())).isFalse();
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName())).isFalse();
		assertThat(metadata.getAnnotationTypes()).hasSize(1);
	}

	/**
	 * In order to preserve backward-compatibility, {@link StandardAnnotationMetadata}
	 * defaults to return nested annotations and annotation arrays as actual
	 * Annotation instances. It is recommended for compatibility with ASM-based
	 * AnnotationMetadata implementations to set the 'nestedAnnotationsAsMap' flag to
	 * 'true' as is done in the main test above.
	 */
	@Test
	void standardAnnotationMetadata_nestedAnnotationsAsMap_false() {
		@SuppressWarnings("deprecation")
		AnnotationMetadata metadata = new StandardAnnotationMetadata(AnnotatedComponent.class);
		AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(SpecialAttr.class.getName());
		Annotation[] nestedAnnoArray = (Annotation[]) specialAttrs.get("nestedAnnoArray");
		assertThat(nestedAnnoArray[0]).isInstanceOf(NestedAnno.class);
	}

	@Test
	void metaAnnotationOverridesUsingStandardAnnotationMetadata() {
		@SuppressWarnings("deprecation")
		AnnotationMetadata metadata = new StandardAnnotationMetadata(ComposedConfigurationWithAttributeOverridesClass.class);
		assertMetaAnnotationOverrides(metadata);
	}

	@Test
	void metaAnnotationOverridesUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(ComposedConfigurationWithAttributeOverridesClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMetaAnnotationOverrides(metadata);
	}

	private void assertMetaAnnotationOverrides(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = (AnnotationAttributes) metadata.getAnnotationAttributes(
				TestComponentScan.class.getName(), false);
		assertThat(attributes.getStringArray("value")).containsExactly("org.example.componentscan");
		assertThat(attributes.getStringArray("basePackages")).containsExactly("org.example.componentscan");
		assertThat(attributes.getClassArray("basePackageClasses")).isEmpty();
	}

	@Test  // SPR-11649
	void multipleAnnotationsWithIdenticalAttributeNamesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(NamedAnnotationsClass.class);
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	@Test  // SPR-11649
	void multipleAnnotationsWithIdenticalAttributeNamesUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NamedAnnotationsClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	@Test  // SPR-11649
	void composedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(NamedComposedAnnotationClass.class);
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	@Test  // SPR-11649
	void composedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NamedComposedAnnotationClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	@Test  // gh-31041
	void multipleComposedRepeatableAnnotationsUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(MultipleComposedRepeatableAnnotationsClass.class);
		assertRepeatableAnnotations(metadata);
	}

	@Test  // gh-31041
	void multipleComposedRepeatableAnnotationsUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(MultipleComposedRepeatableAnnotationsClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertRepeatableAnnotations(metadata);
	}

	@Test  // gh-31074
	void multipleComposedRepeatableAnnotationsSortedByReversedMetaDistanceUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(MultipleComposedRepeatableAnnotationsClass.class);
		assertRepeatableAnnotationsSortedByReversedMetaDistance(metadata);
	}

	@Test  // gh-31074
	void multipleComposedRepeatableAnnotationsSortedByReversedMetaDistanceUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(MultipleComposedRepeatableAnnotationsClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertRepeatableAnnotationsSortedByReversedMetaDistance(metadata);
	}

	@Test  // gh-31041
	void multipleRepeatableAnnotationsInContainersUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(MultipleRepeatableAnnotationsInContainersClass.class);
		assertRepeatableAnnotations(metadata);
	}

	@Test  // gh-31041
	void multipleRepeatableAnnotationsInContainersUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(MultipleRepeatableAnnotationsInContainersClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertRepeatableAnnotations(metadata);
	}

	/**
	 * Tests {@code AnnotatedElementUtils#getMergedRepeatableAnnotations()} variants to ensure that
	 * {@link AnnotationMetadata#getMergedRepeatableAnnotationAttributes(Class, Class, boolean)}
	 * behaves the same.
	 */
	@Test  // gh-31041
	void multipleComposedRepeatableAnnotationsUsingAnnotatedElementUtils() {
		Class<?> element = MultipleComposedRepeatableAnnotationsClass.class;

		Set<TestComponentScan> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(element, TestComponentScan.class);
		assertRepeatableAnnotations(annotations);

		annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(element, TestComponentScan.class, TestComponentScans.class);
		assertRepeatableAnnotations(annotations);
	}

	/**
	 * Tests {@code AnnotatedElementUtils#getMergedRepeatableAnnotations()} variants to ensure that
	 * {@link AnnotationMetadata#getMergedRepeatableAnnotationAttributes(Class, Class, boolean)}
	 * behaves the same.
	 */
	@Test  // gh-31041
	void multipleRepeatableAnnotationsInContainersUsingAnnotatedElementUtils() {
		Class<?> element = MultipleRepeatableAnnotationsInContainersClass.class;

		Set<TestComponentScan> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(element, TestComponentScan.class);
		assertRepeatableAnnotations(annotations);

		annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(element, TestComponentScan.class, TestComponentScans.class);
		assertRepeatableAnnotations(annotations);
	}

	private static void assertRepeatableAnnotations(AnnotationMetadata metadata) {
		Set<AnnotationAttributes> attributesSet =
				metadata.getMergedRepeatableAnnotationAttributes(TestComponentScan.class, TestComponentScans.class, true);
		assertThat(attributesSet.stream().map(attributes -> attributes.getStringArray("value")).flatMap(Arrays::stream))
				.containsExactly("A", "B", "C", "D");
		assertThat(attributesSet.stream().map(attributes -> attributes.getStringArray("basePackages")).flatMap(Arrays::stream))
				.containsExactly("A", "B", "C", "D");
		assertThat(attributesSet.stream().map(attributes -> attributes.getStringArray("basePackageClasses")).flatMap(Arrays::stream))
				.containsExactly("java.lang.String", "java.lang.Integer");
	}

	private static void assertRepeatableAnnotationsSortedByReversedMetaDistance(AnnotationMetadata metadata) {
		// Note: although the real @ComponentScan annotation is not looked up using
		// "sortByReversedMetaDistance" semantics, we can still use @TestComponentScan
		// to verify the expected behavior.
		Set<AnnotationAttributes> attributesSet =
				metadata.getMergedRepeatableAnnotationAttributes(TestComponentScan.class, TestComponentScans.class, false, true);
		assertThat(attributesSet.stream().map(attributes -> attributes.getStringArray("value")).flatMap(Arrays::stream))
				.containsExactly("C", "D", "A", "B");
		assertThat(attributesSet.stream().map(attributes -> attributes.getStringArray("basePackages")).flatMap(Arrays::stream))
				.containsExactly("C", "D", "A", "B");
	}

	private static void assertRepeatableAnnotations(Set<TestComponentScan> annotations) {
		assertThat(annotations.stream().map(TestComponentScan::value).flatMap(Arrays::stream))
				.containsExactly("A", "B", "C", "D");
		assertThat(annotations.stream().map(TestComponentScan::basePackages).flatMap(Arrays::stream))
				.containsExactly("A", "B", "C", "D");
	}

	@Test
	void inheritedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(NamedComposedAnnotationExtended.class);
		assertThat(metadata.hasAnnotation(NamedComposedAnnotation.class.getName())).isTrue();
	}

	@Test
	void inheritedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingSimpleAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NamedComposedAnnotationExtended.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertThat(metadata.hasAnnotation(NamedComposedAnnotation.class.getName())).isFalse();
	}


	private void assertMultipleAnnotationsWithIdenticalAttributeNames(AnnotationMetadata metadata) {
		AnnotationAttributes attributes1 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation1.class.getName(), false);
		String name1 = attributes1.getString("name");
		assertThat(name1).as("name of NamedAnnotation1").isEqualTo("name 1");

		AnnotationAttributes attributes2 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation2.class.getName(), false);
		String name2 = attributes2.getString("name");
		assertThat(name2).as("name of NamedAnnotation2").isEqualTo("name 2");

		AnnotationAttributes attributes3 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation3.class.getName(), false);
		String name3 = attributes3.getString("name");
		assertThat(name3).as("name of NamedAnnotation3").isEqualTo("name 3");
	}

	private void doTestAnnotationInfo(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName()).isEqualTo(AnnotatedComponent.class.getName());
		assertThat(metadata.isInterface()).isFalse();
		assertThat(metadata.isAnnotation()).isFalse();
		assertThat(metadata.isAbstract()).isFalse();
		assertThat(metadata.isConcrete()).isTrue();
		assertThat(metadata.hasSuperClass()).isTrue();
		assertThat(metadata.getSuperClassName()).isEqualTo(Object.class.getName());
		assertThat(metadata.getInterfaceNames()).hasSize(1);
		assertThat(metadata.getInterfaceNames()[0]).isEqualTo(Serializable.class.getName());

		assertThat(metadata.isAnnotated(Component.class.getName())).isTrue();

		assertThat(metadata.isAnnotated(NamedComposedAnnotation.class.getName())).isTrue();

		assertThat(metadata.hasAnnotation(Component.class.getName())).isTrue();
		assertThat(metadata.hasAnnotation(Scope.class.getName())).isTrue();
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName())).isTrue();

		assertThat(metadata.hasAnnotation(NamedComposedAnnotation.class.getName())).isTrue();
		assertThat(metadata.getAnnotationTypes()).containsExactlyInAnyOrder(
				Component.class.getName(), Scope.class.getName(),
				SpecialAttr.class.getName(), DirectAnnotation.class.getName(),
				MetaMetaAnnotation.class.getName(), EnumSubclasses.class.getName(),
				NamedComposedAnnotation.class.getName());

		AnnotationAttributes compAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(Component.class.getName());
		assertThat(compAttrs).hasSize(1);
		assertThat(compAttrs.getString("value")).isEqualTo("myName");
		AnnotationAttributes scopeAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(Scope.class.getName());
		assertThat(scopeAttrs).hasSize(1);
		assertThat(scopeAttrs.getString("value")).isEqualTo("myScope");

		Set<MethodMetadata> methods = metadata.getAnnotatedMethods(DirectAnnotation.class.getName());
		MethodMetadata method = methods.iterator().next();
		assertThat(method.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value")).isEqualTo("direct");
		assertThat(method.getAnnotationAttributes(DirectAnnotation.class.getName()).get("myValue")).isEqualTo("direct");
		List<Object> allMeta = method.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
		assertThat(new HashSet<>(allMeta)).isEqualTo(new HashSet<>(Arrays.asList("direct", "meta")));
		allMeta = method.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("additional");
		assertThat(new HashSet<>(allMeta)).isEqualTo(new HashSet<>(List.of("direct")));

		assertThat(metadata.isAnnotated(IsAnnotatedAnnotation.class.getName())).isTrue();

		{ // perform tests with classValuesAsString = false (the default)
			AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(SpecialAttr.class.getName());
			assertThat(specialAttrs).hasSize(6);
			assertThat(String.class.isAssignableFrom(specialAttrs.getClass("clazz"))).isTrue();
			assertThat(specialAttrs.getEnum("state").equals(Thread.State.NEW)).isTrue();

			AnnotationAttributes nestedAnno = specialAttrs.getAnnotation("nestedAnno");
			assertThat(nestedAnno.getString("value")).isEqualTo("na");
			assertThat(nestedAnno.getEnum("anEnum").equals(SomeEnum.LABEL1)).isTrue();
			assertThat((Class<?>[]) nestedAnno.get("classArray")).isEqualTo(new Class<?>[] {String.class});

			AnnotationAttributes[] nestedAnnoArray = specialAttrs.getAnnotationArray("nestedAnnoArray");
			assertThat(nestedAnnoArray).hasSize(2);
			assertThat(nestedAnnoArray[0].getString("value")).isEqualTo("default");
			assertThat(nestedAnnoArray[0].getEnum("anEnum").equals(SomeEnum.DEFAULT)).isTrue();
			assertThat((Class<?>[]) nestedAnnoArray[0].get("classArray")).isEqualTo(new Class<?>[] {Void.class});
			assertThat(nestedAnnoArray[1].getString("value")).isEqualTo("na1");
			assertThat(nestedAnnoArray[1].getEnum("anEnum").equals(SomeEnum.LABEL2)).isTrue();
			assertThat((Class<?>[]) nestedAnnoArray[1].get("classArray")).isEqualTo(new Class<?>[] {Number.class});
			assertThat(nestedAnnoArray[1].getClassArray("classArray")).isEqualTo(new Class<?>[] {Number.class});

			AnnotationAttributes optional = specialAttrs.getAnnotation("optional");
			assertThat(optional.getString("value")).isEqualTo("optional");
			assertThat(optional.getEnum("anEnum").equals(SomeEnum.DEFAULT)).isTrue();
			assertThat((Class<?>[]) optional.get("classArray")).isEqualTo(new Class<?>[] {Void.class});
			assertThat(optional.getClassArray("classArray")).isEqualTo(new Class<?>[] {Void.class});

			AnnotationAttributes[] optionalArray = specialAttrs.getAnnotationArray("optionalArray");
			assertThat(optionalArray).hasSize(1);
			assertThat(optionalArray[0].getString("value")).isEqualTo("optional");
			assertThat(optionalArray[0].getEnum("anEnum").equals(SomeEnum.DEFAULT)).isTrue();
			assertThat((Class<?>[]) optionalArray[0].get("classArray")).isEqualTo(new Class<?>[] {Void.class});
			assertThat(optionalArray[0].getClassArray("classArray")).isEqualTo(new Class<?>[] {Void.class});

			assertThat(metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value")).isEqualTo("direct");
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
			assertThat(new HashSet<>(allMeta)).isEqualTo(new HashSet<>(Arrays.asList("direct", "meta")));
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("additional");
			assertThat(new HashSet<>(allMeta)).isEqualTo(new HashSet<>(Arrays.asList("direct", "")));
			assertThat(metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("additional")).isEqualTo("");
			assertThat(((String[]) metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("additionalArray"))).isEmpty();
		}
		{ // perform tests with classValuesAsString = true
			AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(
				SpecialAttr.class.getName(), true);
			assertThat(specialAttrs).hasSize(6);
			assertThat(specialAttrs.get("clazz")).isEqualTo(String.class.getName());
			assertThat(specialAttrs.getString("clazz")).isEqualTo(String.class.getName());

			AnnotationAttributes nestedAnno = specialAttrs.getAnnotation("nestedAnno");
			assertThat(nestedAnno.getStringArray("classArray")).isEqualTo(new String[] { String.class.getName() });
			assertThat(nestedAnno.getStringArray("classArray")).isEqualTo(new String[] { String.class.getName() });

			AnnotationAttributes[] nestedAnnoArray = specialAttrs.getAnnotationArray("nestedAnnoArray");
			assertThat((String[]) nestedAnnoArray[0].get("classArray")).isEqualTo(new String[] { Void.class.getName() });
			assertThat(nestedAnnoArray[0].getStringArray("classArray")).isEqualTo(new String[] { Void.class.getName() });
			assertThat((String[]) nestedAnnoArray[1].get("classArray")).isEqualTo(new String[] { Number.class.getName() });
			assertThat(nestedAnnoArray[1].getStringArray("classArray")).isEqualTo(new String[] { Number.class.getName() });

			AnnotationAttributes optional = specialAttrs.getAnnotation("optional");
			assertThat((String[]) optional.get("classArray")).isEqualTo(new String[] { Void.class.getName() });
			assertThat(optional.getStringArray("classArray")).isEqualTo(new String[] { Void.class.getName() });

			AnnotationAttributes[] optionalArray = specialAttrs.getAnnotationArray("optionalArray");
			assertThat((String[]) optionalArray[0].get("classArray")).isEqualTo(new String[] { Void.class.getName() });
			assertThat(optionalArray[0].getStringArray("classArray")).isEqualTo(new String[] { Void.class.getName() });

			assertThat(metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value")).isEqualTo("direct");
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
			assertThat(new HashSet<>(allMeta)).isEqualTo(new HashSet<>(Arrays.asList("direct", "meta")));
		}
	}

	private void doTestMethodAnnotationInfo(AnnotationMetadata classMetadata) {
		assertThat(classMetadata.getDeclaredMethods()).hasSize(3);
		Set<MethodMetadata> methods = classMetadata.getAnnotatedMethods(TestAutowired.class.getName());
		assertThat(methods).hasSize(1);
		for (MethodMetadata methodMetadata : methods) {
			assertThat(methodMetadata.isAnnotated(TestAutowired.class.getName())).isTrue();
		}
	}


	// -------------------------------------------------------------------------

	public enum SomeEnum {
		LABEL1, LABEL2, DEFAULT
	}

	@Target({})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NestedAnno {

		String value() default "default";

		SomeEnum anEnum() default SomeEnum.DEFAULT;

		Class<?>[] classArray() default Void.class;
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SpecialAttr {

		Class<?> clazz();

		Thread.State state();

		NestedAnno nestedAnno();

		NestedAnno[] nestedAnnoArray();

		NestedAnno optional() default @NestedAnno(value = "optional", anEnum = SomeEnum.DEFAULT, classArray = Void.class);

		NestedAnno[] optionalArray() default { @NestedAnno(value = "optional", anEnum = SomeEnum.DEFAULT, classArray = Void.class) };
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DirectAnnotation {

		@AliasFor("myValue")
		String value() default "";

		@AliasFor("value")
		String myValue() default "";

		String additional() default "direct";

		String[] additionalArray() default "direct";
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface IsAnnotatedAnnotation {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@DirectAnnotation("meta")
	@IsAnnotatedAnnotation
	public @interface MetaAnnotation {

		String additional() default "meta";
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@MetaAnnotation
	public @interface MetaMetaAnnotation {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface EnumSubclasses {

		SubclassEnum[] value();
	}

	// SPR-10914
	public enum SubclassEnum {
		FOO {
		/* Do not delete! This subclassing is intentional. */
		},
		BAR {
		/* Do not delete! This subclassing is intentional. */
		}
	}

	@Component("myName")
	@Scope("myScope")
	@SpecialAttr(clazz = String.class, state = Thread.State.NEW,
			nestedAnno = @NestedAnno(value = "na", anEnum = SomeEnum.LABEL1, classArray = {String.class}),
			nestedAnnoArray = {@NestedAnno, @NestedAnno(value = "na1", anEnum = SomeEnum.LABEL2, classArray = {Number.class})})
	@SuppressWarnings({"serial", "unused"})
	@DirectAnnotation(value = "direct", additional = "", additionalArray = {})
	@MetaMetaAnnotation
	@EnumSubclasses({SubclassEnum.FOO, SubclassEnum.BAR})
	@NamedComposedAnnotation
	private static class AnnotatedComponent implements Serializable {

		public AnnotatedComponent() {
		}

		@TestAutowired
		public void doWork(@TestQualifier("myColor") java.awt.Color color) {
		}

		public void doSleep() {
		}

		@DirectAnnotation("direct")
		@MetaMetaAnnotation
		public void meta() {
		}
	}

	@SuppressWarnings("serial")
	private static class AnnotatedComponentSubClass extends AnnotatedComponent {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Component
	public @interface TestConfiguration {

		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface TestComponentScans {

		TestComponentScan[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Repeatable(TestComponentScans.class)
	public @interface TestComponentScan {

		@AliasFor("basePackages")
		String[] value() default {};

		@AliasFor("value")
		String[] basePackages() default {};

		Class<?>[] basePackageClasses() default {};
	}

	@TestConfiguration
	@TestComponentScan(basePackages = "bogus")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedConfigurationWithAttributeOverrides {

		// Do NOT use @AliasFor here until Spring 6.1
		// @AliasFor(annotation = TestComponentScan.class)
		String[] basePackages() default {};
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.example.componentscan")
	public static class ComposedConfigurationWithAttributeOverridesClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TestComponentScan("C")
	public @interface ScanPackageC {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TestComponentScan("D")
	public @interface ScanPackageD {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TestComponentScans({
		@TestComponentScan("C"),
		@TestComponentScan("D")
	})
	public @interface ScanPackagesCandD {
	}

	@TestComponentScan(basePackages = "A", basePackageClasses = String.class)
	@ScanPackageC
	@ScanPackageD
	@TestComponentScan(basePackages = "B", basePackageClasses = Integer.class)
	static class MultipleComposedRepeatableAnnotationsClass {
	}

	@TestComponentScan(basePackages = "A", basePackageClasses = String.class)
	@ScanPackagesCandD
	@TestComponentScans(@TestComponentScan(basePackages = "B", basePackageClasses = Integer.class))
	static class MultipleRepeatableAnnotationsInContainersClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface NamedAnnotation1 {
		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface NamedAnnotation2 {
		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface NamedAnnotation3 {
		String name() default "";
	}

	@NamedAnnotation1(name = "name 1")
	@NamedAnnotation2(name = "name 2")
	@NamedAnnotation3(name = "name 3")
	public static class NamedAnnotationsClass {
	}

	@NamedAnnotation1(name = "name 1")
	@NamedAnnotation2(name = "name 2")
	@NamedAnnotation3(name = "name 3")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface NamedComposedAnnotation {
	}

	@NamedComposedAnnotation
	public static class NamedComposedAnnotationClass {
	}

	public static class NamedComposedAnnotationExtended extends NamedComposedAnnotationClass {
	}

}
