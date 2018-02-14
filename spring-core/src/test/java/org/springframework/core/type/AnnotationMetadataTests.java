/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.type;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests demonstrating that the reflection-based {@link StandardAnnotationMetadata}
 * and ASM-based {@code AnnotationMetadataReadingVisitor} produce identical output.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class AnnotationMetadataTests {

	@Test
	public void standardAnnotationMetadata() throws Exception {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(AnnotatedComponent.class, true);
		doTestAnnotationInfo(metadata);
		doTestMethodAnnotationInfo(metadata);
	}

	@Test
	public void asmAnnotationMetadata() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponent.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestAnnotationInfo(metadata);
		doTestMethodAnnotationInfo(metadata);
	}

	@Test
	public void standardAnnotationMetadataForSubclass() throws Exception {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(AnnotatedComponentSubClass.class, true);
		doTestSubClassAnnotationInfo(metadata);
	}

	@Test
	public void asmAnnotationMetadataForSubclass() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponentSubClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestSubClassAnnotationInfo(metadata);
	}

	private void doTestSubClassAnnotationInfo(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName(), is(AnnotatedComponentSubClass.class.getName()));
		assertThat(metadata.isInterface(), is(false));
		assertThat(metadata.isAnnotation(), is(false));
		assertThat(metadata.isAbstract(), is(false));
		assertThat(metadata.isConcrete(), is(true));
		assertThat(metadata.hasSuperClass(), is(true));
		assertThat(metadata.getSuperClassName(), is(AnnotatedComponent.class.getName()));
		assertThat(metadata.getInterfaceNames().length, is(0));
		assertThat(metadata.isAnnotated(Component.class.getName()), is(false));
		assertThat(metadata.isAnnotated(Scope.class.getName()), is(false));
		assertThat(metadata.isAnnotated(SpecialAttr.class.getName()), is(false));
		assertThat(metadata.hasAnnotation(Component.class.getName()), is(false));
		assertThat(metadata.hasAnnotation(Scope.class.getName()), is(false));
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName()), is(false));
		assertThat(metadata.getAnnotationTypes().size(), is(0));
		assertThat(metadata.getAnnotationAttributes(Component.class.getName()), nullValue());
		assertThat(metadata.getAnnotatedMethods(DirectAnnotation.class.getName()).size(), equalTo(0));
		assertThat(metadata.isAnnotated(IsAnnotatedAnnotation.class.getName()), equalTo(false));
		assertThat(metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()), nullValue());
	}

	@Test
	public void standardAnnotationMetadataForInterface() throws Exception {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(AnnotationMetadata.class, true);
		doTestMetadataForInterfaceClass(metadata);
	}

	@Test
	public void asmAnnotationMetadataForInterface() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotationMetadata.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestMetadataForInterfaceClass(metadata);
	}

	private void doTestMetadataForInterfaceClass(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName(), is(AnnotationMetadata.class.getName()));
		assertThat(metadata.isInterface(), is(true));
		assertThat(metadata.isAnnotation(), is(false));
		assertThat(metadata.isAbstract(), is(true));
		assertThat(metadata.isConcrete(), is(false));
		assertThat(metadata.hasSuperClass(), is(false));
		assertThat(metadata.getSuperClassName(), nullValue());
		assertThat(metadata.getInterfaceNames().length, is(2));
		assertThat(metadata.getInterfaceNames()[0], is(ClassMetadata.class.getName()));
		assertThat(metadata.getInterfaceNames()[1], is(AnnotatedTypeMetadata.class.getName()));
		assertThat(metadata.getAnnotationTypes().size(), is(0));
	}

	@Test
	public void standardAnnotationMetadataForAnnotation() throws Exception {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(Component.class, true);
		doTestMetadataForAnnotationClass(metadata);
	}

	@Test
	public void asmAnnotationMetadataForAnnotation() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(Component.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		doTestMetadataForAnnotationClass(metadata);
	}

	private void doTestMetadataForAnnotationClass(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName(), is(Component.class.getName()));
		assertThat(metadata.isInterface(), is(true));
		assertThat(metadata.isAnnotation(), is(true));
		assertThat(metadata.isAbstract(), is(true));
		assertThat(metadata.isConcrete(), is(false));
		assertThat(metadata.hasSuperClass(), is(false));
		assertThat(metadata.getSuperClassName(), nullValue());
		assertThat(metadata.getInterfaceNames().length, is(1));
		assertThat(metadata.getInterfaceNames()[0], is(Annotation.class.getName()));
		assertThat(metadata.isAnnotated(Documented.class.getName()), is(false));
		assertThat(metadata.isAnnotated(Scope.class.getName()), is(false));
		assertThat(metadata.isAnnotated(SpecialAttr.class.getName()), is(false));
		assertThat(metadata.hasAnnotation(Documented.class.getName()), is(true));
		assertThat(metadata.hasAnnotation(Scope.class.getName()), is(false));
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName()), is(false));
		assertThat(metadata.getAnnotationTypes().size(), is(4));
	}

	/**
	 * In order to preserve backward-compatibility, {@link StandardAnnotationMetadata}
	 * defaults to return nested annotations and annotation arrays as actual
	 * Annotation instances. It is recommended for compatibility with ASM-based
	 * AnnotationMetadata implementations to set the 'nestedAnnotationsAsMap' flag to
	 * 'true' as is done in the main test above.
	 */
	@Test
	public void standardAnnotationMetadata_nestedAnnotationsAsMap_false() throws Exception {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(AnnotatedComponent.class);
		AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(SpecialAttr.class.getName());
		Annotation[] nestedAnnoArray = (Annotation[]) specialAttrs.get("nestedAnnoArray");
		assertThat(nestedAnnoArray[0], instanceOf(NestedAnno.class));
	}

	@Test
	public void metaAnnotationOverridesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(ComposedConfigurationWithAttributeOverridesClass.class);
		assertMetaAnnotationOverrides(metadata);
	}

	@Test
	public void metaAnnotationOverridesUsingAnnotationMetadataReadingVisitor() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(ComposedConfigurationWithAttributeOverridesClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMetaAnnotationOverrides(metadata);
	}

	private void assertMetaAnnotationOverrides(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = (AnnotationAttributes) metadata.getAnnotationAttributes(
				TestComponentScan.class.getName(), false);
		String[] basePackages = attributes.getStringArray("basePackages");
		assertThat("length of basePackages[]", basePackages.length, is(1));
		assertThat("basePackages[0]", basePackages[0], is("org.example.componentscan"));
		String[] value = attributes.getStringArray("value");
		assertThat("length of value[]", value.length, is(0));
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		assertThat("length of basePackageClasses[]", basePackageClasses.length, is(0));
	}

	/**
	 * https://jira.spring.io/browse/SPR-11649
	 */
	@Test
	public void multipleAnnotationsWithIdenticalAttributeNamesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(NamedAnnotationsClass.class);
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	/**
	 * https://jira.spring.io/browse/SPR-11649
	 */
	@Test
	public void multipleAnnotationsWithIdenticalAttributeNamesUsingAnnotationMetadataReadingVisitor() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NamedAnnotationsClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	/**
	 * https://jira.spring.io/browse/SPR-11649
	 */
	@Test
	public void composedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingStandardAnnotationMetadata() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(NamedComposedAnnotationClass.class);
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}

	/**
	 * https://jira.spring.io/browse/SPR-11649
	 */
	@Test
	public void composedAnnotationWithMetaAnnotationsWithIdenticalAttributeNamesUsingAnnotationMetadataReadingVisitor() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(NamedComposedAnnotationClass.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		assertMultipleAnnotationsWithIdenticalAttributeNames(metadata);
	}


	private void assertMultipleAnnotationsWithIdenticalAttributeNames(AnnotationMetadata metadata) {
		AnnotationAttributes attributes1 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation1.class.getName(), false);
		String name1 = attributes1.getString("name");
		assertThat("name of NamedAnnotation1", name1, is("name 1"));

		AnnotationAttributes attributes2 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation2.class.getName(), false);
		String name2 = attributes2.getString("name");
		assertThat("name of NamedAnnotation2", name2, is("name 2"));

		AnnotationAttributes attributes3 = (AnnotationAttributes) metadata.getAnnotationAttributes(
				NamedAnnotation3.class.getName(), false);
		String name3 = attributes3.getString("name");
		assertThat("name of NamedAnnotation3", name3, is("name 3"));
	}

	private void doTestAnnotationInfo(AnnotationMetadata metadata) {
		assertThat(metadata.getClassName(), is(AnnotatedComponent.class.getName()));
		assertThat(metadata.isInterface(), is(false));
		assertThat(metadata.isAnnotation(), is(false));
		assertThat(metadata.isAbstract(), is(false));
		assertThat(metadata.isConcrete(), is(true));
		assertThat(metadata.hasSuperClass(), is(true));
		assertThat(metadata.getSuperClassName(), is(Object.class.getName()));
		assertThat(metadata.getInterfaceNames().length, is(1));
		assertThat(metadata.getInterfaceNames()[0], is(Serializable.class.getName()));

		assertThat(metadata.hasAnnotation(Component.class.getName()), is(true));
		assertThat(metadata.hasAnnotation(Scope.class.getName()), is(true));
		assertThat(metadata.hasAnnotation(SpecialAttr.class.getName()), is(true));
		assertThat(metadata.getAnnotationTypes().size(), is(6));
		assertThat(metadata.getAnnotationTypes().contains(Component.class.getName()), is(true));
		assertThat(metadata.getAnnotationTypes().contains(Scope.class.getName()), is(true));
		assertThat(metadata.getAnnotationTypes().contains(SpecialAttr.class.getName()), is(true));

		AnnotationAttributes compAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(Component.class.getName());
		assertThat(compAttrs.size(), is(1));
		assertThat(compAttrs.getString("value"), is("myName"));
		AnnotationAttributes scopeAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(Scope.class.getName());
		assertThat(scopeAttrs.size(), is(1));
		assertThat(scopeAttrs.getString("value"), is("myScope"));

		Set<MethodMetadata> methods = metadata.getAnnotatedMethods(DirectAnnotation.class.getName());
		MethodMetadata method = methods.iterator().next();
		assertEquals("direct", method.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value"));
		assertEquals("direct", method.getAnnotationAttributes(DirectAnnotation.class.getName()).get("myValue"));
		List<Object> allMeta = method.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
		assertThat(new HashSet<>(allMeta), is(equalTo(new HashSet<Object>(Arrays.asList("direct", "meta")))));
		allMeta = method.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("additional");
		assertThat(new HashSet<>(allMeta), is(equalTo(new HashSet<Object>(Arrays.asList("direct")))));

		assertTrue(metadata.isAnnotated(IsAnnotatedAnnotation.class.getName()));

		{ // perform tests with classValuesAsString = false (the default)
			AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(SpecialAttr.class.getName());
			assertThat(specialAttrs.size(), is(6));
			assertTrue(String.class.isAssignableFrom(specialAttrs.getClass("clazz")));
			assertTrue(specialAttrs.getEnum("state").equals(Thread.State.NEW));

			AnnotationAttributes nestedAnno = specialAttrs.getAnnotation("nestedAnno");
			assertThat("na", is(nestedAnno.getString("value")));
			assertTrue(nestedAnno.getEnum("anEnum").equals(SomeEnum.LABEL1));
			assertArrayEquals(new Class<?>[] {String.class}, (Class<?>[]) nestedAnno.get("classArray"));

			AnnotationAttributes[] nestedAnnoArray = specialAttrs.getAnnotationArray("nestedAnnoArray");
			assertThat(nestedAnnoArray.length, is(2));
			assertThat(nestedAnnoArray[0].getString("value"), is("default"));
			assertTrue(nestedAnnoArray[0].getEnum("anEnum").equals(SomeEnum.DEFAULT));
			assertArrayEquals(new Class<?>[] {Void.class}, (Class<?>[]) nestedAnnoArray[0].get("classArray"));
			assertThat(nestedAnnoArray[1].getString("value"), is("na1"));
			assertTrue(nestedAnnoArray[1].getEnum("anEnum").equals(SomeEnum.LABEL2));
			assertArrayEquals(new Class<?>[] {Number.class}, (Class<?>[]) nestedAnnoArray[1].get("classArray"));
			assertArrayEquals(new Class<?>[] {Number.class}, nestedAnnoArray[1].getClassArray("classArray"));

			AnnotationAttributes optional = specialAttrs.getAnnotation("optional");
			assertThat(optional.getString("value"), is("optional"));
			assertTrue(optional.getEnum("anEnum").equals(SomeEnum.DEFAULT));
			assertArrayEquals(new Class<?>[] {Void.class}, (Class<?>[]) optional.get("classArray"));
			assertArrayEquals(new Class<?>[] {Void.class}, optional.getClassArray("classArray"));

			AnnotationAttributes[] optionalArray = specialAttrs.getAnnotationArray("optionalArray");
			assertThat(optionalArray.length, is(1));
			assertThat(optionalArray[0].getString("value"), is("optional"));
			assertTrue(optionalArray[0].getEnum("anEnum").equals(SomeEnum.DEFAULT));
			assertArrayEquals(new Class<?>[] {Void.class}, (Class<?>[]) optionalArray[0].get("classArray"));
			assertArrayEquals(new Class<?>[] {Void.class}, optionalArray[0].getClassArray("classArray"));

			assertEquals("direct", metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value"));
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
			assertThat(new HashSet<>(allMeta), is(equalTo(new HashSet<Object>(Arrays.asList("direct", "meta")))));
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("additional");
			assertThat(new HashSet<>(allMeta), is(equalTo(new HashSet<Object>(Arrays.asList("direct")))));
		}
		{ // perform tests with classValuesAsString = true
			AnnotationAttributes specialAttrs = (AnnotationAttributes) metadata.getAnnotationAttributes(
				SpecialAttr.class.getName(), true);
			assertThat(specialAttrs.size(), is(6));
			assertThat(specialAttrs.get("clazz"), is((Object) String.class.getName()));
			assertThat(specialAttrs.getString("clazz"), is(String.class.getName()));

			AnnotationAttributes nestedAnno = specialAttrs.getAnnotation("nestedAnno");
			assertArrayEquals(new String[] { String.class.getName() }, nestedAnno.getStringArray("classArray"));
			assertArrayEquals(new String[] { String.class.getName() }, nestedAnno.getStringArray("classArray"));

			AnnotationAttributes[] nestedAnnoArray = specialAttrs.getAnnotationArray("nestedAnnoArray");
			assertArrayEquals(new String[] { Void.class.getName() }, (String[]) nestedAnnoArray[0].get("classArray"));
			assertArrayEquals(new String[] { Void.class.getName() }, nestedAnnoArray[0].getStringArray("classArray"));
			assertArrayEquals(new String[] { Number.class.getName() }, (String[]) nestedAnnoArray[1].get("classArray"));
			assertArrayEquals(new String[] { Number.class.getName() }, nestedAnnoArray[1].getStringArray("classArray"));

			AnnotationAttributes optional = specialAttrs.getAnnotation("optional");
			assertArrayEquals(new String[] { Void.class.getName() }, (String[]) optional.get("classArray"));
			assertArrayEquals(new String[] { Void.class.getName() }, optional.getStringArray("classArray"));

			AnnotationAttributes[] optionalArray = specialAttrs.getAnnotationArray("optionalArray");
			assertArrayEquals(new String[] { Void.class.getName() }, (String[]) optionalArray[0].get("classArray"));
			assertArrayEquals(new String[] { Void.class.getName() }, optionalArray[0].getStringArray("classArray"));

			assertEquals("direct", metadata.getAnnotationAttributes(DirectAnnotation.class.getName()).get("value"));
			allMeta = metadata.getAllAnnotationAttributes(DirectAnnotation.class.getName()).get("value");
			assertThat(new HashSet<>(allMeta), is(equalTo(new HashSet<Object>(Arrays.asList("direct", "meta")))));
		}
	}

	private void doTestMethodAnnotationInfo(AnnotationMetadata classMetadata) {
		Set<MethodMetadata> methods = classMetadata.getAnnotatedMethods(TestAutowired.class.getName());
		assertThat(methods.size(), is(1));
		for (MethodMetadata methodMetadata : methods) {
			assertThat(methodMetadata.isAnnotated(TestAutowired.class.getName()), is(true));
		}
	}


	// -------------------------------------------------------------------------

	public static enum SomeEnum {
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
	@DirectAnnotation("direct")
	@MetaMetaAnnotation
	@EnumSubclasses({SubclassEnum.FOO, SubclassEnum.BAR})
	private static class AnnotatedComponent implements Serializable {

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
	public @interface TestComponentScan {

		String[] value() default {};

		String[] basePackages() default {};

		Class<?>[] basePackageClasses() default {};
	}

	@TestConfiguration
	@TestComponentScan(basePackages = "bogus")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedConfigurationWithAttributeOverrides {

		String[] basePackages() default {};
	}

	@ComposedConfigurationWithAttributeOverrides(basePackages = "org.example.componentscan")
	public static class ComposedConfigurationWithAttributeOverridesClass {
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
	public @interface NamedComposedAnnotation {
	}

	@NamedComposedAnnotation
	public static class NamedComposedAnnotationClass {
	}

}
