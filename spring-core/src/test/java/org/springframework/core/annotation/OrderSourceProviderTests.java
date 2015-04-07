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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.core.Ordered;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class OrderSourceProviderTests {

	private final AnnotationAwareOrderComparator comparator = AnnotationAwareOrderComparator.INSTANCE;


	@Test
	public void plainComparator() {
		List<Object> items = new ArrayList<Object>();
		C c = new C(5);
		C c2 = new C(-5);
		items.add(c);
		items.add(c2);
		Collections.sort(items, comparator);
		assertOrder(items, c2, c);
	}

	@Test
	public void listNoFactoryMethod() {
		A a = new A();
		C c = new C(-50);
		B b = new B();

		List<?> items = Arrays.asList(a, c, b);
		Collections.sort(items, comparator.withSourceProvider(obj -> null));
		assertOrder(items, c, a, b);
	}

	@Test
	public void listFactoryMethod() {
		A a = new A();
		C c = new C(3);
		B b = new B();

		List<?> items = Arrays.asList(a, c, b);
		Collections.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return new C(4);
			}
			if (obj == b) {
				return new C(2);
			}
			return null;
		}));
		assertOrder(items, b, c, a);
	}

	@Test
	public void listFactoryMethodOverridesStaticOrder() {
		A a = new A();
		C c = new C(5);
		C c2 = new C(-5);

		List<?> items = Arrays.asList(a, c, c2);
		Collections.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return 4;
			}
			if (obj == c2) {
				return 2;
			}
			return null;
		}));
		assertOrder(items, c2, a, c);
	}

	@Test
	public void arrayNoFactoryMethod() {
		A a = new A();
		C c = new C(-50);
		B b = new B();

		Object[] items = new Object[] {a, c, b};
		Arrays.sort(items, comparator.withSourceProvider(obj -> null));
		assertOrder(items, c, a, b);
	}

	@Test
	public void arrayFactoryMethod() {
		A a = new A();
		C c = new C(3);
		B b = new B();

		Object[] items = new Object[] {a, c, b};
		Arrays.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return new C(4);
			}
			if (obj == b) {
				return new C(2);
			}
			return null;
		}));
		assertOrder(items, b, c, a);
	}

	@Test
	public void arrayFactoryMethodOverridesStaticOrder() {
		A a = new A();
		C c = new C(5);
		C c2 = new C(-5);

		Object[] items = new Object[] {a, c, c2};
		Arrays.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return 4;
			}
			if (obj == c2) {
				return 2;
			}
			return null;
		}));
		assertOrder(items, c2, a, c);
	}


	private void assertOrder(List<?> actual, Object... expected) {
		for (int i = 0; i < actual.size(); i++) {
			assertSame("Wrong instance at index '" + i + "'", expected[i], actual.get(i));
		}
		assertEquals("Wrong number of items", expected.length, actual.size());
	}

	private void assertOrder(Object[] actual, Object... expected) {
		for (int i = 0; i < actual.length; i++) {
			assertSame("Wrong instance at index '" + i + "'", expected[i], actual[i]);
		}
		assertEquals("Wrong number of items", expected.length, expected.length);
	}


	@Order(1)
	private static class A {
	}


	@Order(2)
	private static class B {
	}


	private static class C implements Ordered {

		private final int order;

		private C(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

}
