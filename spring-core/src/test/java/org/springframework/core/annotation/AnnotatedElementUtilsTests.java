/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.*;

/**
 * Unit tests for {@link AnnotatedElementUtils}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
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

		// TODO [SPR-11598] Set expected to true.
		boolean expected = false;
		assertEquals("readOnly flag for SubSubClassWithInheritedAnnotation.", expected, attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		AnnotationAttributes attributes = getAnnotationAttributes(SubSubClassWithInheritedComposedAnnotation.class,
			Transactional.class.getName());
		assertNotNull("AnnotationAttributtes for @Transactional on SubSubClassWithInheritedComposedAnnotation.",
			attributes);

		// TODO [SPR-11598] Set expected to true.
		boolean expected = false;
		assertEquals("readOnly flag for SubSubClassWithInheritedComposedAnnotation.", expected,
			attributes.getBoolean("readOnly"));
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnInheritedAnnotationInterface() {
		String name = Transactional.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(InheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Transactional on InheritedAnnotationInterface", attributes);
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnSubInheritedAnnotationInterface() {
		String name = Transactional.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(SubInheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Transactional on SubInheritedAnnotationInterface", attributes);
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
		String name = Transactional.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(SubSubInheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Transactional on SubSubInheritedAnnotationInterface", attributes);
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnNonInheritedAnnotationInterface() {
		String name = Order.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(NonInheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Order on NonInheritedAnnotationInterface", attributes);
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
		String name = Order.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(SubNonInheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Order on SubNonInheritedAnnotationInterface", attributes);
	}

	/** @since 4.2 */
	@Test
	public void getAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
		String name = Order.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(SubSubNonInheritedAnnotationInterface.class, name);
		assertNotNull("Should find @Order on SubSubNonInheritedAnnotationInterface", attributes);
	}

	// TODO [SPR-11598] Enable test.
	@Ignore("Disabled until SPR-11598 is resolved")
	@Test
	public void getAnnotationAttributesFromInterfaceImplementedBySuperclass() {
		String name = Transactional.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(ConcreteClassWithInheritedAnnotation.class, name);
		assertNotNull("Should find @Transactional on ConcreteClassWithInheritedAnnotation", attributes);
	}

	// TODO [SPR-12738] Enable test.
	@Ignore("Disabled until SPR-12738 is resolved")
	@Test
	public void getAnnotationAttributesInheritedFromInterfaceMethod() throws NoSuchMethodException {
		String name = Order.class.getName();
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleFromInterface");
		AnnotationAttributes attributes = getAnnotationAttributes(method, name);
		assertNotNull("Should find @Order on ConcreteClassWithInheritedAnnotation.handleFromInterface() method",
			attributes);
	}

	// TODO [SPR-12738] Enable test.
	@Ignore("Disabled until SPR-12738 is resolved")
	@Test
	public void getAnnotationAttributesInheritedFromAbstractMethod() throws NoSuchMethodException {
		String name = Transactional.class.getName();
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
		AnnotationAttributes attributes = getAnnotationAttributes(method, name);
		assertNotNull("Should find @Transactional on ConcreteClassWithInheritedAnnotation.handle() method", attributes);
	}

	// TODO [SPR-12738] Enable test.
	@Ignore("Disabled until SPR-12738 is resolved")
	@Test
	public void getAnnotationAttributesInheritedFromParameterizedMethod() throws NoSuchMethodException {
		String name = Transactional.class.getName();
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleParameterized", String.class);
		AnnotationAttributes attributes = getAnnotationAttributes(method, name);
		assertNotNull("Should find @Transactional on ConcreteClassWithInheritedAnnotation.handleParameterized() method", attributes);
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
	@Target({ElementType.TYPE, ElementType.METHOD})
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

	@Transactional
	static interface InterfaceWithInheritedAnnotation {

		@Order
		void handleFromInterface();
	}

	static abstract class AbstractClassWithInheritedAnnotation<T> implements InterfaceWithInheritedAnnotation {

		@Transactional
		public abstract void handle();

		@Transactional
		public void handleParameterized(T t) {
		}
	}

	static class ConcreteClassWithInheritedAnnotation extends AbstractClassWithInheritedAnnotation<String> {

		@Override
		public void handle() {
		}

		@Override
		public void handleParameterized(String s) {
		}

		@Override
		public void handleFromInterface() {
		}
	}

	@Transactional
	public static interface InheritedAnnotationInterface {
	}

	public static interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
	}

	public static interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
	}

	@Order
	public static interface NonInheritedAnnotationInterface {
	}

	public static interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
	}

	public static interface SubSubNonInheritedAnnotationInterface extends SubNonInheritedAnnotationInterface {
	}

}
