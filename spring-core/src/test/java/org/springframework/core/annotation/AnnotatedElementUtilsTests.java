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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.Matchers.*;
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

	@Rule
	public final ExpectedException exception = ExpectedException.none();

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
		assertEquals("value for TxFromMultipleComposedAnnotations.", asList("TxInheritedComposed", "TxComposed"),
			attributes.get("value"));
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
	public void getAnnotationAttributesWithConventionBasedComposedAnnotation() {
		Class<?> element = ConventionBasedComposedContextConfigClass.class;
		String name = ContextConfig.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);

		assertNotNull("Should find @ContextConfig on " + element.getSimpleName(), attributes);
		assertArrayEquals("locations", new String[] { "explicitDeclaration" }, attributes.getStringArray("locations"));
		assertArrayEquals("value", new String[] { "explicitDeclaration" }, attributes.getStringArray("value"));

		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesWithAliasedComposedAnnotation() {
		Class<?> element = AliasedComposedContextConfigClass.class;
		String name = ContextConfig.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);

		assertNotNull("Should find @ContextConfig on " + element.getSimpleName(), attributes);
		assertArrayEquals("value", new String[] { "test.xml" }, attributes.getStringArray("value"));
		assertArrayEquals("locations", new String[] { "test.xml" }, attributes.getStringArray("locations"));

		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationAttributesWithAliasedValueComposedAnnotation() {
		Class<?> element = AliasedValueComposedContextConfigClass.class;
		String name = ContextConfig.class.getName();
		AnnotationAttributes attributes = getAnnotationAttributes(element, name);

		assertNotNull("Should find @ContextConfig on " + element.getSimpleName(), attributes);
		assertArrayEquals("locations", new String[] { "test.xml" }, attributes.getStringArray("locations"));
		assertArrayEquals("value", new String[] { "test.xml" }, attributes.getStringArray("value"));

		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, name));
	}

	@Test
	public void getAnnotationWithAliasedValueComposedAnnotation() {
		Class<?> element = AliasedValueComposedContextConfigClass.class;
		ContextConfig contextConfig = getAnnotation(element, ContextConfig.class);

		assertNotNull("Should find @ContextConfig on " + element.getSimpleName(), contextConfig);
		assertArrayEquals("locations", new String[] { "test.xml" }, contextConfig.locations());
		assertArrayEquals("value", new String[] { "test.xml" }, contextConfig.value());

		// Verify contracts between utility methods:
		assertTrue(isAnnotated(element, ContextConfig.class.getName()));
	}

	@Test
	public void getAnnotationAttributesWithInvalidConventionBasedComposedAnnotation() {
		Class<?> element = InvalidConventionBasedComposedContextConfigClass.class;
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(either(containsString("attribute [value] and its alias [locations]")).or(
			containsString("attribute [locations] and its alias [value]")));
		exception.expectMessage(either(
			containsString("values of [{duplicateDeclaration}] and [{requiredLocationsDeclaration}]")).or(
			containsString("values of [{requiredLocationsDeclaration}] and [{duplicateDeclaration}]")));
		exception.expectMessage(containsString("but only one declaration is permitted"));
		getAnnotationAttributes(element, ContextConfig.class);
	}

	@Test
	public void getAnnotationAttributesWithInvalidAliasedComposedAnnotation() {
		Class<?> element = InvalidAliasedComposedContextConfigClass.class;
		exception.expect(AnnotationConfigurationException.class);
		exception.expectMessage(either(containsString("attribute [value] and its alias [locations]")).or(
			containsString("attribute [locations] and its alias [value]")));
		exception.expectMessage(either(containsString("values of [{duplicateDeclaration}] and [{test.xml}]")).or(
			containsString("values of [{test.xml}] and [{duplicateDeclaration}]")));
		exception.expectMessage(containsString("but only one declaration is permitted"));
		getAnnotationAttributes(element, ContextConfig.class);
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

	@Test
	public void findAndSynthesizeAnnotationAttributesOnClassWithAttributeAliasesInTargetAnnotation() {
		String qualifier = "aliasForQualifier";

		// 1) Find and merge AnnotationAttributes from the annotation hierarchy
		AnnotationAttributes attributes = findAnnotationAttributes(AliasedTransactionalComponentClass.class,
			AliasedTransactional.class);
		assertNotNull("@AliasedTransactional on AliasedTransactionalComponentClass.", attributes);

		// 2) Synthesize the AnnotationAttributes back into the target annotation
		AliasedTransactional annotation = AnnotationUtils.synthesizeAnnotation(attributes,
			AliasedTransactional.class, AliasedTransactionalComponentClass.class);
		assertNotNull(annotation);

		// 3) Verify that the AnnotationAttributes and synthesized annotation are equivalent
		assertEquals("TX value via attributes.", qualifier, attributes.getString("value"));
		assertEquals("TX value via synthesized annotation.", qualifier, annotation.value());
		assertEquals("TX qualifier via attributes.", qualifier, attributes.getString("qualifier"));
		assertEquals("TX qualifier via synthesized annotation.", qualifier, annotation.qualifier());
	}

	@Test
	public void findAnnotationWithAttributeAliasesInTargetAnnotation() {
		Class<?> element = AliasedTransactionalComponentClass.class;
		AliasedTransactional annotation = findAnnotation(element, AliasedTransactional.class);
		assertNotNull("@AliasedTransactional on " + element, annotation);
		assertEquals("TX value via synthesized annotation.", "aliasForQualifier", annotation.value());
		assertEquals("TX qualifier via synthesized annotation.", "aliasForQualifier", annotation.qualifier());
	}

	@Test
	public void findAnnotationAttributesOnClassWithAttributeAliasInComposedAnnotationAndNestedAnnotationsInTargetAnnotation() {
		Class<?> element = TestComponentScanClass.class;
		AnnotationAttributes attributes = findAnnotationAttributes(element, ComponentScan.class);
		assertNotNull("Should find @ComponentScan on " + element, attributes);
		assertArrayEquals("basePackages for " + element, new String[] { "com.example.app.test" },
			attributes.getStringArray("basePackages"));

		Filter[] excludeFilters = attributes.getAnnotationArray("excludeFilters", Filter.class);
		assertNotNull(excludeFilters);

		List<String> patterns = stream(excludeFilters).map(Filter::pattern).collect(toList());
		assertEquals(asList("*Test", "*Tests"), patterns);
	}

	private Set<String> names(Class<?>... classes) {
		return stream(classes).map(Class::getName).collect(toSet());
	}


	static AnnotationAttributes findAnnotationAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "annotationType must not be null");
		return AnnotatedElementUtils.findAnnotationAttributes(element, annotationType.getName(), false, false);
	}


	// -------------------------------------------------------------------------

	@MetaCycle3
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface MetaCycle1 {
	}

	@MetaCycle1
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface MetaCycle2 {
	}

	@MetaCycle2
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MetaCycle3 {
	}

	@MetaCycle3
	static class MetaCycleAnnotatedClass {
	}

	// -------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Inherited
	@interface Transactional {

		String value() default "";

		String qualifier() default "transactionManager";

		boolean readOnly() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Inherited
	@interface AliasedTransactional {

		@AliasFor(attribute = "qualifier")
		String value() default "";

		@AliasFor(attribute = "value")
		String qualifier() default "";
	}

	@Transactional(qualifier = "composed1")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	@interface InheritedComposed {
	}

	@Transactional(qualifier = "composed2", readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Composed {
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposedWithOverride {

		String qualifier() default "txMgr";
	}

	@Transactional("TxInheritedComposed")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxInheritedComposed {
	}

	@Transactional("TxComposed")
	@Retention(RetentionPolicy.RUNTIME)
	@interface TxComposed {
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

	@AliasedTransactional(value = "aliasForQualifier")
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedTransactionalComponent {
	}

	@TxComposedWithOverride
	// Override default "txMgr" from @TxComposedWithOverride with "localTxMgr"
	@Transactional(qualifier = "localTxMgr")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MetaAndLocalTxConfig {
	}

	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfig {

		@AliasFor(attribute = "locations")
		String[] value() default {};

		@AliasFor(attribute = "value")
		String[] locations() default {};
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface ConventionBasedComposedContextConfig {

		String[] locations() default {};
	}

	@ContextConfig(value = "duplicateDeclaration")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidConventionBasedComposedContextConfig {

		String[] locations();
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles();
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasedValueComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "value")
		String[] locations();
	}

	/**
	 * Invalid because the configuration declares a value for 'value' and
	 * requires a value for the aliased 'locations'. So we likely end up with
	 * both 'value' and 'locations' being present in {@link AnnotationAttributes}
	 * but with different values, which violates the contract of {@code @AliasFor}.
	 */
	@ContextConfig(value = "duplicateDeclaration")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InvalidAliasedComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "locations")
		String[] xmlConfigFiles();
	}

	/**
	 * Mock of {@code org.springframework.context.annotation.ComponentScan}
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComponentScan {

		String[] basePackages() default {};

		Filter[] excludeFilters() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		String pattern();
	}

	@ComponentScan(excludeFilters = { @Filter(pattern = "*Test"), @Filter(pattern = "*Tests") })
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestComponentScan {

		@AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
		String[] packages();
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

	@AliasedTransactionalComponent
	static class AliasedTransactionalComponentClass {
	}

	@Transactional
	static class ClassWithInheritedAnnotation {
	}

	@Composed
	static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class SubSubClassWithInheritedAnnotation extends SubClassWithInheritedAnnotation {
	}

	@InheritedComposed
	static class ClassWithInheritedComposedAnnotation {
	}

	@Composed
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

	@TxInheritedComposed
	@TxComposed
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

	@ConventionBasedComposedContextConfig(locations = "explicitDeclaration")
	static class ConventionBasedComposedContextConfigClass {
	}

	@InvalidConventionBasedComposedContextConfig(locations = "requiredLocationsDeclaration")
	static class InvalidConventionBasedComposedContextConfigClass {
	}

	@AliasedComposedContextConfig(xmlConfigFiles = "test.xml")
	static class AliasedComposedContextConfigClass {
	}

	@AliasedValueComposedContextConfig(locations = "test.xml")
	static class AliasedValueComposedContextConfigClass {
	}

	@InvalidAliasedComposedContextConfig(xmlConfigFiles = "test.xml")
	static class InvalidAliasedComposedContextConfigClass {
	}

	@TestComponentScan(packages = "com.example.app.test")
	static class TestComponentScanClass {
	}

}
