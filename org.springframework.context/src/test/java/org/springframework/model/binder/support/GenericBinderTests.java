/*
 * Copyright 2004-2009 the original author or authors.
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

package org.springframework.model.binder.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.style.ToStringCreator;
import org.springframework.model.binder.Binder;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class GenericBinderTests {

	@Test
	public void simpleValues() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "John Doe");
		map.put("age", 42);
		map.put("male", true);
		Binder <Object> binder = new GenericBinder();
		binder.bind(map, person);
		assertEquals("John Doe", person.name);
		assertEquals(42, person.age);
		assertTrue(person.male);
	}

	@Test
	public void nestedValues() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("pob.city", "Rome");
		map.put("pob.country", "Italy");
		Binder<Object> binder = new GenericBinder();
		binder.bind(map, person);
		assertNotNull(person.pob);
		assertEquals("Rome", person.pob.city);
		assertEquals("Italy", person.pob.country);
	}


	public static class Person {

		private String name;

		private int age;

		private boolean male;

		private PlaceOfBirth pob;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public boolean isMale() {
			return male;
		}

		public void setMale(boolean male) {
			this.male = male;
		}

		public PlaceOfBirth getPob() {
			return pob;
		}

		public void setPob(PlaceOfBirth pob) {
			this.pob = pob;
		}

		public String toString() {
			return new ToStringCreator(this)
					.append("name", name)
					.append("age", age)
					.append("male", male)
					.append("pob", pob)
					.toString();
		}
	}


	public static class PlaceOfBirth {

		private String city;

		private String country;

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String toString() {
			return new ToStringCreator(this)
					.append("city", city)
					.append("country", country)
					.toString();
		}
	}

}
