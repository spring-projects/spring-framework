/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Iterator;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.*;

/**
 * Unit tests that verify support for getting and finding all composed, repeatable
 * annotations on a single annotated element.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-13973">SPR-13973</a>.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations
 * @see AnnotatedElementUtils#findMergedRepeatableAnnotations
 * @see AnnotatedElementUtilsTests
 * @see MultipleComposedAnnotationsOnSingleAnnotatedElementTests
 */
public class ComposedRepeatableAnnotationsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void getNonRepeatableAnnotation() {
		expectNonRepeatableAnnotation();
		getMergedRepeatableAnnotations(getClass(), NonRepeatable.class);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		expectContainerMissingValueAttribute();
		getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerMissingValueAttribute.class);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		expectContainerWithNonArrayValueAttribute();
		getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithNonArrayValueAttribute.class);
	}

	@Test
	public void getInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		expectContainerWithArrayValueAttributeButWrongComponentType();
		getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
			ContainerWithArrayValueAttributeButWrongComponentType.class);
	}

	@Test
	public void getRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	public void getRepeatableAnnotationsOnSuperclass() {
		assertGetRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	public void getComposedRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	public void getComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertGetRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	public void getComposedContainerForRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(ComposedContainerClass.class);
	}

	@Test
	public void getNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	public void getNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, Noninherited.class);
		assertNotNull(annotations);
		assertEquals(0, annotations.size());
	}

	@Test
	public void findNonRepeatableAnnotation() {
		expectNonRepeatableAnnotation();
		findMergedRepeatableAnnotations(getClass(), NonRepeatable.class);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		expectContainerMissingValueAttribute();
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerMissingValueAttribute.class);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		expectContainerWithNonArrayValueAttribute();
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithNonArrayValueAttribute.class);
	}

	@Test
	public void findInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		expectContainerWithArrayValueAttributeButWrongComponentType();
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
			ContainerWithArrayValueAttributeButWrongComponentType.class);
	}

	@Test
	public void findRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	public void findRepeatableAnnotationsOnSuperclass() {
		assertFindRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	public void findComposedRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	public void findComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	public void findNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	public void findNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	public void findComposedContainerForRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedContainerClass.class);
	}

	private void expectNonRepeatableAnnotation() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("Annotation type must be a repeatable annotation"));
		exception.expectMessage(containsString("failed to resolve container type for"));
		exception.expectMessage(containsString(NonRepeatable.class.getName()));
	}

	private void expectContainerMissingValueAttribute() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Invalid declaration of container type"));
		exception.expectMessage(containsString(ContainerMissingValueAttribute.class.getName()));
		exception.expectMessage(containsString("for repeatable annotation"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
		exception.expectCause(isA(NoSuchMethodException.class));
	}

	private void expectContainerWithNonArrayValueAttribute() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Container type"));
		exception.expectMessage(containsString(ContainerWithNonArrayValueAttribute.class.getName()));
		exception.expectMessage(containsString("must declare a 'value' attribute for an array of type"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
	}

	private void expectContainerWithArrayValueAttributeButWrongComponentType() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Container type"));
		exception.expectMessage(containsString(ContainerWithArrayValueAttributeButWrongComponentType.class.getName()));
		exception.expectMessage(containsString("must declare a 'value' attribute for an array of type"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
	}

	private void assertGetRepeatableAnnotations(AnnotatedElement element) {
		assertNotNull(element);

		Set<PeteRepeat> peteRepeats = getMergedRepeatableAnnotations(element, PeteRepeat.class);
		assertNotNull(peteRepeats);
		assertEquals(3, peteRepeats.size());

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
		assertEquals("A", iterator.next().value());
		assertEquals("B", iterator.next().value());
		assertEquals("C", iterator.next().value());
	}

	private void assertFindRepeatableAnnotations(AnnotatedElement element) {
		assertNotNull(element);

		Set<PeteRepeat> peteRepeats = findMergedRepeatableAnnotations(element, PeteRepeat.class);
		assertNotNull(peteRepeats);
		assertEquals(3, peteRepeats.size());

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
		assertEquals("A", iterator.next().value());
		assertEquals("B", iterator.next().value());
		assertEquals("C", iterator.next().value());
	}

	private void assertNoninheritedRepeatableAnnotations(Set<Noninherited> annotations) {
		assertNotNull(annotations);
		assertEquals(3, annotations.size());

		Iterator<Noninherited> iterator = annotations.iterator();
		assertEquals("A", iterator.next().value());
		assertEquals("B", iterator.next().value());
		assertEquals("C", iterator.next().value());
	}


	// -------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@interface NonRepeatable {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerMissingValueAttribute {
		// InvalidRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerWithNonArrayValueAttribute {

		InvalidRepeatable value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ContainerWithArrayValueAttributeButWrongComponentType {

		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidRepeatable {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface PeteRepeats {

		PeteRepeat[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(PeteRepeats.class)
	@interface PeteRepeat {

		String value();
	}

	@PeteRepeat("shadowed")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ForPetesSake {

		@AliasFor(annotation = PeteRepeat.class)
		String value();
	}

	@PeteRepeat("shadowed")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ForTheLoveOfFoo {

		@AliasFor(annotation = PeteRepeat.class)
		String value();
	}

	@PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface ComposedContainer {
	}

	@PeteRepeat("A")
	@PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
	static class RepeatableClass {
	}

	static class SubRepeatableClass extends RepeatableClass {
	}

	@ForPetesSake("B")
	@ForTheLoveOfFoo("C")
	@PeteRepeat("A")
	static class ComposedRepeatableClass {
	}

	@ForPetesSake("C")
	@PeteRepeats(@PeteRepeat("A"))
	@PeteRepeat("B")
	static class ComposedRepeatableMixedWithContainerClass {
	}

	@PeteRepeat("A")
	@ComposedContainer
	static class ComposedContainerClass {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Noninheriteds {

		Noninherited[] value();
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Noninheriteds.class)
	@interface Noninherited {

		@AliasFor("name")
		String value() default "";

		@AliasFor("value")
		String name() default "";
	}

	@Noninherited(name = "shadowed")
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedNoninherited {

		@AliasFor(annotation = Noninherited.class)
		String name() default "";
	}

	@ComposedNoninherited(name = "C")
	@Noninheriteds({ @Noninherited(value = "A"), @Noninherited(name = "B") })
	static class NoninheritedRepeatableClass {
	}

	static class SubNoninheritedRepeatableClass extends NoninheritedRepeatableClass {
	}

}
