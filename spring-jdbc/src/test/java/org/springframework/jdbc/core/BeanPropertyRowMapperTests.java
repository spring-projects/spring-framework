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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.DatePerson;
import org.springframework.jdbc.core.test.ExtendedPerson;
import org.springframework.jdbc.core.test.Person;
import org.springframework.jdbc.core.test.SpacePerson;

import static org.junit.Assert.*;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class BeanPropertyRowMapperTests extends AbstractRowMapperTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testOverridingDifferentClassDefinedForMapping() {
		BeanPropertyRowMapper mapper = new BeanPropertyRowMapper(Person.class);
		thrown.expect(InvalidDataAccessApiUsageException.class);
		mapper.setMappedClass(Long.class);
	}

	@Test
	public void testOverridingSameClassDefinedForMapping() {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<Person>(Person.class);
		mapper.setMappedClass(Person.class);
	}

	@Test
	public void testStaticQueryWithRowMapper() throws Exception {
		Mock mock = new Mock();
		List<Person> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<Person>(Person.class));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	public void testMappingWithInheritance() throws Exception {
		Mock mock = new Mock();
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ConcretePerson>(ConcretePerson.class));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	public void testMappingWithNoUnpopulatedFieldsFound() throws Exception {
		Mock mock = new Mock();
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ConcretePerson>(ConcretePerson.class, true));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	public void testMappingWithUnpopulatedFieldsNotChecked() throws Exception {
		Mock mock = new Mock();
		List<ExtendedPerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ExtendedPerson>(ExtendedPerson.class));
		assertEquals(1, result.size());
		ExtendedPerson bean = result.get(0);
		verifyPerson(bean);
		mock.verifyClosed();
	}

	@Test
	public void testMappingWithUnpopulatedFieldsNotAccepted() throws Exception {
		Mock mock = new Mock();
		thrown.expect(InvalidDataAccessApiUsageException.class);
		mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<ExtendedPerson>(ExtendedPerson.class, true));
	}

	@Test
	public void testMappingNullValue() throws Exception {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<Person>(Person.class);
		Mock mock = new Mock(MockType.TWO);
		thrown.expect(TypeMismatchException.class);
		mock.getJdbcTemplate().query(
				"select name, null as age, birth_date, balance from people", mapper);
	}

	@Test
	public void testQueryWithSpaceInColumnNameAndLocalDateTime() throws Exception {
		Mock mock = new Mock(MockType.THREE);
		List<SpacePerson> result = mock.getJdbcTemplate().query(
				"select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper<SpacePerson>(SpacePerson.class));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	public void testQueryWithSpaceInColumnNameAndLocalDate() throws Exception {
		Mock mock = new Mock(MockType.THREE);
		List<DatePerson> result = mock.getJdbcTemplate().query(
				"select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(DatePerson.class));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

}
