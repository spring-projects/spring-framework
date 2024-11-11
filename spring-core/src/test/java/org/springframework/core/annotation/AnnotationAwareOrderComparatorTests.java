/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.annotation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Priority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 */
class AnnotationAwareOrderComparatorTests {

	@Test
	void instanceVariableIsAnAnnotationAwareOrderComparator() {
		assertThat(AnnotationAwareOrderComparator.INSTANCE).isInstanceOf(AnnotationAwareOrderComparator.class);
	}

	@Test
	void sortInstances() {
		List<Object> list = new ArrayList<>();
		list.add(new B());
		list.add(new A());
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).hasExactlyElementsOfTypes(A.class, B.class);
	}

	@Test
	void sortInstancesWithPriority() {
		List<Object> list = new ArrayList<>();
		list.add(new B2());
		list.add(new A2());
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).hasExactlyElementsOfTypes(A2.class, B2.class);
	}

	@Test
	void sortInstancesWithOrderAndPriority() {
		List<Object> list = new ArrayList<>();
		list.add(new B());
		list.add(new A2());
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).hasExactlyElementsOfTypes(A2.class, B.class);
	}

	@Test
	void sortInstancesWithSubclass() {
		List<Object> list = new ArrayList<>();
		list.add(new B());
		list.add(new C());
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).hasExactlyElementsOfTypes(C.class, B.class);
	}

	@Test
	void sortClasses() {
		List<Object> list = new ArrayList<>();
		list.add(B.class);
		list.add(A.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(A.class, B.class);
	}

	@Test
	void sortClassesWithSubclass() {
		List<Object> list = new ArrayList<>();
		list.add(B.class);
		list.add(C.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(C.class, B.class);
	}

	@Test
	void sortWithNulls() {
		List<Object> list = new ArrayList<>();
		list.add(null);
		list.add(B.class);
		list.add(null);
		list.add(A.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(A.class, B.class, null, null);
	}

	@Order(1)
	private static class A {
	}

	@Order(2)
	private static class B {
	}

	private static class C extends A {
	}

	@Priority(1)
	private static class A2 {
	}

	@Priority(2)
	private static class B2 {
	}

}
