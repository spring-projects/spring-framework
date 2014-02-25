/*
 * Copyright 2002-2014 the original author or authors.
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.subpackage.NonPublicAnnotatedClass;
import org.springframework.stereotype.Component;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * Unit tests for {@link AnnotationUtils}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Phillip Webb
 */
public class AnnotationUtilsTests {

	@Test
	public void findMethodAnnotationOnLeaf() throws Exception {
		Method m = Leaf.class.getMethod("annotatedOnLeaf", (Class[]) null);
		assertNotNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationOnRoot() throws Exception {
		Method m = Leaf.class.getMethod("annotatedOnRoot", (Class[]) null);
		assertNotNull(m.getAnnotation(Order.class));
		assertNotNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationOnRootButOverridden() throws Exception {
		Method m = Leaf.class.getMethod("overrideWithoutNewAnnotation", (Class[]) null);
		assertNull(m.getAnnotation(Order.class));
		assertNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationNotAnnotated() throws Exception {
		Method m = Leaf.class.getMethod("notAnnotated", (Class[]) null);
		assertNull(findAnnotation(m, Order.class));
	}

	@Test
	public void findMethodAnnotationOnBridgeMethod() throws Exception {
		Method m = SimpleFoo.class.getMethod("something", Object.class);
		assertTrue(m.isBridge());
		assertNull(m.getAnnotation(Order.class));
		assertNull(getAnnotation(m, Order.class));
		assertNotNull(findAnnotation(m, Order.class));
		// TODO: actually found on OpenJDK 8 b99! assertNull(m.getAnnotation(Transactional.class));
		assertNotNull(getAnnotation(m, Transactional.class));
		assertNotNull(findAnnotation(m, Transactional.class));
	}

	// TODO consider whether we want this to handle annotations on interfaces
	// public void findMethodAnnotationFromInterfaceImplementedByRoot()
	// throws Exception {
	// Method m = Leaf.class.getMethod("fromInterfaceImplementedByRoot",
	// (Class[]) null);
	// Order o = findAnnotation(Order.class, m, Leaf.class);
	// assertNotNull(o);
	// }

	@Test
	public void findAnnotationFavorsInterfacesOverLocalMetaAnnotations() {
		Component component = AnnotationUtils.findAnnotation(
			ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, Component.class);
		assertNotNull(component);

		// By inspecting ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface, one
		// might expect that "meta2" should be found; however, with the current
		// implementation "meta1" will be found.
		assertEquals("meta1", component.value());
	}

	@Test
	public void findAnnotationFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		Transactional transactional = AnnotationUtils.findAnnotation(SubSubClassWithInheritedAnnotation.class,
			Transactional.class);
		assertNotNull(transactional);

		// By inspecting SubSubClassWithInheritedAnnotation, one might expect that the
		// readOnly flag should be true, since the immediate superclass is annotated with
		// @Composed2; however, with the current implementation the readOnly flag will be
		// false since @Transactional is declared as @Inherited.
		assertFalse("readOnly flag for SubSubClassWithInheritedAnnotation", transactional.readOnly());
	}

	@Test
	public void findAnnotationFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
		Component component = AnnotationUtils.findAnnotation(SubSubClassWithInheritedMetaAnnotation.class,
			Component.class);
		assertNotNull(component);

		// By inspecting SubSubClassWithInheritedMetaAnnotation, one might expect that
		// "meta2" should be found, since the immediate superclass is annotated with
		// @Meta2; however, with the current implementation "meta1" will be found since
		// @Meta1 is declared as @Inherited.
		assertEquals("meta1", component.value());
	}

	@Test
	public void findAnnotationOnMetaMetaAnnotatedClass() {
		Component component = AnnotationUtils.findAnnotation(MetaMetaAnnotatedClass.class, Component.class);
		assertNotNull("Should find meta-annotation on composed annotation on class", component);
		assertEquals("meta2", component.value());
	}

	@Test
	public void findAnnotationOnMetaMetaMetaAnnotatedClass() {
		Component component = AnnotationUtils.findAnnotation(MetaMetaMetaAnnotatedClass.class, Component.class);
		assertNotNull("Should find meta-annotation on meta-annotation on composed annotation on class", component);
		assertEquals("meta2", component.value());
	}

	@Test
	public void findAnnotationOnAnnotatedClassWithMissingTargetMetaAnnotation() {
		// TransactionalClass is NOT annotated or meta-annotated with @Component
		Component component = AnnotationUtils.findAnnotation(TransactionalClass.class, Component.class);
		assertNull("Should not find @Component on TransactionalClass", component);
	}

	@Test
	public void findAnnotationOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
		Component component = AnnotationUtils.findAnnotation(MetaCycleAnnotatedClass.class, Component.class);
		assertNull("Should not find @Component on MetaCycleAnnotatedClass", component);
	}

	@Test
	public void testFindAnnotationDeclaringClass() throws Exception {
		// no class-level annotation
		assertNull(findAnnotationDeclaringClass(Transactional.class, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClass(Transactional.class, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDeclaringClass(Transactional.class, InheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClass(Transactional.class, SubInheritedAnnotationInterface.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClass(Transactional.class, InheritedAnnotationClass.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClass(Transactional.class, SubInheritedAnnotationClass.class));

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClass() should still find it on classes.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDeclaringClass(Order.class, NonInheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClass(Order.class, SubNonInheritedAnnotationInterface.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClass(Order.class, NonInheritedAnnotationClass.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClass(Order.class, SubNonInheritedAnnotationClass.class));
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithSingleCandidateType() {
		// no class-level annotation
		List<Class<? extends Annotation>> transactionalCandidateList = Arrays.<Class<? extends Annotation>> asList(Transactional.class);
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDeclaringClassForTypes(transactionalCandidateList, InheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(transactionalCandidateList, SubInheritedAnnotationInterface.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(transactionalCandidateList, InheritedAnnotationClass.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(transactionalCandidateList, SubInheritedAnnotationClass.class));

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on classes.
		List<Class<? extends Annotation>> orderCandidateList = Arrays.<Class<? extends Annotation>> asList(Order.class);
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDeclaringClassForTypes(orderCandidateList, NonInheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(orderCandidateList, SubNonInheritedAnnotationInterface.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(orderCandidateList, NonInheritedAnnotationClass.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(orderCandidateList, SubNonInheritedAnnotationClass.class));
	}

	@Test
	public void findAnnotationDeclaringClassForTypesWithMultipleCandidateTypes() {
		List<Class<? extends Annotation>> candidates = Arrays.<Class<? extends Annotation>> asList(Transactional.class, Order.class);

		// no class-level annotation
		assertNull(findAnnotationDeclaringClassForTypes(candidates, NonAnnotatedInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(candidates, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDeclaringClassForTypes(candidates, InheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(candidates, SubInheritedAnnotationInterface.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(candidates, InheritedAnnotationClass.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(candidates, SubInheritedAnnotationClass.class));

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDeclaringClassForTypes() should still find it on classes.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDeclaringClassForTypes(candidates, NonInheritedAnnotationInterface.class));
		assertNull(findAnnotationDeclaringClassForTypes(candidates, SubNonInheritedAnnotationInterface.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(candidates, NonInheritedAnnotationClass.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDeclaringClassForTypes(candidates, SubNonInheritedAnnotationClass.class));

		// class hierarchy mixed with @Transactional and @Order declarations
		assertEquals(TransactionalClass.class,
			findAnnotationDeclaringClassForTypes(candidates, TransactionalClass.class));
		assertEquals(TransactionalAndOrderedClass.class,
			findAnnotationDeclaringClassForTypes(candidates, TransactionalAndOrderedClass.class));
		assertEquals(TransactionalAndOrderedClass.class,
			findAnnotationDeclaringClassForTypes(candidates, SubTransactionalAndOrderedClass.class));
	}

	@Test
	public void testIsAnnotationDeclaredLocally() throws Exception {
		// no class-level annotation
		assertFalse(isAnnotationDeclaredLocally(Transactional.class, NonAnnotatedInterface.class));
		assertFalse(isAnnotationDeclaredLocally(Transactional.class, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertTrue(isAnnotationDeclaredLocally(Transactional.class, InheritedAnnotationInterface.class));
		assertFalse(isAnnotationDeclaredLocally(Transactional.class, SubInheritedAnnotationInterface.class));
		assertTrue(isAnnotationDeclaredLocally(Transactional.class, InheritedAnnotationClass.class));
		assertFalse(isAnnotationDeclaredLocally(Transactional.class, SubInheritedAnnotationClass.class));

		// non-inherited class-level annotation; note: @Order is not inherited
		assertTrue(isAnnotationDeclaredLocally(Order.class, NonInheritedAnnotationInterface.class));
		assertFalse(isAnnotationDeclaredLocally(Order.class, SubNonInheritedAnnotationInterface.class));
		assertTrue(isAnnotationDeclaredLocally(Order.class, NonInheritedAnnotationClass.class));
		assertFalse(isAnnotationDeclaredLocally(Order.class, SubNonInheritedAnnotationClass.class));
	}

	@Test
	public void testIsAnnotationInherited() throws Exception {
		// no class-level annotation
		assertFalse(isAnnotationInherited(Transactional.class, NonAnnotatedInterface.class));
		assertFalse(isAnnotationInherited(Transactional.class, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertFalse(isAnnotationInherited(Transactional.class, InheritedAnnotationInterface.class));
		// isAnnotationInherited() does not currently traverse interface
		// hierarchies. Thus the following, though perhaps counter intuitive,
		// must be false:
		assertFalse(isAnnotationInherited(Transactional.class, SubInheritedAnnotationInterface.class));
		assertFalse(isAnnotationInherited(Transactional.class, InheritedAnnotationClass.class));
		assertTrue(isAnnotationInherited(Transactional.class, SubInheritedAnnotationClass.class));

		// non-inherited class-level annotation; note: @Order is not inherited
		assertFalse(isAnnotationInherited(Order.class, NonInheritedAnnotationInterface.class));
		assertFalse(isAnnotationInherited(Order.class, SubNonInheritedAnnotationInterface.class));
		assertFalse(isAnnotationInherited(Order.class, NonInheritedAnnotationClass.class));
		assertFalse(isAnnotationInherited(Order.class, SubNonInheritedAnnotationClass.class));
	}

	@Test
	public void getValueFromAnnotation() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		Order order = findAnnotation(method, Order.class);

		assertEquals(1, AnnotationUtils.getValue(order, AnnotationUtils.VALUE));
		assertEquals(1, AnnotationUtils.getValue(order));
	}

	@Test
	public void getValueFromNonPublicAnnotation() throws Exception {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		assertNotNull(annotation);
		assertEquals("NonPublicAnnotation", annotation.annotationType().getSimpleName());
		assertEquals(42, AnnotationUtils.getValue(annotation, AnnotationUtils.VALUE));
		assertEquals(42, AnnotationUtils.getValue(annotation));
	}

	@Test
	public void getDefaultValueFromAnnotation() throws Exception {
		Method method = SimpleFoo.class.getMethod("something", Object.class);
		Order order = findAnnotation(method, Order.class);

		assertEquals(Ordered.LOWEST_PRECEDENCE, AnnotationUtils.getDefaultValue(order, AnnotationUtils.VALUE));
		assertEquals(Ordered.LOWEST_PRECEDENCE, AnnotationUtils.getDefaultValue(order));
	}

	@Test
	public void getDefaultValueFromNonPublicAnnotation() throws Exception {
		Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
		assertEquals(1, declaredAnnotations.length);
		Annotation annotation = declaredAnnotations[0];
		assertNotNull(annotation);
		assertEquals("NonPublicAnnotation", annotation.annotationType().getSimpleName());
		assertEquals(-1, AnnotationUtils.getDefaultValue(annotation, AnnotationUtils.VALUE));
		assertEquals(-1, AnnotationUtils.getDefaultValue(annotation));
	}

	@Test
	public void getDefaultValueFromAnnotationType() throws Exception {
		assertEquals(Ordered.LOWEST_PRECEDENCE, AnnotationUtils.getDefaultValue(Order.class, AnnotationUtils.VALUE));
		assertEquals(Ordered.LOWEST_PRECEDENCE, AnnotationUtils.getDefaultValue(Order.class));
	}

	@Test
	public void findAnnotationFromInterface() throws Exception {
		Method method = ImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test
	public void findAnnotationFromInterfaceOnSuper() throws Exception {
		Method method = SubOfImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test
	public void findAnnotationFromInterfaceWhenSuperDoesNotImplementMethod() throws Exception {
		Method method = SubOfAbstractImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
		Order order = findAnnotation(method, Order.class);
		assertNotNull(order);
	}

	@Test
	public void findRepeatableAnnotationOnComposedAnnotation() {
		Repeatable repeatable = findAnnotation(MyRepeatableMeta.class, Repeatable.class);
		assertNotNull(repeatable);
		assertEquals(MyRepeatableContainer.class, repeatable.value());
	}

	@Test
	public void getRepeatableFromMethod() throws Exception {
		Method method = InterfaceWithRepeated.class.getMethod("foo");
		Set<MyRepeatable> annotions = AnnotationUtils.getRepeatableAnnotation(method,
				MyRepeatableContainer.class, MyRepeatable.class);
		Set<String> values = new HashSet<String>();
		for (MyRepeatable myRepeatable : annotions) {
			values.add(myRepeatable.value());
		}
		assertThat(values, equalTo((Set<String>) new HashSet<String>(
				Arrays.asList("a", "b", "c", "meta"))));
	}


	@Component(value = "meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Meta1 {
	}

	@Component(value = "meta2")
	@Transactional(readOnly = true)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta2 {
	}

	@Meta2
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMeta {
	}

	@MetaMeta
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMetaMeta {
	}

	@MetaCycle3
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle1 {
	}

	@MetaCycle1
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle2 {
	}

	@MetaCycle2
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaCycle3 {
	}

	@Meta1
	static interface InterfaceWithMetaAnnotation {
	}

	@Meta2
	static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
	}

	@Meta1
	static class ClassWithInheritedMetaAnnotation {
	}

	@Meta2
	static class SubClassWithInheritedMetaAnnotation extends ClassWithInheritedMetaAnnotation {
	}

	static class SubSubClassWithInheritedMetaAnnotation extends SubClassWithInheritedMetaAnnotation {
	}

	@Transactional
	static class ClassWithInheritedAnnotation {
	}

	@Meta2
	static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
	}

	static class SubSubClassWithInheritedAnnotation extends SubClassWithInheritedAnnotation {
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

	public static interface AnnotatedInterface {

		@Order(0)
		void fromInterfaceImplementedByRoot();
	}

	public static class Root implements AnnotatedInterface {

		@Order(27)
		public void annotatedOnRoot() {
		}

		public void overrideToAnnotate() {
		}

		@Order(27)
		public void overrideWithoutNewAnnotation() {
		}

		public void notAnnotated() {
		}

		@Override
		public void fromInterfaceImplementedByRoot() {
		}
	}

	public static class Leaf extends Root {

		@Order(25)
		public void annotatedOnLeaf() {
		}

		@Override
		@Order(1)
		public void overrideToAnnotate() {
		}

		@Override
		public void overrideWithoutNewAnnotation() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Transactional {

		boolean readOnly() default false;
	}

	public static abstract class Foo<T> {

		@Order(1)
		public abstract void something(T arg);
	}

	public static class SimpleFoo extends Foo<String> {

		@Override
		@Transactional
		public void something(final String arg) {
		}
	}

	@Transactional
	public static interface InheritedAnnotationInterface {
	}

	public static interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
	}

	@Order
	public static interface NonInheritedAnnotationInterface {
	}

	public static interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
	}

	public static class NonAnnotatedClass {
	}

	public static interface NonAnnotatedInterface {
	}

	@Transactional
	public static class InheritedAnnotationClass {
	}

	public static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
	}

	@Order
	public static class NonInheritedAnnotationClass {
	}

	public static class SubNonInheritedAnnotationClass extends NonInheritedAnnotationClass {
	}

	@Transactional
	public static class TransactionalClass {
	}

	@Order
	public static class TransactionalAndOrderedClass extends TransactionalClass {
	}

	public static class SubTransactionalAndOrderedClass extends TransactionalAndOrderedClass {
	}

	public static interface InterfaceWithAnnotatedMethod {

		@Order
		void foo();
	}

	public static class ImplementsInterfaceWithAnnotatedMethod implements InterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public static class SubOfImplementsInterfaceWithAnnotatedMethod extends ImplementsInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	public abstract static class AbstractDoesNotImplementInterfaceWithAnnotatedMethod
			implements InterfaceWithAnnotatedMethod {
	}

	public static class SubOfAbstractImplementsInterfaceWithAnnotatedMethod
			extends AbstractDoesNotImplementInterfaceWithAnnotatedMethod {

		@Override
		public void foo() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface MyRepeatableContainer {

		MyRepeatable[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(MyRepeatableContainer.class)
	@interface MyRepeatable {

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@MyRepeatable("meta")
	@interface MyRepeatableMeta {
	}

	public static interface InterfaceWithRepeated {

		@MyRepeatable("a")
		@MyRepeatableContainer({ @MyRepeatable("b"), @MyRepeatable("c") })
		@MyRepeatableMeta
		void foo();
	}

}
