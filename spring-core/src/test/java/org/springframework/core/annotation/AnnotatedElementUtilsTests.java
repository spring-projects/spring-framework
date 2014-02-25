/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AnnotatedElementUtils}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
public class AnnotatedElementUtilsTests {

	@Test
	public void getAnnotationAttributesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		AnnotationAttributes attributes = AnnotatedElementUtils.getAnnotationAttributes(MetaCycleAnnotatedClass.class,
			Transactional.class.getName());
		assertNull("Should not find annotation attributes for @Transactional on MetaCycleAnnotatedClass", attributes);
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		AnnotationAttributes attributes = AnnotatedElementUtils.getAnnotationAttributes(
			SubSubClassWithInheritedAnnotation.class, Transactional.class.getName());
		assertNotNull(attributes);

		// By inspecting SubSubClassWithInheritedAnnotation, one might expect that the
		// readOnly flag should be true, since the immediate superclass is annotated with
		// @Composed2; however, with the current implementation the readOnly flag will be
		// false since @Transactional is declared as @Inherited.
		assertFalse("readOnly flag for SubSubClassWithInheritedAnnotation", attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		AnnotationAttributes attributes = AnnotatedElementUtils.getAnnotationAttributes(
			SubSubClassWithInheritedComposedAnnotation.class, Transactional.class.getName());
		assertNotNull(attributes);

		// By inspecting SubSubClassWithInheritedComposedAnnotation, one might expect that
		// the readOnly flag should be true, since the immediate superclass is annotated
		// with @Composed2; however, with the current implementation the readOnly flag
		// will be false since @Composed1 is declared as @Inherited.
		assertFalse("readOnly flag", attributes.getBoolean("readOnly"));
	}


	// -------------------------------------------------------------------------

	@MetaCycle3
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@Documented
	@interface MetaCycle1 {
	}

	@MetaCycle1
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@Documented
	@interface MetaCycle2 {
	}

	@MetaCycle2
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface MetaCycle3 {
	}

	@MetaCycle3
	static class MetaCycleAnnotatedClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@Inherited
	@interface Transactional {

		boolean readOnly() default false;
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@Inherited
	@interface Composed1 {
	}

	@Transactional(readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface Composed2 {
	}

	@Transactional
	static class ClassWithInheritedAnnotation {
	}

	@Composed2
	static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class SubSubClassWithInheritedAnnotation extends SubClassWithInheritedAnnotation {
	}

	@Composed1
	static class ClassWithInheritedComposedAnnotation {
	}

	@Composed2
	static class SubClassWithInheritedComposedAnnotation extends ClassWithInheritedComposedAnnotation {
	}

	static class SubSubClassWithInheritedComposedAnnotation extends SubClassWithInheritedComposedAnnotation {
	}

}
