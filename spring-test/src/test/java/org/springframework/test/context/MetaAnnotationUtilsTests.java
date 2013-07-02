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
	public void testFindAnnotationDescriptor() throws Exception {
		// no annotation
		assertNull(findAnnotationDescriptor(Transactional.class, NonAnnotatedInterface.class));
		assertNull(findAnnotationDescriptor(Transactional.class, NonAnnotatedClass.class));

		// inherited class-level annotation; note: @Transactional is inherited
		assertEquals(InheritedAnnotationInterface.class,
			findAnnotationDescriptor(Transactional.class, InheritedAnnotationInterface.class).getAnnotatedClass());
		assertNull(findAnnotationDescriptor(Transactional.class, SubInheritedAnnotationInterface.class));
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(Transactional.class, InheritedAnnotationClass.class).getAnnotatedClass());
		assertEquals(InheritedAnnotationClass.class,
			findAnnotationDescriptor(Transactional.class, SubInheritedAnnotationClass.class).getAnnotatedClass());

		// non-inherited class-level annotation; note: @Order is not inherited,
		// but findAnnotationDescriptor() should still find it.
		assertEquals(NonInheritedAnnotationInterface.class,
			findAnnotationDescriptor(Order.class, NonInheritedAnnotationInterface.class).getAnnotatedClass());
		assertNull(findAnnotationDescriptor(Order.class, SubNonInheritedAnnotationInterface.class));
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(Order.class, NonInheritedAnnotationClass.class).getAnnotatedClass());
		assertEquals(NonInheritedAnnotationClass.class,
			findAnnotationDescriptor(Order.class, SubNonInheritedAnnotationClass.class).getAnnotatedClass());

		// Meta-annotations:
		AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(Component.class,
			HasMetaComponentAnnotation.class);
		assertEquals(HasMetaComponentAnnotation.class, descriptor.getAnnotatedClass());
		assertEquals(Meta1.class, descriptor.getMetaAnnotatedClass());
		descriptor = findAnnotationDescriptor(Component.class, HasLocalAndMetaComponentAnnotation.class);
		assertEquals(HasLocalAndMetaComponentAnnotation.class, descriptor.getAnnotatedClass());
		assertNull(descriptor.getMetaAnnotatedClass());
	}


	// -------------------------------------------------------------------------

	@Component(value = "meta1")
	@Order
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta1 {
	}

	@Component(value = "meta2")
	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta2 {
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

	// -------------------------------------------------------------------------

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

}
