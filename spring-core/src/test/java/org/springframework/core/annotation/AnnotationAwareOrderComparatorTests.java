/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.core.CyclicOrderException;
import org.springframework.core.DependsOnAfter;
import org.springframework.core.DependsOnBefore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Yongjun Hong
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

	@Test
	void sortWithDependsOnBefore() {
		List<Object> list = new ArrayList<>();
		list.add(A.class);
		list.add(D.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(D.class, A.class);
	}

	@Test
	void sortWithDependsOnAfter() {
		List<Object> list = new ArrayList<>();
		list.add(B.class);
		list.add(E.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(B.class, E.class);
	}

	@Test
	void sortWithDependsOnBeforeAndAfter() {
		List<Object> list = new ArrayList<>();
		list.add(A.class);
		list.add(B.class);
		list.add(D.class);
		list.add(E.class);
		AnnotationAwareOrderComparator.sort(list);
		assertThat(list).containsExactly(D.class, A.class, B.class, E.class);
	}

	@Test
	void sortWithCircularDependsOn() {
		List<Object> list = new ArrayList<>();
		list.add(F.class);
		list.add(G.class);
		assertThatThrownBy(() -> AnnotationAwareOrderComparator.sort(list))
				.isInstanceOf(CyclicOrderException.class);
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

	@DependsOnBefore(A.class)
	private static class D {
	}

	@DependsOnAfter(B.class)
	private static class E {
	}

	@DependsOnBefore(G.class)
	private static class F {
	}

	@DependsOnBefore(F.class)
	private static class G {
	}
}
