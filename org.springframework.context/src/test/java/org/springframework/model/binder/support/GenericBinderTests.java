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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.springframework.context.alert.Severity;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.message.MockMessageSource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResults;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class GenericBinderTests {

	@Test
	public void simpleValues() {
		Person person = new Person();
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("name", "John Doe");
		map.put("age", 42);
		map.put("male", true);
		Binder<Object> binder = new GenericBinder();
		
		BindingResults results = binder.bind(map, person);
		assertEquals(3, results.size());
		assertEquals(3, results.successes().size());
		assertEquals(0, results.failures().size());
		assertEquals(0, results.errors().size());
		assertEquals("name", results.get(0).getFieldName());
		assertEquals("John Doe", results.get(0).getSubmittedValue());
		assertEquals(false, results.get(0).isFailure());
		assertEquals(Severity.INFO, results.get(0).getAlert().getSeverity());
		assertEquals("bindSuccess", results.get(0).getAlert().getCode());
		assertEquals("Successfully bound submitted value John Doe to field 'name'", results.get(0).getAlert().getMessage());
		assertEquals("name", results.get("name").getFieldName());
		assertEquals("John Doe", results.get("name").getSubmittedValue());
		
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
	
	@Test
	public void mapValues() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("jobHistory['0']", "Clerk");
		map.put("jobHistory['1']", "Plumber");
		Binder<Object> binder = new GenericBinder();
		binder.bind(map, person);
		assertEquals("Clerk", person.jobHistory.get(0));
		assertEquals("Plumber", person.jobHistory.get(1));
	}

	@Test
	public void typeMismatch() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("male", "bogus");
		Binder<Object> binder = new GenericBinder();
		BindingResults results = binder.bind(map, person);
		assertEquals(1, results.size());
		assertEquals(0, results.successes().size());
		assertEquals(1, results.failures().size());
		assertEquals(1, results.errors().size());
		assertEquals("bogus", results.get(0).getSubmittedValue());
		assertEquals("typeMismatch", results.get(0).getAlert().getCode());
		assertEquals(Severity.ERROR, results.get(0).getAlert().getSeverity());
		assertEquals("Failed to bind submitted value bogus to field 'male'; value could not be converted to type [boolean]", results.get(0).getAlert().getMessage());
	}
	
	@Test
	public void internalError() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("bogus", "bogus");
		Binder<Object> binder = new GenericBinder();
		BindingResults results = binder.bind(map, person);
		assertEquals(1, results.size());
		assertEquals(0, results.successes().size());
		assertEquals(1, results.failures().size());
		assertEquals(1, results.errors().size());
		assertEquals("bogus", results.get(0).getSubmittedValue());
		assertEquals("internalError", results.get(0).getAlert().getCode());
		assertEquals(Severity.FATAL, results.get(0).getAlert().getSeverity());
		assertEquals("An internal error occurred; message = [EL1034E:(pos 0): A problem occurred whilst attempting to set the property 'bogus': Unable to access property 'bogus' through setter]", results.get(0).getAlert().getMessage());
	}

	@Test
	public void fieldNotEditable() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("readOnly", "whatever");
		Binder<Object> binder = new GenericBinder();
		BindingResults results = binder.bind(map, person);
		assertEquals(1, results.size());
		assertEquals(0, results.successes().size());
		assertEquals(1, results.failures().size());
		assertEquals(0, results.errors().size());
		assertEquals("whatever", results.get(0).getSubmittedValue());
		assertEquals("fieldNotEditable", results.get(0).getAlert().getCode());
		assertEquals(Severity.WARNING, results.get(0).getAlert().getSeverity());
		assertEquals("Failed to bind submitted value whatever; field 'readOnly' is not editable", results.get(0).getAlert().getMessage());
	}

	@Test
	public void messageSource() {
		Person person = new Person();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("male", "bogus");
		GenericBinder binder = new GenericBinder();
		MockMessageSource messageSource = new MockMessageSource();
		messageSource.addMessage("typeMismatch", Locale.US, "Please enter true or false for the value of the #{label} field; you entered #{value}");
		binder.setMessageSource(messageSource);
		LocaleContextHolder.setLocale(Locale.US);
		BindingResults results = binder.bind(map, person);
		assertEquals("Please enter true or false for the value of the male field; you entered bogus", results.get(0).getAlert().getMessage());		
		LocaleContextHolder.setLocale(null);
	}
	
	public static class Person {

		private String name;

		private int age;

		private boolean male;

		private PlaceOfBirth pob;

		private Map<Integer, String> jobHistory;
		
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

		public Map<Integer, String> getJobHistory() {
			return jobHistory;
		}

		public void setJobHistory(Map<Integer, String> jobHistory) {
			this.jobHistory = jobHistory;
		}

		public void setBogus(String bogus) {
			throw new RuntimeException("internal error");
		}
		
		public boolean isReadOnly() {
			return true;
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
