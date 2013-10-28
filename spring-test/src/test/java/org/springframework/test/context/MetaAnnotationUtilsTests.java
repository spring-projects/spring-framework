/*
 * Copyright 2002-2013 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
 */
public class MetaAnnotationUtilsTests {

	private void assertComponentOnStereotype(Class<?> startClass, Class<?> declaringClass, String name,
			Class<? extends Annotation> stereotypeType) {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(startClass, Component.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals(name, descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(stereotypeType, descriptor.getStereotypeType());
	}

	@SuppressWarnings("unchecked")
	private void assertComponentOnStereotypeForMultipleCandidateTypes(Class<?> startClass, Class<?> declaringClass,
			String name, Class<? extends Annotation> stereotypeType) {
		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass, Service.class,
			annotationType, Order.class, Transactional.class);
		assertNotNull(descriptor);
		assertEquals(declaringClass, descriptor.getDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertEquals(name, ((Component) descriptor.getAnnotation()).value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(stereotypeType, descriptor.getStereotypeType());
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
			findAnnotationDescriptor(InheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(SubInheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(InheritedAnnotationInterface.class, Transactional.class).getDeclaringClass());
		assertNull(findAnnotationDescriptor(SubInheritedAnnotationInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptor(SubSubInheritedAnnotationInterface.class, Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(NonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(SubNonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptor(NonInheritedAnnotationInterface.class, Order.class).getDeclaringClass());
		assertNull(findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class, Order.class));
	}

	@Test
	public void findAnnotationDescriptorWithMetaComponentAnnotation() throws Exception {
		Class<HasMetaComponentAnnotation> startClass = HasMetaComponentAnnotation.class;
		assertComponentOnStereotype(startClass, startClass, "meta1", Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Component> annotationType = Component.class;
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(HasLocalAndMetaComponentAnnotation.class,
			annotationType);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertNull(descriptor.getStereotype());
		assertNull(descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForInterfaceWithMetaAnnotation() {
		Class<InterfaceWithMetaAnnotation> startClass = InterfaceWithMetaAnnotation.class;
		assertComponentOnStereotype(startClass, startClass, "meta1", Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() {
		assertNull(findAnnotationDescriptor(ClassWithMetaAnnotatedInterface.class, Component.class));
	}

	@Test
	public void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		Class<ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface> startClass = ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class;
		assertComponentOnStereotype(startClass, startClass, "meta2", Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertComponentOnStereotype(SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
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
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(InheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
		assertEquals(
			InheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(SubInheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		assertEquals(
			InheritedAnnotationInterface.class,
			findAnnotationDescriptorForTypes(InheritedAnnotationInterface.class, Transactional.class).getDeclaringClass());
		assertNull(findAnnotationDescriptorForTypes(SubInheritedAnnotationInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptorForTypes(SubSubInheritedAnnotationInterface.class, Transactional.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(NonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptorForTypes(SubNonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptorForTypes(NonInheritedAnnotationInterface.class, Order.class).getDeclaringClass());
		assertNull(findAnnotationDescriptorForTypes(SubNonInheritedAnnotationInterface.class, Order.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Component> annotationType = Component.class;
		UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
			HasLocalAndMetaComponentAnnotation.class, Transactional.class, annotationType, Order.class);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertNull(descriptor.getStereotype());
		assertNull(descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForTypesWithMetaComponentAnnotation() throws Exception {
		Class<HasMetaComponentAnnotation> startClass = HasMetaComponentAnnotation.class;
		assertComponentOnStereotypeForMultipleCandidateTypes(startClass, startClass, "meta1", Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesForInterfaceWithMetaAnnotation() {
		Class<InterfaceWithMetaAnnotation> startClass = InterfaceWithMetaAnnotation.class;
		assertComponentOnStereotypeForMultipleCandidateTypes(startClass, startClass, "meta1", Meta1.class);
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
		assertComponentOnStereotypeForMultipleCandidateTypes(startClass, startClass, "meta2", Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		assertComponentOnStereotypeForMultipleCandidateTypes(
			SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
	}


	// -------------------------------------------------------------------------

	@Component(value = "meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Meta1 {
	}

	@Component(value = "meta2")
	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	static @interface Meta2 {
	}

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
