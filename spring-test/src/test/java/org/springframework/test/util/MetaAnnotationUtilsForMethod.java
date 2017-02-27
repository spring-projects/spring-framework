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

package org.springframework.test.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptor;
import static org.springframework.test.util.MetaAnnotationUtils.findAnnotationDescriptorForTypes;

/**
 * Unit tests for {@link MetaAnnotationUtils} for method-level annotations handling methods.
 *
 * @author Sergei Ustimenko
 * @since 5.0
 * @see MetaAnnotationUtilsTests
 */
public class MetaAnnotationUtilsForMethod {

	private void assertAtComponentOnComposedAnnotation(Method method, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotation(method, method.getDeclaringClass(), name, composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(Method method, Class<?> rootDeclaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotation(method, rootDeclaringClass, composedAnnotationType, name,
				composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotation(Method method, Class<?> rootDeclaringClass,
			Class<?> declaringClass, String name, Class<? extends Annotation> composedAnnotationType) {
		MetaAnnotationUtils.AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(method,
				Component.class);
		assertNotNull("AnnotationDescriptor should not be null", descriptor);
		assertEquals("rootDeclaringClass", rootDeclaringClass, descriptor.getRootDeclaringClass());
		assertEquals("declaringClass", declaringClass, descriptor.getDeclaringClass());
		assertEquals("annotationType", Component.class, descriptor.getAnnotationType());
		assertEquals("component name", name, descriptor.getAnnotation().value());
		assertNotNull("composedAnnotation should not be null", descriptor.getComposedAnnotation());
		assertEquals("composedAnnotationType", composedAnnotationType, descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorWithNoAnnotationPresent() throws Exception {
		assertNull(findAnnotationDescriptor(
				NonAnnotatedInterface.class.getMethod("something"), Transactional.class));
		assertNull(findAnnotationDescriptor(
				NonAnnotatedClass.class.getMethod("something"), Transactional.class));
	}

	@Test
	public void findAnnotationDescriptorWithInheritedAnnotationOnMethod() throws Exception {
		// Note: @Transactional is inherited
		Method inheritedAnnotationMethod = InheritedAnnotationClass.class.getMethod("something");

		assertEquals(InheritedAnnotationClass.class, findAnnotationDescriptor(
				inheritedAnnotationMethod,Transactional.class).getRootDeclaringClass());
		assertEquals(InheritedAnnotationClass.class, findAnnotationDescriptor(
				inheritedAnnotationMethod,Transactional.class).getDeclaringClass());
		assertEquals(inheritedAnnotationMethod, findAnnotationDescriptor(
				inheritedAnnotationMethod,Transactional.class).getDeclaringMethod());

		assertEquals(SubInheritedAnnotationClass.class, findAnnotationDescriptor(
				SubInheritedAnnotationClass.class.getMethod("something"), Transactional.class).getRootDeclaringClass());
		assertEquals(InheritedAnnotationClass.class, findAnnotationDescriptor(
				SubInheritedAnnotationClass.class.getMethod("something"), Transactional.class).getDeclaringClass());
		assertEquals(inheritedAnnotationMethod, findAnnotationDescriptor(
				SubInheritedAnnotationClass.class.getMethod("something"), Transactional.class).getDeclaringMethod());
	}

	@Test
	public void findAnnotationDescriptorWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		Method something = InheritedAnnotationInterface.class.getMethod("something");

		Transactional rawAnnotation = something.getAnnotation(Transactional.class);

		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Transactional> descriptor;

		descriptor = findAnnotationDescriptor(something, Transactional.class);
		assertNotNull(descriptor);
		assertEquals(InheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(something, descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		Method somethingInSubInherited = SubInheritedAnnotationInterface.class.getMethod("something");
		descriptor = findAnnotationDescriptor(somethingInSubInherited, Transactional.class);
		assertNotNull(descriptor);
		assertEquals(SubInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		Method somethingInSubSubInherited = SubSubInheritedAnnotationInterface.class.getMethod("something");
		descriptor = findAnnotationDescriptor(somethingInSubSubInherited, Transactional.class);
		assertNotNull(descriptor);
		assertEquals(SubSubInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Order> something = findAnnotationDescriptor(
				NonInheritedAnnotationClass.class.getMethod("something"), Order.class);
		assertEquals(NonInheritedAnnotationClass.class, something.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class.getMethod("something"), something.getDeclaringMethod());

		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Order> somethingOnSub = findAnnotationDescriptor(
				SubNonInheritedAnnotationClass.class.getMethod("something"), Order.class);
		assertEquals(SubNonInheritedAnnotationClass.class, somethingOnSub.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class.getMethod("something"), somethingOnSub.getDeclaringMethod());
	}

	@Test
	public void findAnnotationDescriptorForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		Order rawAnnotation = NonInheritedAnnotationInterface.class.getMethod("something").getAnnotation(Order.class);

		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Order> descriptor;

		descriptor = findAnnotationDescriptor(NonInheritedAnnotationInterface.class.getMethod("something"), Order.class);
		assertNotNull(descriptor);
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		descriptor = findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class.getMethod("something"), Order.class);
		assertNotNull(descriptor);
		assertEquals(SubNonInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
	}

	@Test
	public void findAnnotationDescriptorWithMetaComponentAnnotation() throws Exception {
		assertAtComponentOnComposedAnnotation(HasMetaComponentAnnotation.class.getDeclaredMethod("something"),
				"meta1", MetaAnnotationUtilsTests.Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Transactional> annotationType = Transactional.class;
		Method something = HasLocalAndMetaComponentAnnotation.class.getDeclaredMethod("something");
		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Transactional> descriptor = findAnnotationDescriptor(
				something, annotationType);

		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getRootDeclaringClass());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertEquals(something, descriptor.getDeclaringMethod());
		assertNull(descriptor.getComposedAnnotation());
		assertNull(descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForInterfaceWithMetaAnnotation() throws Exception  {
		assertAtComponentOnComposedAnnotation(InterfaceWithMetaAnnotation.class.getDeclaredMethod("something"),
				"meta1", MetaAnnotationUtilsTests.Meta1.class);
	}

	@Test
	public void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() throws Exception {
		Method something = ClassWithMetaAnnotatedInterface.class.getMethod("something");
		Component rawAnnotation = findAnnotation(something, Component.class);

		MetaAnnotationUtils.AnnotationOnMethodDescriptor<Component> descriptor;

		descriptor = findAnnotationDescriptor(something, Component.class);
		assertNotNull(descriptor);
		assertEquals(ClassWithMetaAnnotatedInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(MetaAnnotationUtilsTests.Meta1.class, descriptor.getDeclaringClass());
		assertEquals(InterfaceWithMetaAnnotation.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
		assertEquals(MetaAnnotationUtilsTests.Meta1.class, descriptor.getComposedAnnotation().annotationType());
	}

	@Test
	public void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndAnnotatedSuperclass() throws Exception {
		Method something = MetaAnnotatedAndSuperAnnotatedContextConfigClass.class.getDeclaredMethod("something");
		MetaAnnotationUtils.AnnotationOnMethodDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(
				something, ContextConfiguration.class);

		assertNotNull("AnnotationDescriptor should not be null", descriptor);
		assertEquals("rootDeclaringClass", MetaAnnotatedAndSuperAnnotatedContextConfigClass.class, descriptor.getRootDeclaringClass());
		assertEquals("declaringClass", MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getDeclaringClass());
		assertEquals("annotationType", ContextConfiguration.class, descriptor.getAnnotationType());
		assertEquals("method", something, descriptor.getDeclaringMethod());
		assertNotNull("composedAnnotation should not be null", descriptor.getComposedAnnotation());
		assertEquals("composedAnnotationType", MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getComposedAnnotationType());

		assertArrayEquals("configured classes", new Class[] { String.class },
				descriptor.getAnnotationAttributes().getClassArray("classes"));
	}

	@Test
	public void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() throws Exception {
		assertAtComponentOnComposedAnnotation(
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				"meta2", MetaAnnotationUtilsTests.Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() throws Exception {
		assertAtComponentOnComposedAnnotation(
				SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
				"meta2", MetaAnnotationUtilsTests.Meta2.class);
	}


	@Test
	public void findAnnotationDescriptorOnMetaMetaAnnotatedClass() throws Exception {
		Method something = MetaMetaAnnotatedClass.class.getDeclaredMethod("something");
		assertAtComponentOnComposedAnnotation(something, MetaMetaAnnotatedClass.class,
				MetaAnnotationUtilsTests.Meta2.class, "meta2", MetaAnnotationUtilsTests.MetaMeta.class);
	}

	@Test
	public void findAnnotationDescriptorOnMetaMetaMetaAnnotatedClass() throws Exception {
		Method something = MetaMetaMetaAnnotatedClass.class.getDeclaredMethod("something");
		assertAtComponentOnComposedAnnotation(something, MetaMetaMetaAnnotatedClass.class,
				MetaAnnotationUtilsTests.Meta2.class, "meta2", MetaAnnotationUtilsTests.MetaMetaMeta.class);
	}


	@Test
	public void findAnnotationDescriptorOnAnnotatedClassWithMissingTargetMetaAnnotation() throws Exception {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component
		MetaAnnotationUtils.AnnotationDescriptor<Component>
				descriptor = findAnnotationDescriptor(
						InheritedAnnotationClass.class.getDeclaredMethod("something"), Component.class);
		assertNull("Should not find @Component on InheritedAnnotationClass", descriptor);
	}

	@Test
	public void findAnnotationDescriptorOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() throws Exception {
		MetaAnnotationUtils.AnnotationDescriptor<Component>
				descriptor = findAnnotationDescriptor(
						MetaCycleAnnotatedClass.class.getDeclaredMethod("something"), Component.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", descriptor);
	}

	// -------------------------------------------------------------------------

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithNoAnnotationPresent() throws Exception {
		assertNull(findAnnotationDescriptorForTypes(NonAnnotatedInterface.class.getDeclaredMethod("something"),
				Transactional.class, Component.class));
		assertNull(findAnnotationDescriptorForTypes(NonAnnotatedClass.class.getDeclaredMethod("something"),
				Transactional.class, Order.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithInheritedAnnotationOnClass() throws Exception {
		// Note: @Transactional is inherited
		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor something = findAnnotationDescriptorForTypes(
				InheritedAnnotationClass.class.getDeclaredMethod("something"), Transactional.class);
		assertEquals(InheritedAnnotationClass.class, something.getRootDeclaringClass());
		assertEquals(InheritedAnnotationClass.class, something.getDeclaringClass());
		assertEquals(InheritedAnnotationClass.class.getDeclaredMethod("something"), something.getDeclaringMethod());

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor somethingOnSub = findAnnotationDescriptorForTypes(
				SubInheritedAnnotationClass.class.getDeclaredMethod("something"), Transactional.class);
		assertEquals(InheritedAnnotationClass.class, somethingOnSub.getDeclaringClass());
		assertEquals(SubInheritedAnnotationClass.class, somethingOnSub.getRootDeclaringClass());
		assertEquals(InheritedAnnotationClass.class.getDeclaredMethod("something"), somethingOnSub.getDeclaringMethod());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithInheritedAnnotationOnInterface() throws Exception {
		// Note: @Transactional is inherited
		Transactional rawAnnotation = InheritedAnnotationInterface.class.getDeclaredMethod("something")
				.getAnnotation(Transactional.class);

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor;

		descriptor = findAnnotationDescriptorForTypes(InheritedAnnotationInterface.class.getDeclaredMethod("something"),
				Transactional.class);
		assertNotNull(descriptor);
		assertEquals(InheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		descriptor = findAnnotationDescriptorForTypes(SubInheritedAnnotationInterface.class.getDeclaredMethod("something"),
				Transactional.class);
		assertNotNull(descriptor);
		assertEquals(SubInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		descriptor = findAnnotationDescriptorForTypes(SubSubInheritedAnnotationInterface.class.getDeclaredMethod("something"),
				Transactional.class);
		assertNotNull(descriptor);
		assertEquals(SubSubInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(InheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnClass() throws Exception {
		// Note: @Order is not inherited.
		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor something = findAnnotationDescriptorForTypes(
				NonInheritedAnnotationClass.class.getDeclaredMethod("something"), Order.class);
		assertEquals(NonInheritedAnnotationClass.class, something.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class, something.getDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class.getDeclaredMethod("something"), something.getDeclaringMethod());


		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor somethingOnSub = findAnnotationDescriptorForTypes(
				SubNonInheritedAnnotationClass.class.getDeclaredMethod("something"), Order.class);
		assertEquals(SubNonInheritedAnnotationClass.class, somethingOnSub.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class, somethingOnSub.getDeclaringClass());
		assertEquals(NonInheritedAnnotationClass.class.getDeclaredMethod("something"), somethingOnSub.getDeclaringMethod());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnInterface() throws Exception {
		// Note: @Order is not inherited.
		Order rawAnnotation = NonInheritedAnnotationInterface.class.getDeclaredMethod("something").getAnnotation(Order.class);

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor;

		descriptor = findAnnotationDescriptorForTypes(
				NonInheritedAnnotationInterface.class.getDeclaredMethod("something"), Order.class);
		assertNotNull(descriptor);
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());

		descriptor = findAnnotationDescriptorForTypes(
				SubNonInheritedAnnotationInterface.class.getDeclaredMethod("something"), Order.class);
		assertNotNull(descriptor);
		assertEquals(SubNonInheritedAnnotationInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class, descriptor.getDeclaringClass());
		assertEquals(NonInheritedAnnotationInterface.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithLocalAndMetaComponentAnnotation() throws Exception {
		Class<Transactional> annotationType = Transactional.class;
		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor = findAnnotationDescriptorForTypes(
				HasLocalAndMetaComponentAnnotation.class.getDeclaredMethod("something"), Component.class,
				annotationType, Order.class);

		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getRootDeclaringClass());
		assertEquals(HasLocalAndMetaComponentAnnotation.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertNull(descriptor.getComposedAnnotation());
		assertNull(descriptor.getComposedAnnotationType());
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Method method, String name,
			Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(method, method.getDeclaringClass(), name,
				composedAnnotationType);
	}

	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Method method,
			Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(method, method, rootDeclaringClass,
				composedAnnotationType, name, composedAnnotationType);
	}

	@SuppressWarnings("unchecked")
	private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Method startMethod,
			Method expectedMethod, Class<?> rootDeclaringClass, Class<?> declaringClass, String name,
			Class<? extends Annotation> composedAnnotationType) {
		Class<Component> annotationType = Component.class;
		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor = findAnnotationDescriptorForTypes(
				startMethod, Service.class, annotationType, Order.class, Transactional.class);
		assertNotNull("UntypedAnnotationMethodDescriptor should not be null", descriptor);
		assertEquals("rootDeclaringClass", rootDeclaringClass, descriptor.getRootDeclaringClass());
		assertEquals("declaringClass", declaringClass, descriptor.getDeclaringClass());
		assertEquals("declaringMethod", expectedMethod, descriptor.getDeclaringMethod());
		assertEquals("annotationType", annotationType, descriptor.getAnnotationType());
		assertEquals("component name", name, ((Component) descriptor.getAnnotation()).value());
		assertNotNull("composedAnnotation should not be null", descriptor.getComposedAnnotation());
		assertEquals("composedAnnotationType", composedAnnotationType, descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForTypesWithMetaComponentAnnotation() throws Exception {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				HasMetaComponentAnnotation.class.getDeclaredMethod("something"), "meta1",
				MetaAnnotationUtilsTests.Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithMetaAnnotationWithDefaultAttributes() throws Exception {
		Class<?> startClass = MetaConfigWithDefaultAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor = findAnnotationDescriptorForTypes(
				startClass.getDeclaredMethod("something"), Service.class,
				ContextConfiguration.class, Order.class, Transactional.class);

		assertNotNull(descriptor);
		assertEquals(startClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getDeclaringClass());
		assertEquals(startClass.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertArrayEquals(new Class[] {}, ((ContextConfiguration) descriptor.getAnnotation()).value());
		assertArrayEquals(new Class[] {
						MetaAnnotationUtilsTests.MetaConfig.DevConfig.class,
						MetaAnnotationUtilsTests.MetaConfig.ProductionConfig.class
		}, descriptor.getAnnotationAttributes().getClassArray("classes"));
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getComposedAnnotationType());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesWithMetaAnnotationWithOverriddenAttributes() throws Exception {
		Class<?> startClass = MetaConfigWithOverriddenAttributesTestCase.class;
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor = findAnnotationDescriptorForTypes(
				startClass.getDeclaredMethod("something"), Service.class,
				ContextConfiguration.class, Order.class, Transactional.class);

		assertNotNull(descriptor);
		assertEquals(startClass, descriptor.getRootDeclaringClass());
		assertEquals(MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getDeclaringClass());
		assertEquals(startClass.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(annotationType, descriptor.getAnnotationType());
		assertArrayEquals(new Class[] {}, ((ContextConfiguration) descriptor.getAnnotation()).value());
		assertArrayEquals(new Class[] { MetaAnnotationUtilsTests.class },
				descriptor.getAnnotationAttributes().getClassArray("classes"));
		assertNotNull(descriptor.getComposedAnnotation());
		assertEquals(MetaAnnotationUtilsTests.MetaConfig.class, descriptor.getComposedAnnotationType());
	}

	@Test
	public void findAnnotationDescriptorForTypesForInterfaceWithMetaAnnotation() throws Exception {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				InterfaceWithMetaAnnotation.class.getDeclaredMethod("something"),
				"meta1", MetaAnnotationUtilsTests.Meta1.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesForClassWithMetaAnnotatedInterface() throws Exception {
		Component rawAnnotation = findAnnotation(ClassWithMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				Component.class);

		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor descriptor;

		descriptor = findAnnotationDescriptorForTypes(
				ClassWithMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				Service.class, Component.class, Order.class, Transactional.class);
		assertNotNull(descriptor);
		assertEquals(ClassWithMetaAnnotatedInterface.class, descriptor.getRootDeclaringClass());
		assertEquals(MetaAnnotationUtilsTests.Meta1.class, descriptor.getDeclaringClass());
		assertEquals(InterfaceWithMetaAnnotation.class.getDeclaredMethod("something"), descriptor.getDeclaringMethod());
		assertEquals(rawAnnotation, descriptor.getAnnotation());
		assertEquals(MetaAnnotationUtilsTests.Meta1.class, descriptor.getComposedAnnotation().annotationType());
	}

	@Test
	public void findAnnotationDescriptorForTypesForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() throws Exception {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				"meta2", MetaAnnotationUtilsTests.Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() throws Exception {
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class.getDeclaredMethod("something"),
				SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
				MetaAnnotationUtilsTests.Meta2.class, "meta2", MetaAnnotationUtilsTests.Meta2.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesOnMetaMetaAnnotatedClass() throws Exception {
		Class<MetaMetaAnnotatedClass> startClass = MetaMetaAnnotatedClass.class;
		Method something = startClass.getDeclaredMethod("something");
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				something, something,
				startClass, MetaAnnotationUtilsTests.Meta2.class, "meta2",
				MetaAnnotationUtilsTests.MetaMeta.class);
	}

	@Test
	public void findAnnotationDescriptorForTypesOnMetaMetaMetaAnnotatedClass() throws Exception {
		Class<MetaMetaMetaAnnotatedClass> startClass = MetaMetaMetaAnnotatedClass.class;
		Method something = startClass.getDeclaredMethod("something");
		assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
				something, something,
				startClass, MetaAnnotationUtilsTests.Meta2.class, "meta2",
				MetaAnnotationUtilsTests.MetaMetaMeta.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesOnAnnotatedClassWithMissingTargetMetaAnnotation() throws Exception {
		// InheritedAnnotationClass is NOT annotated or meta-annotated with @Component,
		// @Service, or @Order, but it is annotated with @Transactional.
		MetaAnnotationUtils.UntypedAnnotationOnMethodDescriptor
				descriptor = findAnnotationDescriptorForTypes(InheritedAnnotationClass.class.getDeclaredMethod("something"),
				Service.class, Component.class, Order.class);
		assertNull("Should not find @Component on InheritedAnnotationClass", descriptor);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void findAnnotationDescriptorForTypesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() throws Exception {
		MetaAnnotationUtils.UntypedAnnotationDescriptor
				descriptor = findAnnotationDescriptorForTypes(MetaCycleAnnotatedClass.class.getDeclaredMethod("something"),
				Service.class, Component.class, Order.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", descriptor);
	}

	// -------------------------------------------------------------------------

	static class HasMetaComponentAnnotation {
		@MetaAnnotationUtilsTests.Meta1
		public void something(){
			// for test purposes
		}
	}

	static class HasLocalAndMetaComponentAnnotation {
		@MetaAnnotationUtilsTests.Meta1
		@Transactional
		@MetaAnnotationUtilsTests.Meta2
		public void something(){
			// for test purposes
		}
	}

	interface InterfaceWithMetaAnnotation {
		@MetaAnnotationUtilsTests.Meta1
		void something();
	}

	static class ClassWithMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
		@Override
		public void something(){
			// for test purposes
		}
	}

	static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
		@Override
		@MetaAnnotationUtilsTests.Meta2
		public void something(){
			// for test purposes
		}
	}

	static class SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface extends ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface {
		@Override
		public void something(){
			// for test purposes
		}
	}

	static class MetaMetaAnnotatedClass {
		@MetaAnnotationUtilsTests.MetaMeta
		public void something(){
			// for test purposes
		}
	}

	static class MetaMetaMetaAnnotatedClass {
		@MetaAnnotationUtilsTests.MetaMetaMeta
		public void something(){
			// for test purposes
		}
	}

	static class MetaCycleAnnotatedClass {
		@MetaAnnotationUtilsTests.MetaCycle3
		public void something(){
			// for test purposes
		}
	}

	public class MetaConfigWithDefaultAttributesTestCase {
		@MetaAnnotationUtilsTests.MetaConfig
		public void something(){
			// for test purposes
		}
	}

	public class MetaConfigWithOverriddenAttributesTestCase {
		@MetaAnnotationUtilsTests.MetaConfig(classes = MetaAnnotationUtilsTests.class)
		public void something(){
			// for test purposes
		}
	}

	// -------------------------------------------------------------------------

	interface InheritedAnnotationInterface {
		@Transactional
		void something();
	}

	interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
		@Override
		void something();
	}

	interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
		@Override
		void something();
	}

	interface NonInheritedAnnotationInterface {
		@Order
		void something();
	}

	interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
		@Override
		void something();
	}

	static class NonAnnotatedClass {
		public void something(){
			// for test purposes
		}
	}

	interface NonAnnotatedInterface {
		void something();
	}

	static class InheritedAnnotationClass {
		@Transactional
		public void something(){
			// for test purposes
		}
	}

	static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
		@Override
		public void something(){
			// for test purposes
		}
	}

	static class NonInheritedAnnotationClass {
		@Order
		public void something(){
			// for test purposes
		}
	}

	static class SubNonInheritedAnnotationClass extends NonInheritedAnnotationClass {
		@Override
		public void something(){
			// for test purposes
		}
	}

	static class AnnotatedContextConfigClass {
		@ContextConfiguration(classes = Number.class)
		public void something(){
			// for test purposes
		}
	}

	static class MetaAnnotatedAndSuperAnnotatedContextConfigClass extends AnnotatedContextConfigClass {
		@MetaAnnotationUtilsTests.MetaConfig(classes = String.class)
		@Override
		public void something(){
			// for test purposes
		}
	}

}
