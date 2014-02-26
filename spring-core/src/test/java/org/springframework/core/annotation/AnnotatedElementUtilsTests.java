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
import java.util.Arrays;

import org.junit.Test;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.*;

/**
 * Unit tests for {@link AnnotatedElementUtils}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
public class AnnotatedElementUtilsTests {

	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotation() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxConfig.class,
			Transactional.class.getName());
		assertNotNull("Annotation attributes map for @Transactional on TxConfig", attributes);
		assertEquals("value for TxConfig.", Arrays.asList("TxConfig"), attributes.get("value"));
	}

	/**
	 * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then
	 * the algorithm is accidentally picking up shadowed annotations of the same
	 * type within the class hierarchy. Such undesirable behavior would cause the
	 * logic in {@link org.springframework.context.annotation.ProfileCondition}
	 * to fail.
	 *
	 * @see org.springframework.core.env.EnvironmentIntegrationTests#mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(DerivedTxConfig.class,
			Transactional.class.getName());
		assertNotNull("Annotation attributes map for @Transactional on DerivedTxConfig", attributes);
		assertEquals("value for DerivedTxConfig.", Arrays.asList("DerivedTxConfig"), attributes.get("value"));
	}

	/**
	 * Note: this functionality is required by {@link org.springframework.context.annotation.ProfileCondition}.
	 *
	 * @see org.springframework.core.env.EnvironmentIntegrationTests
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithMultipleComposedAnnotations() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxFromMultipleComposedAnnotations.class,
			Transactional.class.getName());
		assertNotNull("Annotation attributes map for @Transactional on TxFromMultipleComposedAnnotations", attributes);
		assertEquals("value for TxFromMultipleComposedAnnotations.", Arrays.asList("TxComposed1", "TxComposed2"),
			attributes.get("value"));
	}

	@Test
	public void getAnnotationAttributesOnClassWithLocalAnnotation() {
		AnnotationAttributes attributes = getAnnotationAttributes(TxConfig.class, Transactional.class.getName());
		assertNotNull("Annotation attributes for @Transactional on TxConfig", attributes);
		assertEquals("value for TxConfig.", "TxConfig", attributes.getString("value"));
	}

	@Test
	public void getAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		AnnotationAttributes attributes = getAnnotationAttributes(DerivedTxConfig.class, Transactional.class.getName());
		assertNotNull("Annotation attributes for @Transactional on DerivedTxConfig", attributes);
		assertEquals("value for DerivedTxConfig.", "DerivedTxConfig", attributes.getString("value"));
	}

	@Test
	public void getAnnotationAttributesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		AnnotationAttributes attributes = getAnnotationAttributes(MetaCycleAnnotatedClass.class,
			Transactional.class.getName());
		assertNull("Should not find annotation attributes for @Transactional on MetaCycleAnnotatedClass", attributes);
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		AnnotationAttributes attributes = getAnnotationAttributes(SubSubClassWithInheritedAnnotation.class,
			Transactional.class.getName());
		assertNotNull("AnnotationAttributes for @Transactional on SubSubClassWithInheritedAnnotation", attributes);
		assertEquals("readOnly flag for SubSubClassWithInheritedAnnotation.", true, attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		AnnotationAttributes attributes = getAnnotationAttributes(SubSubClassWithInheritedComposedAnnotation.class,
			Transactional.class.getName());
		assertNotNull("AnnotationAttributtes for @Transactional on SubSubClassWithInheritedComposedAnnotation.",
			attributes);
		assertEquals("readOnly flag for SubSubClassWithInheritedComposedAnnotation.", true,
			attributes.getBoolean("readOnly"));
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

	// -------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@Inherited
	@interface Transactional {

		String value() default "";

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

	@Transactional("TxComposed1")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed1 {
	}

	@Transactional("TxComposed2")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed2 {
	}

	// -------------------------------------------------------------------------

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

	@Transactional("TxConfig")
	static class TxConfig {
	}

	@Transactional("DerivedTxConfig")
	static class DerivedTxConfig extends TxConfig {
	}

	@TxComposed1
	@TxComposed2
	static class TxFromMultipleComposedAnnotations {
	}

}
