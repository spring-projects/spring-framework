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

package org.springframework.test.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.test.util.MetaAnnotationUtils.UntypedAnnotationDescriptor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptor;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptorForTypes;

/**
 * Unit tests for {@link MetaAnnotationUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see OverriddenMetaAnnotationAttributesTests
 */
class MetaAnnotationUtilsTests {

	private void assertAtComponentOnComposedAnnotation(
			Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

		assertAtComponentOnComposedAnnotation(rootDeclaringClass, rootDeclaringClass, name, composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(
			Class<?> startClass, Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

		assertAtComponentOnComposedAnnotation(startClass, rootDeclaringClass, composedAnnotationType, name, composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(Class<?> startClass, Class<?> rootDeclaringClass,
			Class<?> declaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(startClass, Component.class);
		assertThat(descriptor).as("AnnotationDescriptor should not be null").isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(declaringClass);
		assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(Component.class);
		assertThat(descriptor.getAnnotation().value()).as("component name").isEqualTo(name);
		assertThat(descriptor.getComposedAnnotation()).as("composedAnnotation should not be null").isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).as("composedAnnotationType").isEqualTo(composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
			Class<?> startClass, String name, Class<? extends Annotation> composedAnnotationType) {

		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				startClass, startClass, name, composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
			Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				startClass, rootDeclaringClass, composedAnnotationType, name, composedAnnotationType);
	}

	@SuppressWarnings("unchecked")
	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
			Class<?> rootDeclaringClass, Class<?> declaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {

		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				startClass, Service.class, annotationType, Order.class, Transactional.class);

		assertThat(descriptor).as("UntypedAnnotationDescriptor should not be null").isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(rootDeclaringClass);
		assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(declaringClass);
		assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(annotationType);
		assertThat(((Component) descriptor.getAnnotation()).value()).as("component name").isEqualTo(name);
		assertThat(descriptor.getComposedAnnotation()).as("composedAnnotation should not be null").isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).as("composedAnnotationType").isEqualTo(composedAnnotationType);
	}

	@Test
	void findAnnotationDescriptorWithNoAnnotationPresent() {
		assertThat(findAnnotationDescriptor(NonAnnotatedInterface.class, Transactional.class)).isNull();
		assertThat(findAnnotationDescriptor(NonAnnotatedClass.class, Transactional.class)).isNull();
	}

	@Test
	void findAnnotationDescriptorWithInheritedAnnotationOnClass() {
		// Note: @Transactional is inherited
		assertThat(findAnnotationDescriptor(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
		assertThat(findAnnotationDescriptor(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
	}

	@Test
	void findAnnotationDescriptorWithInheritedAnnotationOnInterface() {
		// Note: @Transactional is inherited
		Transactional rawAnnotation = InheritedAnnotationInterface.class.getAnnotation(Transactional.class);

		AnnotationDescriptor<Transactional> descriptor =
				findAnnotationDescriptor(InheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptor(SubInheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptor(SubSubInheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubSubInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
	}

	@Test
	void findAnnotationDescriptorForNonInheritedAnnotationOnClass() {
		// Note: @Order is not inherited.
		assertThat(findAnnotationDescriptor(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(findAnnotationDescriptor(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
	}

	@Test
	void findAnnotationDescriptorForNonInheritedAnnotationOnInterface() {
		// Note: @Order is not inherited.
		Order rawAnnotation = NonInheritedAnnotationInterface.class.getAnnotation(Order.class);

		AnnotationDescriptor<Order> descriptor =
				findAnnotationDescriptor(NonInheritedAnnotationInterface.class, Order.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class, Order.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubNonInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
	}

	@Test
	void findAnnotationDescriptorWithMetaComponentAnnotation() {
		assertAtComponentOnComposedAnnotation(HasMetaComponentAnnotation.class, "meta1", Meta1.class);
	}

	@Test
	void findAnnotationDescriptorWithLocalAndMetaComponentAnnotation() {
		Class<Component> annotationType = Component.class;
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
				HasLocalAndMetaComponentAnnotation.class, annotationType);

		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(HasLocalAndMetaComponentAnnotation.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
		assertThat(descriptor.getComposedAnnotation()).isNull();
		assertThat(descriptor.getComposedAnnotationType()).isNull();
	}

	@Test
	void findAnnotationDescriptorForInterfaceWithMetaAnnotation() {
		assertAtComponentOnComposedAnnotation(InterfaceWithMetaAnnotation.class, "meta1", Meta1.class);
	}

	@Test
	void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() {
		Component rawAnnotation = AnnotationUtils.findAnnotation(ClassWithMetaAnnotatedInterface.class, Component.class);
		AnnotationDescriptor<Component> descriptor =
				findAnnotationDescriptor(ClassWithMetaAnnotatedInterface.class, Component.class);

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(ClassWithMetaAnnotatedInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(Meta1.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
		assertThat(descriptor.getComposedAnnotation().annotationType()).isEqualTo(Meta1.class);
	}

	@Test
	void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndAnnotatedSuperclass() {
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(
				MetaAnnotatedAndSuperAnnotatedContextConfigClass.class, ContextConfiguration.class);

		assertThat(descriptor).as("AnnotationDescriptor should not be null").isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(MetaAnnotatedAndSuperAnnotatedContextConfigClass.class);
		assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(MetaConfig.class);
		assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(ContextConfiguration.class);
		assertThat(descriptor.getComposedAnnotation()).as("composedAnnotation should not be null").isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).as("composedAnnotationType").isEqualTo(MetaConfig.class);

		assertThat(descriptor.getAnnotationAttributes().getClassArray("classes")).as("configured classes").isEqualTo(new Class<?>[] {String.class});
	}

	@Test
	void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotation(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}

	@Test
	void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotation(SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorOnMetaMetaAnnotatedClass() {
		Class<MetaMetaAnnotatedClass> startClass = MetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2", MetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorOnMetaMetaMetaAnnotatedClass() {
		Class<MetaMetaMetaAnnotatedClass> startClass = MetaMetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2", MetaMetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
				InheritedAnnotationClass.class, Component.class);
		assertThat(descriptor).as("Should not find @Component on InheritedAnnotationClass").isNull();
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
				MetaCycleAnnotatedClass.class, Component.class);
		assertThat(descriptor).as("Should not find @Component on MetaCycleAnnotatedClass").isNull();
	}

	// -------------------------------------------------------------------------

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithNoAnnotationPresent() {
		assertThat(findAnnotationDescriptorForTypes(NonAnnotatedInterface.class, Transactional.class, Component.class)).isNull();
		assertThat(findAnnotationDescriptorForTypes(NonAnnotatedClass.class, Transactional.class, Order.class)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithInheritedAnnotationOnClass() {
		// Note: @Transactional is inherited
		assertThat(findAnnotationDescriptorForTypes(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
		assertThat(findAnnotationDescriptorForTypes(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithInheritedAnnotationOnInterface() {
		// Note: @Transactional is inherited
		Transactional rawAnnotation = InheritedAnnotationInterface.class.getAnnotation(Transactional.class);

		UntypedAnnotationDescriptor descriptor =
				findAnnotationDescriptorForTypes(InheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptorForTypes(SubInheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptorForTypes(SubSubInheritedAnnotationInterface.class, Transactional.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubSubInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnClass() {
		// Note: @Order is not inherited.
		assertThat(findAnnotationDescriptorForTypes(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
		assertThat(findAnnotationDescriptorForTypes(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnInterface() {
		// Note: @Order is not inherited.
		Order rawAnnotation = NonInheritedAnnotationInterface.class.getAnnotation(Order.class);

		UntypedAnnotationDescriptor descriptor =
				findAnnotationDescriptorForTypes(NonInheritedAnnotationInterface.class, Order.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

		descriptor = findAnnotationDescriptorForTypes(SubNonInheritedAnnotationInterface.class, Order.class);
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubNonInheritedAnnotationInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithLocalAndMetaComponentAnnotation() {
		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				HasLocalAndMetaComponentAnnotation.class, Transactional.class, annotationType, Order.class);
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(HasLocalAndMetaComponentAnnotation.class);
		assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
		assertThat(descriptor.getComposedAnnotation()).isNull();
		assertThat(descriptor.getComposedAnnotationType()).isNull();
	}

	@Test
	void findAnnotationDescriptorForTypesWithMetaComponentAnnotation() {
		Class<HasMetaComponentAnnotation> startClass = HasMetaComponentAnnotation.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithMetaAnnotationWithDefaultAttributes() {
		Class<?> startClass = MetaConfigWithDefaultAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass,
				Service.class, ContextConfiguration.class, Order.class, Transactional.class);

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(startClass);
		assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
		assertThat(((ContextConfiguration) descriptor.getAnnotation()).value()).isEqualTo(new Class<?>[] {});
		assertThat(descriptor.getAnnotationAttributes().getClassArray("classes")).isEqualTo(new Class<?>[] {MetaConfig.DevConfig.class, MetaConfig.ProductionConfig.class});
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaConfig.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesWithMetaAnnotationWithOverriddenAttributes() {
		Class<?> startClass = MetaConfigWithOverriddenAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				startClass, Service.class, ContextConfiguration.class, Order.class, Transactional.class);

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(startClass);
		assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
		assertThat(((ContextConfiguration) descriptor.getAnnotation()).value()).isEqualTo(new Class<?>[] {});
		assertThat(descriptor.getAnnotationAttributes().getClassArray("classes")).isEqualTo(new Class<?>[] {MetaAnnotationUtilsTests.class});
		assertThat(descriptor.getComposedAnnotation()).isNotNull();
		assertThat(descriptor.getComposedAnnotationType()).isEqualTo(MetaConfig.class);
	}

	@Test
	void findAnnotationDescriptorForTypesForInterfaceWithMetaAnnotation() {
		Class<InterfaceWithMetaAnnotation> startClass = InterfaceWithMetaAnnotation.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesForClassWithMetaAnnotatedInterface() {
		Component rawAnnotation = AnnotationUtils.findAnnotation(ClassWithMetaAnnotatedInterface.class, Component.class);

		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				ClassWithMetaAnnotatedInterface.class, Service.class, Component.class, Order.class, Transactional.class);

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getRootDeclaringClass()).isEqualTo(ClassWithMetaAnnotatedInterface.class);
		assertThat(descriptor.getDeclaringClass()).isEqualTo(Meta1.class);
		assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
		assertThat(descriptor.getComposedAnnotation().annotationType()).isEqualTo(Meta1.class);
	}

	@Test
	void findAnnotationDescriptorForTypesForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		Class<ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface> startClass = ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta2", Meta2.class);
	}

	@Test
	void findAnnotationDescriptorForTypesForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorForTypesOnMetaMetaAnnotatedClass() {
		Class<MetaMetaAnnotatedClass> startClass = MetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				startClass, startClass, Meta2.class, "meta2", MetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	void findAnnotationDescriptorForTypesOnMetaMetaMetaAnnotatedClass() {
		Class<MetaMetaMetaAnnotatedClass> startClass = MetaMetaMetaAnnotatedClass.class;
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				startClass, startClass, Meta2.class, "meta2", MetaMetaMeta.class);
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component,
		// @Service, or @Order, but it is annotated with @Transactional.
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				InheritedAnnotationClass.class, Service.class, Component.class, Order.class);
		assertThat(descriptor).as("Should not find @Component on InheritedAnnotationClass").isNull();
	}

	/**
	 * @since 4.0.3
	 */
	@Test
	@SuppressWarnings("unchecked")
	void findAnnotationDescriptorForTypesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
				MetaCycleAnnotatedClass.class, Service.class, Component.class, Order.class);
		assertThat(descriptor).as("Should not find @Component on MetaCycleAnnotatedClass").isNull();
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
	class MetaConfigWithDefaultAttributesTestCase {
	}

	@MetaConfig(classes = MetaAnnotationUtilsTests.class)
	class MetaConfigWithOverriddenAttributesTestCase {
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

	@ContextConfiguration(classes = Number.class)
	static class AnnotatedContextConfigClass {
	}

	@MetaConfig(classes = String.class)
	static class MetaAnnotatedAndSuperAnnotatedContextConfigClass extends AnnotatedContextConfigClass {
	}

}
