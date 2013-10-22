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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.test.context.MetaAnnotationUtils.AnnotationDescriptor;
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

	@Test
	public void findAnnotationDescriptorWithNoAnnotationPresent() throws Exception {
		assertNull(findAnnotationDescriptor(NonAnnotatedInterface.class, Transactional.class));
		assertNull(findAnnotationDescriptor(NonAnnotatedClass.class, Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorWithInheritedClassLevelAnnotation() throws Exception {
		// Note: @Transactional is inherited

		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(InheritedAnnotationInterface.class, Transactional.class).getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(SubInheritedAnnotationInterface.class, Transactional.class).getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(SubSubInheritedAnnotationInterface.class, Transactional.class).getDeclaringClass());

		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(InheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(SubInheritedAnnotationClass.class, Transactional.class).getDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorWithNonInheritedClassLevelAnnotation() throws Exception {
		// Note: @Order is not inherited, but findAnnotationDescriptor() should still find
		// it.

		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptor(NonInheritedAnnotationInterface.class, Order.class).getDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class, Order.class).getDeclaringClass());

		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(NonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(SubNonInheritedAnnotationClass.class, Order.class).getDeclaringClass());
	}

	@Test
	public void findAnnotationDescriptorWithMetaAnnotations() throws Exception {

		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(HasMetaComponentAnnotation.class,
			Component.class);
		assertEquals(HasMetaComponentAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals("meta1", descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(Meta1.class, descriptor.getStereotypeType());

		descriptor = findAnnotationDescriptor(HasLocalAndMetaComponentAnnotation.class, Component.class);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertNull(descriptor.getStereotype());
		assertNull(descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForInterfaceWithMetaAnnotation() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(InterfaceWithMetaAnnotation.class,
			Component.class);
		assertEquals(InterfaceWithMetaAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals("meta1", descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(Meta1.class, descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(ClassWithMetaAnnotatedInterface.class,
			Component.class);
		assertEquals(InterfaceWithMetaAnnotation.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals("meta1", descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(Meta1.class, descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, Component.class);
		assertEquals(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals("meta2", descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(Meta2.class, descriptor.getStereotypeType());
	}

	@Test
	public void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
			SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, Component.class);
		assertEquals(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, descriptor.getDeclaringClass());
		assertEquals(Component.class, descriptor.getAnnotationType());
		assertEquals("meta2", descriptor.getAnnotation().value());
		assertNotNull(descriptor.getStereotype());
		assertEquals(Meta2.class, descriptor.getStereotypeType());
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
