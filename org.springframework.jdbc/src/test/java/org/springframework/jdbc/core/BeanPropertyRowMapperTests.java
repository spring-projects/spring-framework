/*
 * Copyright 2002-2008 the original author or authors.
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
		List result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper(Person.class));
		assertEquals(1, result.size());
		Person bean = (Person) result.get(0);
		verifyPerson(bean);
	}

	public void testMappingWithInheritance() throws SQLException {
		List result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper(ConcretePerson.class));
		assertEquals(1, result.size());
		ConcretePerson bean = (ConcretePerson) result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithNoUnpopulatedFieldsFound() throws SQLException {
		List result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper(ConcretePerson.class, true));
		assertEquals(1, result.size());
		ConcretePerson bean = (ConcretePerson) result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithUnpopulatedFieldsNotChecked() throws SQLException {
		List result = jdbcTemplate.query("select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper(ExtendedPerson.class));
		assertEquals(1, result.size());
		ExtendedPerson bean = (ExtendedPerson) result.get(0);
		verifyConcretePerson(bean);
	}

	public void testMappingWithUnpopulatedFieldsNotAccepted() throws SQLException {
		try {
			List result = jdbcTemplate.query("select name, age, birth_date, balance from people",
					new BeanPropertyRowMapper(ExtendedPerson.class, true));
			fail("Should have thrown InvalidDataAccessApiUsageException because of missing field");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}
	}

	public void testMappingNullValue() throws SQLException {
		BeanPropertyRowMapper mapper = new BeanPropertyRowMapper(Person.class);
		try {
			List result1 = jdbcTemplate2.query("select name, null as age, birth_date, balance from people",
					mapper);
			fail("Should have thrown TypeMismatchException because of null value");
		}
		catch (TypeMismatchException ex) {
			// expected
		}
		mapper.setPrimitivesDefaultedForNullValue(true);
		List result2 = jdbcTemplate2.query("select name, null as age, birth_date, balance from people",
				mapper);
		assertEquals(1, result2.size());
		Person bean = (Person) result2.get(0);
		verifyPersonWithZeroAge(bean);
	}

	public void testQueryWithSpaceInColumnName() throws SQLException {
		List result = jdbcTemplate3.query("select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper(SpacePerson.class));
		assertEquals(1, result.size());
		SpacePerson bean = (SpacePerson) result.get(0);
		verifySpacePerson(bean);
	}

}
