/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 */
public class AnnotationAwareOrderComparatorTests {

	@Test
	public void instanceVariableIsAnAnnotationAwareOrderComparator() {
		assertThat(AnnotationAwareOrderComparator.INSTANCE, is(instanceOf(AnnotationAwareOrderComparator.class)));
	}

	@Test
	public void sortInstances() {
		List<Object> list = new ArrayList<>();
		list.add(new B());
		list.add(new A());
		AnnotationAwareOrderComparator.sort(list);
		assertTrue(list.get(0) instanceof A);
		assertTrue(list.get(1) instanceof B);
	}

	@Test
	public void sortClasses() {
		List<Object> list = new ArrayList<>();
		list.add(B.class);
		list.add(A.class);
		AnnotationAwareOrderComparator.sort(list);
		assertEquals(A.class, list.get(0));
		assertEquals(B.class, list.get(1));
	}


	@Order(1)
	private static class A {
	}

	@Order(2)
	private static class B {
	}

}
