/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.support;

import java.util.Comparator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PropertyComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 */
public class PropertyComparatorTests {

	@Test
	public void testPropertyComparator() {
		Dog dog = new Dog();
		dog.setNickName("mace");

		Dog dog2 = new Dog();
		dog2.setNickName("biscy");

		PropertyComparator<Dog> c = new PropertyComparator<>("nickName", false, true);
		assertTrue(c.compare(dog, dog2) > 0);
		assertTrue(c.compare(dog, dog) == 0);
		assertTrue(c.compare(dog2, dog) < 0);
	}

	@Test
	public void testPropertyComparatorNulls() {
		Dog dog = new Dog();
		Dog dog2 = new Dog();
		PropertyComparator<Dog> c = new PropertyComparator<>("nickName", false, true);
		assertTrue(c.compare(dog, dog2) == 0);
	}

	@Test
	public void testCompoundComparator() {
		Comparator<Dog> c = new PropertyComparator<>("lastName", false, true);

		Dog dog1 = new Dog();
		dog1.setFirstName("macy");
		dog1.setLastName("grayspots");

		Dog dog2 = new Dog();
		dog2.setFirstName("biscuit");
		dog2.setLastName("grayspots");

		assertTrue(c.compare(dog1, dog2) == 0);

		c = c.thenComparing(new PropertyComparator<>("firstName", false, true));
		assertTrue(c.compare(dog1, dog2) > 0);

		dog2.setLastName("konikk dog");
		assertTrue(c.compare(dog2, dog1) > 0);
	}

	@Test
	public void testCompoundComparatorInvert() {
		Comparator<Dog> c = (new PropertyComparator<Dog>("lastName", false, true)).
				thenComparing(new PropertyComparator<>("firstName", false, true));
		Dog dog1 = new Dog();
		dog1.setFirstName("macy");
		dog1.setLastName("grayspots");

		Dog dog2 = new Dog();
		dog2.setFirstName("biscuit");
		dog2.setLastName("grayspots");

		assertTrue(c.compare(dog1, dog2) > 0);
		c = c.reversed();
		assertTrue(c.compare(dog1, dog2) < 0);
	}


	@SuppressWarnings("unused")
	private static class Dog implements Comparable<Object> {

		private String nickName;

		private String firstName;

		private String lastName;

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

		@Override
		public int compareTo(Object o) {
			return this.nickName.compareTo(((Dog) o).nickName);
		}
	}

}
