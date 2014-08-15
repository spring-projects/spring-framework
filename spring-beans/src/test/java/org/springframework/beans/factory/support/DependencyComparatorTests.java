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

package org.springframework.beans.factory.support;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.core.Ordered;

/**
 *
 * @author Stephane Nicoll
 */
public class DependencyComparatorTests {

	private final DefaultDependencyComparator comparator = new DefaultDependencyComparator();

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

	private void assertOrder(List<?> actual, Object... expected) {
		for (int i = 0; i < actual.size(); i++) {
			assertSame("Wrong instance at index '" + i + "'", expected[i], actual.get(i));
		}
		assertEquals("Wrong number of items", expected.length, actual.size());
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
