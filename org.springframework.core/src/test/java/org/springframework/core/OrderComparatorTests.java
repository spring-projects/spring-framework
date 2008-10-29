/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;

import java.util.Comparator;

/**
 * Unit tests for the {@link OrderComparator} class.
 *
 * @author Rick Evans
 */
public final class OrderComparatorTests extends TestCase {

	private Comparator comparator;


	protected void setUp() throws Exception {
		this.comparator = new OrderComparator();
	}


	public void testCompareOrderedInstancesBefore() throws Exception {
		assertEquals(-1, this.comparator.compare(
				new StubOrdered(100), new StubOrdered(2000)));
	}

	public void testCompareOrderedInstancesSame() throws Exception {
		assertEquals(0, this.comparator.compare(
				new StubOrdered(100), new StubOrdered(100)));
	}

	public void testCompareOrderedInstancesAfter() throws Exception {
		assertEquals(1, this.comparator.compare(
				new StubOrdered(982300), new StubOrdered(100)));
	}

	public void testCompareTwoNonOrderedInstancesEndsUpAsSame() throws Exception {
		assertEquals(0, this.comparator.compare(new Object(), new Object()));
	}


	private static final class StubOrdered implements Ordered {

		private final int order;


		public StubOrdered(int order) {
			this.order = order;
		}

		public int getOrder() {
			return this.order;
		}
	}

}
