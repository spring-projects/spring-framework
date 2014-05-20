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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link DirectFieldAccessor}
 *
 * @author Jose Luis Martin
 * @author Chris Beams
 * @author Maciej Walkowiak
 */
public class DirectFieldAccessorTests {

	private Person person;
	private DirectFieldAccessor accessor;

	@Before
	public void setup() {
		person = new Person();
		accessor = new DirectFieldAccessor(person);
	}

	@Test
	public void testGettingSimpleProperty(){
		person.name = "John";

		assertThat(accessor.getPropertyValue("name"), is("John"));
	}

	@Test
	public void testGettingNestedProperty() {
		person.name = "John";
		person.address = new Address();
		person.address.city = "London";

		assertThat(accessor.getPropertyValue("address.city"), is("London"));
	}

	@Test(expected = NotReadablePropertyException.class)
	public void testGettingNonExistingField() {
		accessor.getPropertyValue("foo");
	}

	@Test
	public void testSettingSimpleProperty() {
		accessor.setPropertyValue("name", "John");

		assertThat(person.name, is("John"));
	}

	@Test
	public void testSettingNestedProperty() {
		accessor.setPropertyValue("address.city", "London");

		assertThat(person.address.city, is("London"));
	}

	@Test(expected = NotWritablePropertyException.class)
	public void testSettingNonExistingField() {
		accessor.setPropertyValue("foo", "bar");
	}

	@Test
	public void testSettingNestedPropertyWhenDefaultConstructorNotDefined() {
		accessor.setPropertyValue("address.country.name", "United Kingdom");

		assertThat(person.address.country.name, is("United Kingdom"));
	}

	@Test
	public void testSettingNestedPropertyAsObject() {
		accessor.setPropertyValue("address.country", new Country("United Kingdom"));

		assertThat(person.address.country.name, is("United Kingdom"));
	}

	@Test
	public void testNonReadableProperty() {
		assertThat(accessor.isReadableProperty("foo"), is(false));
	}

	@Test
	public void testNonwritableProperty() {
		assertThat(accessor.isWritableProperty("foo"), is(false));
	}

	@Test
	public void testReadableProperty() {
		assertThat(accessor.isReadableProperty("name"), is(true));
	}

	@Test
	public void testWritableProperty() {
		assertThat(accessor.isWritableProperty("name"), is(true));
	}

	@Test
	public void returnsNullPropertyDescriptorWhenFieldNotFound() {
		assertThat(accessor.getPropertyTypeDescriptor("foo"), is(nullValue()));
	}

	@Test
	public void returnsPropertyDescriptorForExistingField() {
		assertThat(accessor.getPropertyTypeDescriptor("address.city"), is(notNullValue()));
	}

	public static class Person {
		private String name;
		private Address address;
	}

	public static class Address {
		private String city;
		private Country country;
	}

	public static class Country {
		private String name;

		public Country(String name) {
			this.name = name;
		}
	}
}
