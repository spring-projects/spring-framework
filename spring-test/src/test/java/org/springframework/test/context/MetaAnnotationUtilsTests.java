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

package org.springframework.test.context;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.test.context.MetaAnnotationUtils.UntypedAnnotationDescriptor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.springframework.test.context.MetaAnnotationUtils.*;

/**
 * Unit tests for {@link MetaAnnotationUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see OverriddenMetaAnnotationAttributesTests
 */
public class MetaAnnotationUtilsTests {

	private void assertAtComponentOnComposedAnnotation(Class<?> rootDeclaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotation(rootDeclaringClass, rootDeclaringClass, name, composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(Class<?> startClass, Class<?> rootDeclaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotation(rootDeclaringClass, rootDeclaringClass, composedAnnotationType, name,
			composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(Class<?> startClass, Class<?> rootDeclaringClass,
			Class<?> declaringClass, String name, Class<? extends Annotation> composedAnnotationType) {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(startClass, Component.class);
		assertNotNull("AnnotationDescriptor should not be null", descriptor);
		assertEquals("rootDeclaringClass", rootDeclaringClass, descriptor.getRootDeclaringClass());
		assertEquals("declaringClass", declaringClass, descriptor.getDeclaringClass());
		assertEquals("annotationType", Component.class, descriptor.getAnnotationType());
		assertEquals("component name", name, descriptor.getAnnotation().value());
		assertNotNull("composedAnnotation should not be null", descriptor.getComposedAnnotation());
		assertEquals("composedAnnotationType", composedAnnotationType, descriptor.getComposedAnnotationType());
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, startClass, name,
			composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
			Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, rootDeclaringClass,
			composedAnnotationType, name, composedAnnotationType);
	}

	@SuppressWarnings("unchecked")
	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
			Class<?> rootDeclaringClass, Class<?> declaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass, Service.class,
			annotationType, Order.class, Transactional.class);
		assertNotNull("UntypedAnnotationDescriptor should not be null", descriptor);
		assertEquals("rootDeclaringClass", rootDeclaringClass, descriptor.getRootDeclaringClass());
		assertEquals("declaringClass", declaringClass, descriptor.getDeclaringClass());
		assertEquals("annotationType", annotationType, descriptor.getAnnotationType());
		assertEquals("component name", name, ((Component) descriptor.getAnnotation()).value());
		assertNotNull("composedAnnotation should not be null", descriptor.getComposedAnnotation());
		assertEquals("composedAnnotationType", composedAnnotationType, descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorWithNoAnnotationPresent() throws Exception {
		assertNull(findAnnotationDescriptor(NonAnnotatedInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptor(NonAnnotatedClass.class, Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorWithInheritedAnnotationOnClass() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass());
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(InheritedAnnotationInterface.class, Transactional.class).getRootDeclaringClass());
		assertNull(findAnnotationDescriptor(SubInheritedAnnotationInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptor(SubSubInheritedAnnotationInterface.class, Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptor(NonInheritedAnnotationInterface.class, Order.class).getRootDeclaringClass());
		assertNull(findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class, Order.class));
	}

	@Test
	public void findAnnotationDescriptorWithMetaComponentAnnotation() throws Exception {
		assertAtComponentOnComposedAnnotation(HasMetaComponentAnnotation.class, "meta1", Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Component> annotationType = Component.class;
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(HasLocalAndMetaComponentAnnotation.class,
			annotationType);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getRootDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertNull(descriptor.getComposedAnnotation());
		assertNull(descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForInterfaceWithMetaAnnotation() {
		assertAtComponentOnComposedAnnotation(InterfaceWithMetaAnnotation.class, "meta1", Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() {
		assertNull(findAnnotationDescriptor(ClassWithMetaAnnotatedInterface.class, Component.class));
	}

	@Test
	public void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotation(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2",
			Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotation(SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorOnMetaMetaAnnotatedClass() {
		Class<MetaMetaAnnotatedClass> startClass = MetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2", MetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorOnMetaMetaMetaAnnotatedClass() {
		Class<MetaMetaMetaAnnotatedClass> startClass = MetaMetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2", MetaMetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(InheritedAnnotationClass.class,
			Component.class);
		assertNull("Should not find @Component on InheritedAnnotationClass", descriptor);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(MetaCycleAnnotatedClass.class,
			Component.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", descriptor);
	}

	// -------------------------------------------------------------------------

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithNoAnnotationPresent() throws Exception {
		assertNull(findAnnotationDescriptorForTypes(NonAnnotatedInterface.class, Transactional.class, Component.class));
		assertNull(findAnnotationDescriptorForTypes(NonAnnotatedClass.class, Transactional.class, Order.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithInheritedAnnotationOnClass() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(
			InheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass());
		assertEquals(
			InheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(
			InheritedAnnotationInterface.class,
			findAnnotationDescriptorForTypes(InheritedAnnotationInterface.class, Transactional.class).getRootDeclaringClass());
		assertNull(findAnnotationDescriptorForTypes(SubInheritedAnnotationInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptorForTypes(SubSubInheritedAnnotationInterface.class, Transactional.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(
			NonInheritedAnnotationInterface.class,
			findAnnotationDescriptorForTypes(NonInheritedAnnotationInterface.class, Order.class).getRootDeclaringClass());
		assertNull(findAnnotationDescriptorForTypes(SubNonInheritedAnnotationInterface.class, Order.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
			HasLocalAndMetaComponentAnnotation.class, Transactional.class, annotationType, Order.class);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getRootDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertNull(descriptor.getComposedAnnotation());
		assertNull(descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForTypesWithMetaComponentAnnotation() throws Exception {
		Class<HasMetaComponentAnnotation> startClass = HasMetaComponentAnnotation.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithMetaAnnotationWithDefaultAttributes() throws Exception {
		Class<?> startClass = MetaConfigWithDefaultAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass, Service.class,
			ContextConfiguration.class, Order.class, Transactional.class);

		assertNotNull(descriptor);
		assertEquals(startClass, descriptor.getRootDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertArrayEquals(new Class[] {}, ((ContextConfiguration) descriptor.getAnnotation()).value());
		assertArrayEquals(new Class[] { MetaConfig.DevConfig.class, MetaConfig.ProductionConfig.class },
			descriptor.getAnnotationAttributes().getClassArray("classes"));
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaConfig.class, descriptor.getComposedAnnotationType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithMetaAnnotationWithOverriddenAttributes() throws Exception {
		Class<?> startClass = MetaConfigWithOverriddenAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass, Service.class,
			ContextConfiguration.class, Order.class, Transactional.class);

		assertNotNull(descriptor);
		assertEquals(startClass, descriptor.getRootDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertArrayEquals(new Class[] {}, ((ContextConfiguration) descriptor.getAnnotation()).value());
		assertArrayEquals(new Class[] { MetaAnnotationUtilsTests.class },
			descriptor.getAnnotationAttributes().getClassArray("classes"));
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaConfig.class, descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForTypesForInterfaceWithMetaAnnotation() {
		Class<InterfaceWithMetaAnnotation> startClass = InterfaceWithMetaAnnotation.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForClassWithMetaAnnotatedInterface() {
		assertNull(findAnnotationDescriptorForTypes(ClassWithMetaAnnotatedInterface.class, Service.class,
			Component.class, Order.class, Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorForTypesForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		Class<ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface> startClass = ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta2", Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
			SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorForTypesOnMetaMetaAnnotatedClass() {
		Class<MetaMetaAnnotatedClass> startClass = MetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, startClass, Meta2.class, "meta2",
			MetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	public void findAnnotationDescriptorForTypesOnMetaMetaMetaAnnotatedClass() {
		Class<MetaMetaMetaAnnotatedClass> startClass = MetaMetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, startClass, Meta2.class, "meta2",
			MetaMetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component,
		// @Service, or @Order, but it is annotated with @Transactional.
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(InheritedAnnotationClass.class,
			Service.class, Component.class, Order.class);
		assertNull("Should not find @Component on InheritedAnnotationClass", descriptor);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(MetaCycleAnnotatedClass.class,
			Service.class, Component.class, Order.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", descriptor);
	}


	// -------------------------------------------------------------------------

	@Component(value = "meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	static @interface Meta1 {
	}

	@Component(value = "meta2")
	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	static @interface Meta2 {
	}

	@Meta2
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface MetaMeta {
	}

	@MetaMeta
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface MetaMetaMeta {
	}

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

	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	static @interface MetaConfig {

		static class DevConfig {
		}

		static class ProductionConfig {
		}


		Class<?>[] classes() default { DevConfig.class, ProductionConfig.class };
	}

	// -------------------------------------------------------------------------

	@Meta1
	static class HasMetaComponentAnnotation {
	}

	@Meta1
	@Component(value = "local")
	@Meta2
	static class HasLocalAndMetaComponentAnnotation {
	}

	@Meta1
	static interface InterfaceWithMetaAnnotation {
	}

	static class ClassWithMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
	}

	@Meta2
	static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
	}

	static class SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface extends
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface {
	}

	@MetaMeta
	static class MetaMetaAnnotatedClass {
	}

	@MetaMetaMeta
	static class MetaMetaMetaAnnotatedClass {
	}

	@MetaCycle3
	static class MetaCycleAnnotatedClass {
	}

	@MetaConfig
	public class MetaConfigWithDefaultAttributesTestCase {
	}

	@MetaConfig(classes = MetaAnnotationUtilsTests.class)
	public class MetaConfigWithOverriddenAttributesTestCase {
	}

	// -------------------------------------------------------------------------

	@Transactional
	static interface InheritedAnnotationInterface {
	}

	static interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
	}

	static interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
	}

	@Order
	static interface NonInheritedAnnotationInterface {
	}

	static interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
	}

	static class NonAnnotatedClass {
	}

	static interface NonAnnotatedInterface {
	}

	@Transactional
	static class InheritedAnnotationClass {
	}

	static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
	}

	@Order
	static class NonInheritedAnnotationClass {
	}

	static class SubNonInheritedAnnotationClass extends NonInheritedAnnotationClass {
	}

}
