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

package org.springframework.beans;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractConfigurablePropertyAccessorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	protected abstract ConfigurablePropertyAccessor createAccessor(Object target);

	@Test
	public void isReadableProperty() {
		ConfigurablePropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isReadableProperty("name"), is(true));
	}

	@Test
	public void isReadablePropertyNotReadable() {
		ConfigurablePropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isReadableProperty("age"));
	}

	/**
	 * Shouldn't throw an exception: should just return false
	 */
	@Test
	public void isReadablePropertyNoSuchProperty() {
		ConfigurablePropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isReadableProperty("xxxxx"));
	}

	@Test
	public void isReadablePropertyNull() {
		ConfigurablePropertyAccessor accessor = createAccessor(new NoRead());

		thrown.expect(IllegalArgumentException.class);
		accessor.isReadableProperty(null);
	}

	@Test
	public void isWritableProperty() {
		ConfigurablePropertyAccessor accessor = createAccessor(new Simple("John", 2));

		assertThat(accessor.isWritableProperty("name"), is(true));
	}

	@Test
	public void isWritablePropertyNull() {
		ConfigurablePropertyAccessor accessor = createAccessor(new NoRead());

		thrown.expect(IllegalArgumentException.class);
		accessor.isWritableProperty(null);
	}

	@Test
	public void isWritablePropertyNoSuchProperty() {
		ConfigurablePropertyAccessor accessor = createAccessor(new NoRead());

		assertFalse(accessor.isWritableProperty("xxxxx"));
	}

	@Test
	public void getSimpleProperty() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);
		assertThat(accessor.getPropertyValue("name"), is("John"));
	}

	@Test
	public void getNestedProperty() {
		Person person = createPerson("John", "London", "UK");
		ConfigurablePropertyAccessor accessor = createAccessor(person);
		assertThat(accessor.getPropertyValue("address.city"), is("London"));
	}

	@Test
	public void getNestedDeepProperty() {
		Person person = createPerson("John", "London", "UK");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		assertThat(accessor.getPropertyValue("address.country.name"), is("UK"));
	}

	@Test
	public void getPropertyIntermediateFieldIsNull() {
		Person person = createPerson("John", "London", "UK");
		person.address = null;
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		try {
			accessor.getPropertyValue("address.country.name");
			fail("Should have failed to get value with null intermediate path");
		}
		catch (NullValueInNestedPathException e) {
			assertEquals("address", e.getPropertyName());
			assertEquals(Person.class, e.getBeanClass());
		}
	}

	@Test
	public void getPropertyIntermediateFieldIsNullWithAutoGrow() {
		Person person = createPerson("John", "London", "UK");
		person.address = null;
		ConfigurablePropertyAccessor accessor = createAccessor(person);
		accessor.setAutoGrowNestedPaths(true);

		assertEquals("DefaultCountry", accessor.getPropertyValue("address.country.name"));
	}

	@Test
	public void getUnknownField() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);

		try {
			accessor.getPropertyValue("foo");
			fail("Should have failed to get an unknown field.");
		}
		catch (NotReadablePropertyException e) {
			assertEquals(Simple.class, e.getBeanClass());
			assertEquals("foo", e.getPropertyName());
		}
	}

	@Test
	public void getUnknownNestedField() {
		Person person = createPerson("John", "London", "UK");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		thrown.expect(NotReadablePropertyException.class);
		accessor.getPropertyValue("address.bar");
	}

	@Test
	public void setSimpleProperty() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);

		accessor.setPropertyValue("name", "SomeValue");

		assertThat(simple.name, is("SomeValue"));
		assertThat(simple.getName(), is("SomeValue"));
	}

	@Test
	public void setNestedProperty() {
		Person person = createPerson("John", "Paris", "FR");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		accessor.setPropertyValue("address.city", "London");
		assertThat(person.address.city, is("London"));
	}

	@Test
	public void setNestedDeepProperty() {
		Person person = createPerson("John", "Paris", "FR");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(person.address.country.name, is("UK"));
	}

	@Test
	public void setPropertyIntermediateFieldIsNull() {
		Person person = createPerson("John", "Paris", "FR");
		person.address.country = null;
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		try {
			accessor.setPropertyValue("address.country.name", "UK");
			fail("Should have failed to set value with intermediate null value");
		}
		catch (NullValueInNestedPathException e) {
			assertEquals("address.country", e.getPropertyName());
			assertEquals(Person.class, e.getBeanClass());
		}
		assertThat(person.address.country, is(nullValue())); // Not touched
	}

	@Test
	public void setPropertyIntermediateFieldIsNullWithAutoGrow() {
		Person person = createPerson("John", "Paris", "FR");
		person.address.country = null;
		ConfigurablePropertyAccessor accessor = createAccessor(person);
		accessor.setAutoGrowNestedPaths(true);

		accessor.setPropertyValue("address.country.name", "UK");
		assertThat(person.address.country.name, is("UK"));
	}

	@Test
	public void setUnknownField() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);

		try {
			accessor.setPropertyValue("foo", "value");
			fail("Should have failed to set an unknown field.");
		}
		catch (NotWritablePropertyException e) {
			assertEquals(Simple.class, e.getBeanClass());
			assertEquals("foo", e.getPropertyName());
		}
	}

	@Test
	public void setUnknownNestedField() {
		Person person = createPerson("John", "Paris", "FR");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		thrown.expect(NotWritablePropertyException.class);
		accessor.setPropertyValue("address.bar", "value");
	}


	@Test
	public void propertyType() {
		Person person = createPerson("John", "Paris", "FR");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		assertEquals(String.class, accessor.getPropertyType("address.city"));
	}

	@Test
	public void propertyTypeUnknownField() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);

		assertThat(accessor.getPropertyType("foo"), is(nullValue()));
	}

	@Test
	public void propertyTypeDescriptor() {
		Person person = createPerson("John", "Paris", "FR");
		ConfigurablePropertyAccessor accessor = createAccessor(person);

		assertThat(accessor.getPropertyTypeDescriptor("address.city"), is(notNullValue()));
	}

	@Test
	public void propertyTypeDescriptorUnknownField() {
		Simple simple = new Simple("John", 2);
		ConfigurablePropertyAccessor accessor = createAccessor(simple);

		assertThat(accessor.getPropertyTypeDescriptor("foo"), is(nullValue()));
	}


	private Person createPerson(String name, String city, String country) {
		return new Person(name, new Address(city, country));
	}


	private static class Simple {

		private String name;

		private Integer integer;

		private Simple(String name, Integer integer) {
			this.name = name;
			this.integer = integer;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getInteger() {
			return integer;
		}

		public void setInteger(Integer integer) {
			this.integer = integer;
		}
	}

	private static class Person {
		private String name;

		private Address address;

		private Person(String name, Address address) {
			this.name = name;
			this.address = address;
		}

		public Person() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	private static class Address {
		private String city;

		private Country country;

		private Address(String city, String country) {
			this.city = city;
			this.country = new Country(country);
		}

		public Address() {
			this("DefaultCity", "DefaultCountry");
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	}

	private static class Country {
		private String name;

		public Country(String name) {
			this.name = name;
		}

		public Country() {
			this(null);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@SuppressWarnings("unused")
	static class NoRead {

		public void setAge(int age) {
		}
	}
}
