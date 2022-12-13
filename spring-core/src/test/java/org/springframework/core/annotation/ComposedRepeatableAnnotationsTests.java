/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Iterator;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedRepeatableAnnotations;

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
class ComposedRepeatableAnnotationsTests {


	@Test
	void getNonRepeatableAnnotation() {
		expectNonRepeatableAnnotation(() ->
				getMergedRepeatableAnnotations(getClass(), NonRepeatable.class));
	}

	@Test
	void getInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		expectContainerMissingValueAttribute(() ->
				getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerMissingValueAttribute.class));
	}

	@Test
	void getInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		expectContainerWithNonArrayValueAttribute(() ->
				getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithNonArrayValueAttribute.class));
	}

	@Test
	void getInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		expectContainerWithArrayValueAttributeButWrongComponentType(() ->
				getMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithArrayValueAttributeButWrongComponentType.class));
	}

	@Test
	void getRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	void getRepeatableAnnotationsOnSuperclass() {
		assertGetRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	void getComposedRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	void getComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertGetRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	void getComposedContainerForRepeatableAnnotationsOnClass() {
		assertGetRepeatableAnnotations(ComposedContainerClass.class);
	}

	@Test
	void getNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	void getNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = getMergedRepeatableAnnotations(element, Noninherited.class);
		assertThat(annotations).isNotNull();
		assertThat(annotations).isEmpty();
	}

	@Test
	void findNonRepeatableAnnotation() {
		expectNonRepeatableAnnotation(() ->
				findMergedRepeatableAnnotations(getClass(), NonRepeatable.class));
	}

	@Test
	void findInvalidRepeatableAnnotationContainerMissingValueAttribute() {
		expectContainerMissingValueAttribute(() ->
				findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerMissingValueAttribute.class));
	}

	@Test
	void findInvalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		expectContainerWithNonArrayValueAttribute(() ->
				findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithNonArrayValueAttribute.class));
	}

	@Test
	void findInvalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		expectContainerWithArrayValueAttributeButWrongComponentType(() ->
				findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
					ContainerWithArrayValueAttributeButWrongComponentType.class));
	}

	@Test
	void findRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	void findRepeatableAnnotationsOnSuperclass() {
		assertFindRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	void findComposedRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	void findComposedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertFindRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	void findNoninheritedComposedRepeatableAnnotationsOnClass() {
		Class<?> element = NoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	void findNoninheritedComposedRepeatableAnnotationsOnSuperclass() {
		Class<?> element = SubNoninheritedRepeatableClass.class;
		Set<Noninherited> annotations = findMergedRepeatableAnnotations(element, Noninherited.class);
		assertNoninheritedRepeatableAnnotations(annotations);
	}

	@Test
	void findComposedContainerForRepeatableAnnotationsOnClass() {
		assertFindRepeatableAnnotations(ComposedContainerClass.class);
	}

	private void expectNonRepeatableAnnotation(ThrowingCallable throwingCallable) {
		assertThatIllegalArgumentException().isThrownBy(throwingCallable)
			.withMessageStartingWith("Annotation type must be a repeatable annotation")
			.withMessageContaining("failed to resolve container type for")
			.withMessageContaining(NonRepeatable.class.getName());
	}

	private void expectContainerMissingValueAttribute(ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(throwingCallable)
			.withMessageStartingWith("Invalid declaration of container type")
			.withMessageContaining(ContainerMissingValueAttribute.class.getName())
			.withMessageContaining("for repeatable annotation")
			.withMessageContaining(InvalidRepeatable.class.getName())
			.withCauseExactlyInstanceOf(NoSuchMethodException.class);
	}

	private void expectContainerWithNonArrayValueAttribute(ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(throwingCallable)
			.withMessageStartingWith("Container type")
			.withMessageContaining(ContainerWithNonArrayValueAttribute.class.getName())
			.withMessageContaining("must declare a 'value' attribute for an array of type")
			.withMessageContaining(InvalidRepeatable.class.getName());
	}

	private void expectContainerWithArrayValueAttributeButWrongComponentType(ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(throwingCallable)
			.withMessageStartingWith("Container type")
			.withMessageContaining(ContainerWithArrayValueAttributeButWrongComponentType.class.getName())
			.withMessageContaining("must declare a 'value' attribute for an array of type")
			.withMessageContaining(InvalidRepeatable.class.getName());
	}

	private void assertGetRepeatableAnnotations(AnnotatedElement element) {
		assertThat(element).isNotNull();

		Set<PeteRepeat> peteRepeats = getMergedRepeatableAnnotations(element, PeteRepeat.class);
		assertThat(peteRepeats).isNotNull();
		assertThat(peteRepeats).hasSize(3);

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
		assertThat(iterator.next().value()).isEqualTo("A");
		assertThat(iterator.next().value()).isEqualTo("B");
		assertThat(iterator.next().value()).isEqualTo("C");
	}

	private void assertFindRepeatableAnnotations(AnnotatedElement element) {
		assertThat(element).isNotNull();

		Set<PeteRepeat> peteRepeats = findMergedRepeatableAnnotations(element, PeteRepeat.class);
		assertThat(peteRepeats).isNotNull();
		assertThat(peteRepeats).hasSize(3);

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
		assertThat(iterator.next().value()).isEqualTo("A");
		assertThat(iterator.next().value()).isEqualTo("B");
		assertThat(iterator.next().value()).isEqualTo("C");
	}

	private void assertNoninheritedRepeatableAnnotations(Set<Noninherited> annotations) {
		assertThat(annotations).isNotNull();
		assertThat(annotations).hasSize(3);

		Iterator<Noninherited> iterator = annotations.iterator();
		assertThat(iterator.next().value()).isEqualTo("A");
		assertThat(iterator.next().value()).isEqualTo("B");
		assertThat(iterator.next().value()).isEqualTo("C");
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
