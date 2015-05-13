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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import static java.util.Arrays.*;
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

	private static final String TX_NAME = Transactional.class.getName();

	private Set<String> names(Class<?>... classes) {
		return stream(classes).map(clazz -> clazz.getName()).collect(Collectors.toSet());
	}

	@Test
	public void getMetaAnnotationTypesOnNonAnnotatedClass() {
		assertNull(getMetaAnnotationTypes(NonAnnotatedClass.class, TransactionalComponent.class));
	}

	@Test
	public void getMetaAnnotationTypesOnClassWithMetaDepth1() {
		Set<String> names = getMetaAnnotationTypes(TransactionalComponentClass.class, TransactionalComponent.class);
		assertEquals(names(Transactional.class, Component.class), names);
	}

	@Test
	public void getMetaAnnotationTypesOnClassWithMetaDepth2() {
		Set<String> names = getMetaAnnotationTypes(ComposedTransactionalComponentClass.class,
			ComposedTransactionalComponent.class);
		assertEquals(names(TransactionalComponent.class, Transactional.class, Component.class), names);
	}

	@Test
	public void hasMetaAnnotationTypesOnNonAnnotatedClass() {
		assertFalse(hasMetaAnnotationTypes(NonAnnotatedClass.class, TX_NAME));
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth0() {
		assertFalse(hasMetaAnnotationTypes(TransactionalComponentClass.class, TransactionalComponent.class.getName()));
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth1() {
		assertTrue(hasMetaAnnotationTypes(TransactionalComponentClass.class, TX_NAME));
		assertTrue(hasMetaAnnotationTypes(TransactionalComponentClass.class, Component.class.getName()));
	}

	@Test
	public void hasMetaAnnotationTypesOnClassWithMetaDepth2() {
		assertTrue(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class, TX_NAME));
		assertTrue(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class, Component.class.getName()));
		assertFalse(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class,
			ComposedTransactionalComponent.class.getName()));
	}

	@Test
	public void isAnnotatedOnNonAnnotatedClass() {
		assertFalse(isAnnotated(NonAnnotatedClass.class, TX_NAME));
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth0() {
		assertTrue(isAnnotated(TransactionalComponentClass.class, TransactionalComponent.class.getName()));
	}

	@Test
	public void isAnnotatedOnSubclassWithMetaDepth0() {
		assertFalse("isAnnotated() does not search the class hierarchy.",
			isAnnotated(SubTransactionalComponentClass.class, TransactionalComponent.class.getName()));
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth1() {
		assertTrue(isAnnotated(TransactionalComponentClass.class, TX_NAME));
		assertTrue(isAnnotated(TransactionalComponentClass.class, Component.class.getName()));
	}

	@Test
	public void isAnnotatedOnClassWithMetaDepth2() {
		assertTrue(isAnnotated(ComposedTransactionalComponentClass.class, TX_NAME));
		assertTrue(isAnnotated(ComposedTransactionalComponentClass.class, Component.class.getName()));
		assertTrue(isAnnotated(ComposedTransactionalComponentClass.class,
			ComposedTransactionalComponent.class.getName()));
	}

	@Test
	public void getAllAnnotationAttributesOnNonAnnotatedClass() {
		assertNull(getAllAnnotationAttributes(NonAnnotatedClass.class, TX_NAME));
	}

	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotation() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxConfig.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on TxConfig", attributes);
		assertEquals("value for TxConfig.", asList("TxConfig"), attributes.get("value"));
	}

	@Test
	public void getAllAnnotationAttributesOnClassWithLocalComposedAnnotationAndInheritedAnnotation() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(SubClassWithInheritedAnnotation.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on SubClassWithInheritedAnnotation", attributes);
		assertEquals(asList("composed2", "transactionManager"), attributes.get("qualifier"));
	}

	@Test
	public void getAllAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(SubSubClassWithInheritedAnnotation.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on SubSubClassWithInheritedAnnotation", attributes);
		assertEquals(asList("transactionManager"), attributes.get("qualifier"));
	}

	@Test
	public void getAllAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes( SubSubClassWithInheritedComposedAnnotation.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on SubSubClassWithInheritedComposedAnnotation", attributes);
		assertEquals(asList("composed1"), attributes.get("qualifier"));
	}

	/**
	 * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then
	 * the algorithm is accidentally picking up shadowed annotations of the same
	 * type within the class hierarchy. Such undesirable behavior would cause the
	 * logic in {@link org.springframework.context.annotation.ProfileCondition}
	 * to fail.
	 *
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests#mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(DerivedTxConfig.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on DerivedTxConfig", attributes);
		assertEquals("value for DerivedTxConfig.", asList("DerivedTxConfig"), attributes.get("value"));
	}

	/**
	 * Note: this functionality is required by {@link org.springframework.context.annotation.ProfileCondition}.
	 *
	 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests
	 */
	@Test
	public void getAllAnnotationAttributesOnClassWithMultipleComposedAnnotations() {
		MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxFromMultipleComposedAnnotations.class, TX_NAME);
		assertNotNull("Annotation attributes map for @Transactional on TxFromMultipleComposedAnnotations", attributes);
		assertEquals("value for TxFromMultipleComposedAnnotations.", asList("TxComposed1", "TxComposed2"), attributes.get("value"));
	}

	@Test
	public void getAnnotationAttributesOnClassWithLocalAnnotation() {
		Class<?> element = TxConfig.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("Annotation attributes for @Transactional on TxConfig", attributes);
		assertEquals("value for TxConfig.", "TxConfig", attributes.getString("value"));
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
		Class<?> element = DerivedTxConfig.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("Annotation attributes for @Transactional on DerivedTxConfig", attributes);
		assertEquals("value for DerivedTxConfig.", "DerivedTxConfig", attributes.getString("value"));
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		AnnotationAttributes attributes = getAnnotationAttributes(MetaCycleAnnotatedClass.class, TX_NAME);
		assertNull("Should not find annotation attributes for @Transactional on MetaCycleAnnotatedClass", attributes);
	}

	@Test
	public void getAnnotationAttributesFavorsLocalComposedAnnotationOverInheritedAnnotation() {
		Class<?> element = SubClassWithInheritedAnnotation.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("AnnotationAttributes for @Transactional on SubClassWithInheritedAnnotation", attributes);
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
		assertTrue("readOnly flag for SubClassWithInheritedAnnotation.", attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		Class<?> element = SubSubClassWithInheritedAnnotation.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("AnnotationAttributes for @Transactional on SubSubClassWithInheritedAnnotation", attributes);
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
		assertFalse("readOnly flag for SubSubClassWithInheritedAnnotation.", attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		Class<?> element = SubSubClassWithInheritedComposedAnnotation.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("AnnotationAttributtes for @Transactional on SubSubClassWithInheritedComposedAnnotation.", attributes);
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
		assertFalse("readOnly flag for SubSubClassWithInheritedComposedAnnotation.", attributes.getBoolean("readOnly"));
	}

	@Test
	public void getAnnotationAttributesFromInterfaceImplementedBySuperclass() {
		Class<?> element = ConcreteClassWithInheritedAnnotation.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNull("Should not find @Transactional on ConcreteClassWithInheritedAnnotation", attributes);
		// Verify contracts between utility methods:
		assertFalse(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesOnInheritedAnnotationInterface() {
		Class<?> element = InheritedAnnotationInterface.class;
		String name = TX_NAME;
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("Should find @Transactional on InheritedAnnotationInterface", attributes);
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesOnNonInheritedAnnotationInterface() {
		Class<?> element = NonInheritedAnnotationInterface.class;
		String name = Order.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);
		assertNotNull("Should find @Order on NonInheritedAnnotationInterface", attributes);
		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void findAnnotationAttributesOnInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(InheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on InheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesOnSubInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(SubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubInheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(SubSubInheritedAnnotationInterface.class, Transactional.class);
		assertNotNull("Should find @Transactional on SubSubInheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesOnNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(NonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on NonInheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(SubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubNonInheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
		AnnotationAttributes attributes = findAnnotationAttributes(SubSubNonInheritedAnnotationInterface.class, Order.class);
		assertNotNull("Should find @Order on SubSubNonInheritedAnnotationInterface", attributes);
	}

	@Test
	public void findAnnotationAttributesInheritedFromInterfaceMethod() throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleFromInterface");
		AnnotationAttributes attributes = findAnnotationAttributes(method, Order.class);
		assertNotNull("Should find @Order on ConcreteClassWithInheritedAnnotation.handleFromInterface() method", attributes);
	}

	@Test
	public void findAnnotationAttributesInheritedFromAbstractMethod() throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
		AnnotationAttributes attributes = findAnnotationAttributes(method, Transactional.class);
		assertNotNull("Should find @Transactional on ConcreteClassWithInheritedAnnotation.handle() method", attributes);
	}

	/**
	 * <p>{@code AbstractClassWithInheritedAnnotation} declares {@code handleParameterized(T)}; whereas,
	 * {@code ConcreteClassWithInheritedAnnotation} declares {@code handleParameterized(String)}.
	 *
	 * <p>As of Spring 4.2 RC1, {@code AnnotatedElementUtils.processWithFindSemantics()} does not resolve an
	 * <em>equivalent</em> method in {@code AbstractClassWithInheritedAnnotation} for the <em>bridged</em>
	 * {@code handleParameterized(String)} method.
	 *
	 * @since 4.2
	 */
	@Test
	public void findAnnotationAttributesInheritedFromBridgedMethod() throws NoSuchMethodException {
		Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleParameterized", String.class);
		AnnotationAttributes attributes = findAnnotationAttributes(method, Transactional.class);
		assertNull("Should not find @Transactional on bridged ConcreteClassWithInheritedAnnotation.handleParameterized()", attributes);
	}

	/**
	 * Bridge/bridged method setup code copied from
	 * {@link org.springframework.core.BridgeMethodResolverTests#testWithGenericParameter()}.
	 * @since 4.2
	 */
	@Test
	public void findAnnotationAttributesFromBridgeMethod() throws NoSuchMethodException {
		Method[] methods = StringGenericParameter.class.getMethods();
		Method bridgeMethod = null;
		Method bridgedMethod = null;
		for (Method method : methods) {
			if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
				if (method.getReturnType().equals(Object.class)) {
					bridgeMethod = method;
				}
				else {
					bridgedMethod = method;
				}
			}
		}
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		assertTrue(bridgedMethod != null && !bridgedMethod.isBridge());

		AnnotationAttributes attributes = findAnnotationAttributes(bridgeMethod, Order.class);
		assertNotNull("Should find @Order on StringGenericParameter.getFor() bridge method", attributes);
	}

	@Test
	public void findAnnotationAttributesOnClassWithMetaAndLocalTxConfig() {
		AnnotationAttributes attributes = findAnnotationAttributes(MetaAndLocalTxConfigClass.class, Transactional.class);
		assertNotNull("Should find @Transactional on MetaAndLocalTxConfigClass", attributes);
		assertEquals("TX qualifier for MetaAndLocalTxConfigClass.", "localTxMgr", attributes.getString("qualifier"));
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
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Documented
	@Inherited
	@interface Transactional {

		String value() default "";

		String qualifier() default "transactionManager";

		boolean readOnly() default false;
	}

	@Transactional(qualifier = "composed1")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@Inherited
	@interface Composed1 {
	}

	@Transactional(qualifier = "composed2", readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface Composed2 {
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposedWithOverride {

		String qualifier() default "txMgr";
	}

	@Transactional("TxComposed1")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed1 {
	}

	@Transactional("TxComposed2")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed2 {
	}

	@Transactional
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@interface TransactionalComponent {
	}

	@TransactionalComponent
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedTransactionalComponent {
	}

	@TxComposedWithOverride
	// Override default "txMgr" from @TxComposedWithOverride with "localTxMgr"
	@Transactional(qualifier = "localTxMgr")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MetaAndLocalTxConfig {
	}

	// -------------------------------------------------------------------------

	static class NonAnnotatedClass {
	}

	@TransactionalComponent
	static class TransactionalComponentClass {
	}

	static class SubTransactionalComponentClass extends TransactionalComponentClass {
	}

	@ComposedTransactionalComponent
	static class ComposedTransactionalComponentClass {
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

	@MetaAndLocalTxConfig
	static class MetaAndLocalTxConfigClass {
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

	public static interface GenericParameter<T> {

		T getFor(Class<T> cls);
	}

	@SuppressWarnings("unused")
	private static class StringGenericParameter implements GenericParameter<String> {

		@Order
		@Override
		public String getFor(Class<String> cls) {
			return "foo";
		}

		public String getFor(Integer integer) {
			return "foo";
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
