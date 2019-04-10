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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MergedAnnotations} and {@link RepeatableContainers} that
 * verify support for repeatable annotations.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class MergedAnnotationsRepeatableAnnotationTests {

	// See SPR-13973

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void inheritedAnnotationsWhenNonRepeatableThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> getAnnotations(null, NonRepeatable.class,
						SearchStrategy.INHERITED_ANNOTATIONS, getClass())).satisfies(
								this::nonRepeatableRequirements);
	}

	@Test
	public void inheritedAnnotationsWhenContainerMissingValueAttributeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getAnnotations(ContainerMissingValueAttribute.class,
						InvalidRepeatable.class, SearchStrategy.INHERITED_ANNOTATIONS,
						getClass())).satisfies(this::missingValueAttributeRequirements);
	}

	@Test
	public void inheritedAnnotationsWhenWhenNonArrayValueAttributeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getAnnotations(ContainerWithNonArrayValueAttribute.class,
						InvalidRepeatable.class, SearchStrategy.INHERITED_ANNOTATIONS,
						getClass())).satisfies(this::nonArrayValueAttributeRequirements);
	}

	@Test
	public void inheritedAnnotationsWhenWrongComponentTypeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(() -> getAnnotations(
				ContainerWithArrayValueAttributeButWrongComponentType.class,
				InvalidRepeatable.class, SearchStrategy.INHERITED_ANNOTATIONS,
				getClass())).satisfies(this::wrongComponentTypeRequirements);
	}

	@Test
	public void inheritedAnnotationsWhenOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.INHERITED_ANNOTATIONS, RepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void inheritedAnnotationsWhenWhenOnSuperclassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.INHERITED_ANNOTATIONS, SubRepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void inheritedAnnotationsWhenComposedOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.INHERITED_ANNOTATIONS, ComposedRepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void inheritedAnnotationsWhenComposedMixedWithContainerOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.INHERITED_ANNOTATIONS,
				ComposedRepeatableMixedWithContainerClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void inheritedAnnotationsWhenComposedContainerForRepeatableOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.INHERITED_ANNOTATIONS, ComposedContainerClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void inheritedAnnotationsWhenNoninheritedComposedRepeatableOnClassReturnsAnnotations() {
		Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
				SearchStrategy.INHERITED_ANNOTATIONS, NoninheritedRepeatableClass.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	@Test
	public void inheritedAnnotationsWhenNoninheritedComposedRepeatableOnSuperclassReturnsAnnotations() {
		Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
				SearchStrategy.INHERITED_ANNOTATIONS,
				SubNoninheritedRepeatableClass.class);
		assertThat(annotations).isEmpty();
	}

	@Test
	public void exhaustiveWhenNonRepeatableThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> getAnnotations(null,
				NonRepeatable.class, SearchStrategy.EXHAUSTIVE, getClass())).satisfies(
						this::nonRepeatableRequirements);
	}

	@Test
	public void exhaustiveWhenContainerMissingValueAttributeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getAnnotations(ContainerMissingValueAttribute.class,
						InvalidRepeatable.class, SearchStrategy.EXHAUSTIVE,
						getClass())).satisfies(this::missingValueAttributeRequirements);
	}

	@Test
	public void exhaustiveWhenWhenNonArrayValueAttributeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(
				() -> getAnnotations(ContainerWithNonArrayValueAttribute.class,
						InvalidRepeatable.class, SearchStrategy.EXHAUSTIVE,
						getClass())).satisfies(this::nonArrayValueAttributeRequirements);
	}

	@Test
	public void exhaustiveWhenWrongComponentTypeThrowsException() {
		assertThatAnnotationConfigurationException().isThrownBy(() -> getAnnotations(
				ContainerWithArrayValueAttributeButWrongComponentType.class,
				InvalidRepeatable.class, SearchStrategy.EXHAUSTIVE,
				getClass())).satisfies(this::wrongComponentTypeRequirements);
	}

	@Test
	public void exhaustiveWhenOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.EXHAUSTIVE, RepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void exhaustiveWhenWhenOnSuperclassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.EXHAUSTIVE, SubRepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void exhaustiveWhenComposedOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.EXHAUSTIVE, ComposedRepeatableClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void exhaustiveWhenComposedMixedWithContainerOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.EXHAUSTIVE,
				ComposedRepeatableMixedWithContainerClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void exhaustiveWhenComposedContainerForRepeatableOnClassReturnsAnnotations() {
		Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
				SearchStrategy.EXHAUSTIVE, ComposedContainerClass.class);
		assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
				"C");
	}

	@Test
	public void exhaustiveAnnotationsWhenNoninheritedComposedRepeatableOnClassReturnsAnnotations() {
		Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
				SearchStrategy.EXHAUSTIVE, NoninheritedRepeatableClass.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	@Test
	public void exhaustiveAnnotationsWhenNoninheritedComposedRepeatableOnSuperclassReturnsAnnotations() {
		Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
				SearchStrategy.EXHAUSTIVE, SubNoninheritedRepeatableClass.class);
		assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
				"B", "C");
	}

	private <A extends Annotation> Set<A> getAnnotations(
			Class<? extends Annotation> container, Class<A> repeatable,
			SearchStrategy searchStrategy, AnnotatedElement element) {
		RepeatableContainers containers = RepeatableContainers.of(repeatable, container);
		MergedAnnotations annotations = MergedAnnotations.from(element,
				searchStrategy, containers, AnnotationFilter.PLAIN);
		return annotations.stream(repeatable).collect(
				MergedAnnotationCollectors.toAnnotationSet());
	}

	private void nonRepeatableRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith(
				"Annotation type must be a repeatable annotation").contains(
						"failed to resolve container type for",
						NonRepeatable.class.getName());
	}

	private void missingValueAttributeRequirements(Exception ex) {
		ex.printStackTrace();
		assertThat(ex.getMessage()).startsWith(
				"Invalid declaration of container type").contains(
						ContainerMissingValueAttribute.class.getName(),
						"for repeatable annotation", InvalidRepeatable.class.getName());
		assertThat(ex).hasCauseInstanceOf(NoSuchMethodException.class);
	}

	private void nonArrayValueAttributeRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith("Container type").contains(
				ContainerWithNonArrayValueAttribute.class.getName(),
				"must declare a 'value' attribute for an array of type",
				InvalidRepeatable.class.getName());
	}

	private void wrongComponentTypeRequirements(Exception ex) {
		assertThat(ex.getMessage()).startsWith("Container type").contains(
				ContainerWithArrayValueAttributeButWrongComponentType.class.getName(),
				"must declare a 'value' attribute for an array of type",
				InvalidRepeatable.class.getName());
	}

	private static ThrowableTypeAssert<AnnotationConfigurationException> assertThatAnnotationConfigurationException() {
		return assertThatExceptionOfType(AnnotationConfigurationException.class);
	}

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
