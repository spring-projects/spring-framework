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

import java.util.Comparator;

import junit.framework.TestCase;

import org.springframework.beans.support.PropertyComparator;

/**
 * @author Keith Donald
 */
public class ComparatorTests extends TestCase {

	public void testComparableComparator() {
		Comparator c = new ComparableComparator();
		String s1 = "abc";
		String s2 = "cde";
		assertTrue(c.compare(s1, s2) < 0);
	}

	public void testComparableComparatorIllegalArgs() {
		Comparator c = new ComparableComparator();
		Object o1 = new Object();
		Object o2 = new Object();
		try {
			c.compare(o1, o2);
		}
		catch (IllegalArgumentException e) {
			return;
		}
		fail("Comparator should have thrown a cce");
	}

	public void testBooleanComparatorTrueLow() {
		Comparator c = BooleanComparator.TRUE_LOW;
		assertTrue(c.compare(new Boolean(true), new Boolean(false)) < 0);
	}

	public void testBooleanComparatorTrueHigh() {
		Comparator c = BooleanComparator.TRUE_HIGH;
		assertTrue(c.compare(new Boolean(true), new Boolean(false)) > 0);
		assertTrue(c.compare(Boolean.TRUE, Boolean.TRUE) == 0);
	}

	public void testPropertyComparator() {
		Dog dog = new Dog();
		dog.setNickName("mace");

		Dog dog2 = new Dog();
		dog2.setNickName("biscy");

		PropertyComparator c = new PropertyComparator("nickName", false, true);
		assertTrue(c.compare(dog, dog2) > 0);
		assertTrue(c.compare(dog, dog) == 0);
		assertTrue(c.compare(dog2, dog) < 0);
	}

	public void testPropertyComparatorNulls() {
		Dog dog = new Dog();
		Dog dog2 = new Dog();
		PropertyComparator c = new PropertyComparator("nickName", false, true);
		assertTrue(c.compare(dog, dog2) == 0);
	}

	public void testNullSafeComparatorNullsLow() {
		Comparator c = NullSafeComparator.NULLS_LOW;
		assertTrue(c.compare(null, "boo") < 0);
	}

	public void testNullSafeComparatorNullsHigh() {
		Comparator c = NullSafeComparator.NULLS_HIGH;
		assertTrue(c.compare(null, "boo") > 0);
		assertTrue(c.compare(null, null) == 0);
	}

	public void testCompoundComparatorEmpty() {
		Comparator c = new CompoundComparator();
		try {
			c.compare("foo", "bar");
		}
		catch (IllegalStateException e) {
			return;
		}
		fail("illegal state should've been thrown on empty list");
	}

	public void testCompoundComparator() {
		CompoundComparator c = new CompoundComparator();
		c.addComparator(new PropertyComparator("lastName", false, true));

		Dog dog1 = new Dog();
		dog1.setFirstName("macy");
		dog1.setLastName("grayspots");

		Dog dog2 = new Dog();
		dog2.setFirstName("biscuit");
		dog2.setLastName("grayspots");

		assertTrue(c.compare(dog1, dog2) == 0);

		c.addComparator(new PropertyComparator("firstName", false, true));
		assertTrue(c.compare(dog1, dog2) > 0);

		dog2.setLastName("konikk dog");
		assertTrue(c.compare(dog2, dog1) > 0);
	}

	public void testCompoundComparatorInvert() {
		CompoundComparator c = new CompoundComparator();
		c.addComparator(new PropertyComparator("lastName", false, true));
		c.addComparator(new PropertyComparator("firstName", false, true));
		Dog dog1 = new Dog();
		dog1.setFirstName("macy");
		dog1.setLastName("grayspots");

		Dog dog2 = new Dog();
		dog2.setFirstName("biscuit");
		dog2.setLastName("grayspots");

		assertTrue(c.compare(dog1, dog2) > 0);
		c.invertOrder();
		assertTrue(c.compare(dog1, dog2) < 0);
	}


	private static class Dog implements Comparable {

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
