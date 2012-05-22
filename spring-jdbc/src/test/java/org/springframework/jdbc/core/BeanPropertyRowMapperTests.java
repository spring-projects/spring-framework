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

package org.springframework.jdbc.core;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.ExtendedPerson;
import org.springframework.jdbc.core.test.Person;
import org.springframework.jdbc.core.test.SpacePerson;
import org.springframework.beans.TypeMismatchException;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class BeanPropertyRowMapperTests extends AbstractRowMapperTests {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testOverridingClassDefinedForMapping() {
		BeanPropertyRowMapper mapper = new BeanPropertyRowMapper(Person.class);
		try {
			mapper.setMappedClass(Long.class);
			fail("Setting new class should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
		}
		try {
			mapper.setMappedClass(Person.class);
		}
		catch (InvalidDataAccessApiUsageException ex) {
			fail("Setting same class should not have thrown InvalidDataAccessApiUsageException");
		}
	}

	public void testStaticQueryWithRowMapper() throws SQLException {
		List<Person> result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<Person>(Person.class));
		assertEquals(1, result.size());
		Person bean = result.get(0);
		verifyPerson(bean);
	}

	public void testMappingWithInheritance() throws SQLException {
		List<ConcretePerson> result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ConcretePerson>(ConcretePerson.class));
		assertEquals(1, result.size());
		ConcretePerson bean = result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithNoUnpopulatedFieldsFound() throws SQLException {
		List<ConcretePerson> result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ConcretePerson>(ConcretePerson.class, true));
		assertEquals(1, result.size());
		ConcretePerson bean = result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithUnpopulatedFieldsNotChecked() throws SQLException {
		List<ExtendedPerson> result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ExtendedPerson>(ExtendedPerson.class));
		assertEquals(1, result.size());
		ExtendedPerson bean = result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithUnpopulatedFieldsNotAccepted() throws SQLException {
		try {
			jdbcTemplate.query("select name, age, birth_date, balance from people",
					new BeanPropertyRowMapper<ExtendedPerson>(ExtendedPerson.class, true));
			fail("Should have thrown InvalidDataAccessApiUsageException because of missing field");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}
	}

	public void testMappingNullValue() throws SQLException {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<Person>(Person.class);
		try {
			jdbcTemplate2.query(
				"select name, null as age, birth_date, balance from people", mapper);
			fail("Should have thrown TypeMismatchException because of null value");
		}
		catch (TypeMismatchException ex) {
			// expected
		}
		mapper.setPrimitivesDefaultedForNullValue(true);
		List<Person> result = jdbcTemplate2.query("select name, null as age, birth_date, balance from people",
				mapper);
		assertEquals(1, result.size());
		Person bean = result.get(0);
		verifyPersonWithZeroAge(bean);
	}

	public void testQueryWithSpaceInColumnName() throws SQLException {
		List<SpacePerson> result = jdbcTemplate3.query("select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper<SpacePerson>(SpacePerson.class));
		assertEquals(1, result.size());
		SpacePerson bean = result.get(0);
		verifySpacePerson(bean);
	}

}
