/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

import java.util.Comparator;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link OrderComparator} class.
 *
 * @author Rick Evans
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class OrderComparatorTests {

	private final OrderComparator comparator = new OrderComparator();


	@Test
	public void compareOrderedInstancesBefore() {
		assertThat(this.comparator.compare(new StubOrdered(100), new StubOrdered(2000))).isEqualTo(-1);
	}

	@Test
	public void compareOrderedInstancesSame() {
		assertThat(this.comparator.compare(new StubOrdered(100), new StubOrdered(100))).isEqualTo(0);
	}

	@Test
	public void compareOrderedInstancesAfter() {
		assertThat(this.comparator.compare(new StubOrdered(982300), new StubOrdered(100))).isEqualTo(1);
	}

	@Test
	public void compareOrderedInstancesNullFirst() {
		assertThat(this.comparator.compare(null, new StubOrdered(100))).isEqualTo(1);
	}

	@Test
	public void compareOrderedInstancesNullLast() {
		assertThat(this.comparator.compare(new StubOrdered(100), null)).isEqualTo(-1);
	}

	@Test
	public void compareOrderedInstancesDoubleNull() {
		assertThat(this.comparator.compare(null, null)).isEqualTo(0);
	}

	@Test
	public void compareTwoNonOrderedInstancesEndsUpAsSame() {
		assertThat(this.comparator.compare(new Object(), new Object())).isEqualTo(0);
	}

	@Test
	public void comparePriorityOrderedInstancesBefore() {
		assertThat(this.comparator.compare(new StubPriorityOrdered(100), new StubPriorityOrdered(2000))).isEqualTo(-1);
	}

	@Test
	public void comparePriorityOrderedInstancesSame() {
		assertThat(this.comparator.compare(new StubPriorityOrdered(100), new StubPriorityOrdered(100))).isEqualTo(0);
	}

	@Test
	public void comparePriorityOrderedInstancesAfter() {
		assertThat(this.comparator.compare(new StubPriorityOrdered(982300), new StubPriorityOrdered(100))).isEqualTo(1);
	}

	@Test
	public void comparePriorityOrderedInstanceToStandardOrderedInstanceWithHigherPriority() {
		assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(200), new StubOrdered(100));
	}

	@Test
	public void comparePriorityOrderedInstanceToStandardOrderedInstanceWithSamePriority() {
		assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(100), new StubOrdered(100));
	}

	@Test
	public void comparePriorityOrderedInstanceToStandardOrderedInstanceWithLowerPriority() {
		assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(100), new StubOrdered(200));
	}

	private void assertThatPriorityOrderedAlwaysWins(StubPriorityOrdered priority, StubOrdered standard) {
		assertThat(this.comparator.compare(priority, standard)).isEqualTo(-1);
		assertThat(this.comparator.compare(standard, priority)).isEqualTo(1);
	}

	@Test
	public void compareWithSimpleSourceProvider() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new StubOrdered(25)));
		assertThat(customComparator.compare(new StubOrdered(10), 5L)).isEqualTo(-1);
	}

	@Test
	public void compareWithSourceProviderArray() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new Object[] {new StubOrdered(10), new StubOrdered(-25)}));
		assertThat(customComparator.compare(5L, new Object())).isEqualTo(-1);
	}

	@Test
	public void compareWithSourceProviderArrayNoMatch() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(5L, new Object[] {new Object(), new Object()}));
		assertThat(customComparator.compare(new Object(), 5L)).isEqualTo(0);
	}

	@Test
	public void compareWithSourceProviderEmpty() {
		Comparator<Object> customComparator = this.comparator.withSourceProvider(
				new TestSourceProvider(50L, new Object()));
		assertThat(customComparator.compare(new Object(), 5L)).isEqualTo(0);
	}


	private static class StubOrdered implements Ordered {

		private final int order;

		StubOrdered(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}
	}

	private static class StubPriorityOrdered implements PriorityOrdered {

		private final int order;

		StubPriorityOrdered(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}
	}

	private static class TestSourceProvider implements OrderComparator.OrderSourceProvider {

		private final Object target;

		private final Object orderSource;

		TestSourceProvider(Object target, Object orderSource) {
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

}
