/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.util.Comparator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link OrderComparator} class.
 *
 * @author Rick Evans
 * @author Stephane Nicoll
 */
public class OrderComparatorTests {

	private final OrderComparator comparator = new OrderComparator();

	@Test
	public void compareOrderedInstancesBefore() {
		assertEquals(-1, this.comparator.compare(
				new StubOrdered(100), new StubOrdered(2000)));
	}

	@Test
	public void compareOrderedInstancesSame() {
		assertEquals(0, this.comparator.compare(
				new StubOrdered(100), new StubOrdered(100)));
	}

	@Test
	public void compareOrderedInstancesAfter() {
		assertEquals(1, this.comparator.compare(
				new StubOrdered(982300), new StubOrdered(100)));
	}

	@Test
	public void compareTwoNonOrderedInstancesEndsUpAsSame() {
		assertEquals(0, this.comparator.compare(new Object(), new Object()));
	}

	@Test
	public void compareWithSimpleSourceProvider() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new StubOrdered(25)));
		assertEquals(-1, customComparator.compare(new StubOrdered(10), 5L));
	}

	@Test
	public void compareWithSourceProviderArray() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new Object[] {new StubOrdered(10), new StubOrdered(-25)}));
		assertEquals(-1, customComparator.compare(5L, new Object()));
	}

	@Test
	public void compareWithSourceProviderArrayNoMatch() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new Object[]{new Object(), new Object()}));
		assertEquals(0, customComparator.compare(new Object(), 5L));
	}

	@Test
	public void compareWithSourceProviderEmpty() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(50L, new Object()));
		assertEquals(0, customComparator.compare(new Object(), 5L));
	}


	private static final class TestSourceProvider implements OrderComparator.OrderSourceProvider {

		private final Object target;
		private final Object orderSource;

		public TestSourceProvider(Object target, Object orderSource) {
			this.target = target;
			this.orderSource = orderSource;
		}

		@Override
		public Object getOrderSource(Object obj) {
			if (target.equals(obj)) {
				return orderSource;
			}
			return null;
		}
	}

	private static final class StubOrdered implements Ordered {

		private final int order;


		public StubOrdered(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}
	}

}
