/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.util.comparator;

import static org.junit.Assert.*;

import java.util.Comparator;

import org.junit.Test;

/**
 * Unit tests for {@link PropertyComparator}
 * 
 * @see org.springframework.beans.support.PropertyComparatorTests
 * 
 * @author Keith Donald
 * @author Chris Beams
 */
public class ComparatorTests {

	@Test
	public void testComparableComparator() {
		Comparator<String> c = new ComparableComparator<String>();
		String s1 = "abc";
		String s2 = "cde";
		assertTrue(c.compare(s1, s2) < 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testComparableComparatorIllegalArgs() {
		Comparator c = new ComparableComparator();
		Object o1 = new Object();
		Object o2 = new Object();
		try {
			c.compare(o1, o2);
		}
		catch (ClassCastException e) {
			return;
		}
		fail("Comparator should have thrown a cce");
	}

	@Test
	public void testBooleanComparatorTrueLow() {
		Comparator<Boolean> c = BooleanComparator.TRUE_LOW;
		assertTrue(c.compare(new Boolean(true), new Boolean(false)) < 0);
	}

	@Test
	public void testBooleanComparatorTrueHigh() {
		Comparator<Boolean> c = BooleanComparator.TRUE_HIGH;
		assertTrue(c.compare(new Boolean(true), new Boolean(false)) > 0);
		assertTrue(c.compare(Boolean.TRUE, Boolean.TRUE) == 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNullSafeComparatorNullsLow() {
		Comparator<String> c = NullSafeComparator.NULLS_LOW;
		assertTrue(c.compare(null, "boo") < 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNullSafeComparatorNullsHigh() {
		Comparator<String> c = NullSafeComparator.NULLS_HIGH;
		assertTrue(c.compare(null, "boo") > 0);
		assertTrue(c.compare(null, null) == 0);
	}

	@Test
	public void testCompoundComparatorEmpty() {
		Comparator<String> c = new CompoundComparator<String>();
		try {
			c.compare("foo", "bar");
		}
		catch (IllegalStateException e) {
			return;
		}
		fail("illegal state should've been thrown on empty list");
	}

	private static class Dog implements Comparable<Object> {

		private String nickName;

		private String firstName;

		private String lastName;

		public int compareTo(Object o) {
			return nickName.compareTo(((Dog)o).nickName);
		}

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

}
