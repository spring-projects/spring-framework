/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.DatePerson;
import org.springframework.jdbc.core.test.EmailPerson;
import org.springframework.jdbc.core.test.ExtendedPerson;
import org.springframework.jdbc.core.test.Person;
import org.springframework.jdbc.core.test.SpacePerson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link BeanPropertyRowMapper}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class BeanPropertyRowMapperTests extends AbstractRowMapperTests {

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void overridingDifferentClassDefinedForMapping() {
		BeanPropertyRowMapper mapper = new BeanPropertyRowMapper(Person.class);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				mapper.setMappedClass(Long.class));
	}

	@Test
	void overridingSameClassDefinedForMapping() {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
		assertThatNoException().isThrownBy(() -> mapper.setMappedClass(Person.class));
	}

	@Test
	void staticQueryWithRowMapper() throws Exception {
		Mock mock = new Mock();
		List<Person> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(Person.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void mappingWithInheritance() throws Exception {
		Mock mock = new Mock();
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(ConcretePerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void mappingWithNoUnpopulatedFieldsFound() throws Exception {
		Mock mock = new Mock();
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(ConcretePerson.class, true));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void mappingWithUnpopulatedFieldsNotChecked() throws Exception {
		Mock mock = new Mock();
		List<ExtendedPerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(ExtendedPerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void mappingWithUnpopulatedFieldsNotAccepted() throws Exception {
		Mock mock = new Mock();
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				mock.getJdbcTemplate().query("select name, age, birth_date, balance from people",
						new BeanPropertyRowMapper<>(ExtendedPerson.class, true)));
	}

	@Test
	void mappingNullValue() throws Exception {
		BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
		Mock mock = new Mock(MockType.TWO);
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				mock.getJdbcTemplate().query("select name, null as age, birth_date, balance from people", mapper));
	}

	@Test
	void queryWithSpaceInColumnNameAndLocalDateTime() throws Exception {
		Mock mock = new Mock(MockType.THREE);
		List<SpacePerson> result = mock.getJdbcTemplate().query(
				"select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(SpacePerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void queryWithSpaceInColumnNameAndLocalDate() throws Exception {
		Mock mock = new Mock(MockType.THREE);
		List<DatePerson> result = mock.getJdbcTemplate().query(
				"select last_name as \"Last Name\", age, birth_date, balance from people",
				new BeanPropertyRowMapper<>(DatePerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void queryWithDirectNameMatchOnBirthDate() throws Exception {
		Mock mock = new Mock(MockType.FOUR);
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birthdate, balance from people",
				new BeanPropertyRowMapper<>(ConcretePerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	void queryWithUnderscoreInColumnNameAndPersonWithMultipleAdjacentUppercaseLettersInPropertyName() throws Exception {
		Mock mock = new Mock();
		List<EmailPerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance, e_mail from people",
				new BeanPropertyRowMapper<>(EmailPerson.class));
		assertThat(result).hasSize(1);
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@ParameterizedTest
	@CsvSource({
		"age, age",
		"lastName, last_name",
		"Name, name",
		"FirstName, first_name",
		"EMail, e_mail",
		"URL, u_r_l", // likely undesirable, but that's the status quo
	})
	void underscoreName(String input, String expected) {
		BeanPropertyRowMapper<?> mapper = new BeanPropertyRowMapper<>(Object.class);
		assertThat(mapper.underscoreName(input)).isEqualTo(expected);
	}

}
