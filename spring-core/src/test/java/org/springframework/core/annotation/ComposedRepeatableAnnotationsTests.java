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
 * Unit tests that verify support for finding all composed, repeatable
 * annotations on a single annotated element.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-13973">SPR-13973</a>.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see AnnotatedElementUtils
 * @see AnnotatedElementUtilsTests
 */
public class ComposedRepeatableAnnotationsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void nonRepeatableAnnotation() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(startsWith("annotationType must be a repeatable annotation"));
		exception.expectMessage(containsString("failed to resolve container type for"));
		exception.expectMessage(containsString(NonRepeatable.class.getName()));
		findMergedRepeatableAnnotations(getClass(), NonRepeatable.class);
	}

	@Test
	public void invalidRepeatableAnnotationContainerMissingValueAttribute() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Invalid declaration of container type"));
		exception.expectMessage(containsString(ContainerMissingValueAttribute.class.getName()));
		exception.expectMessage(containsString("for repeatable annotation"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
		exception.expectCause(isA(NoSuchMethodException.class));
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerMissingValueAttribute.class);
	}

	@Test
	public void invalidRepeatableAnnotationContainerWithNonArrayValueAttribute() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Container type"));
		exception.expectMessage(containsString(ContainerWithNonArrayValueAttribute.class.getName()));
		exception.expectMessage(containsString("must declare a 'value' attribute for an array of type"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class, ContainerWithNonArrayValueAttribute.class);
	}

	@Test
	public void invalidRepeatableAnnotationContainerWithArrayValueAttributeButWrongComponentType() {
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(startsWith("Container type"));
		exception.expectMessage(containsString(ContainerWithArrayValueAttributeButWrongComponentType.class.getName()));
		exception.expectMessage(containsString("must declare a 'value' attribute for an array of type"));
		exception.expectMessage(containsString(InvalidRepeatable.class.getName()));
		findMergedRepeatableAnnotations(getClass(), InvalidRepeatable.class,
			ContainerWithArrayValueAttributeButWrongComponentType.class);
	}

	@Test
	public void repeatableAnnotationsOnClass() {
		assertRepeatableAnnotations(RepeatableClass.class);
	}

	@Test
	public void repeatableAnnotationsOnSuperclass() {
		assertRepeatableAnnotations(SubRepeatableClass.class);
	}

	@Test
	public void composedRepeatableAnnotationsOnClass() {
		assertRepeatableAnnotations(ComposedRepeatableClass.class);
	}

	@Test
	public void composedRepeatableAnnotationsMixedWithContainerOnClass() {
		assertRepeatableAnnotations(ComposedRepeatableMixedWithContainerClass.class);
	}

	@Test
	public void composedContainerForRepeatableAnnotationsOnClass() {
		assertRepeatableAnnotations(ComposedContainerClass.class);
	}

	private void assertRepeatableAnnotations(AnnotatedElement element) {
		assertNotNull(element);

		Set<PeteRepeat> peteRepeats = findMergedRepeatableAnnotations(element, PeteRepeat.class);
		assertNotNull(peteRepeats);
		assertEquals(3, peteRepeats.size());

		Iterator<PeteRepeat> iterator = peteRepeats.iterator();
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

}
