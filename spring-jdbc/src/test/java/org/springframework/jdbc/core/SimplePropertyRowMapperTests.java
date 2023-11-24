/*
 * Copyright 2002-2023 the original author or authors.
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

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.ConstructorPerson;
import org.springframework.jdbc.core.test.ConstructorPersonWithGenerics;
import org.springframework.jdbc.core.test.ConstructorPersonWithSetters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimplePropertyRowMapper}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 */
class SimplePropertyRowMapperTests extends AbstractRowMapperTests {

	@Test
	void staticQueryWithDataClass() throws Exception {
		Mock mock = new Mock();
		ConstructorPerson person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(ConstructorPerson.class));
		verifyPerson(person);

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithDataClassAndGenerics() throws Exception {
		Mock mock = new Mock();
		ConstructorPersonWithGenerics person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(ConstructorPersonWithGenerics.class));
		assertThat(person.name()).isEqualTo("Bubba");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birthDate()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).containsExactly(new BigDecimal("1234.56"));

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithDataClassAndSetters() throws Exception {
		Mock mock = new Mock(MockType.FOUR);
		ConstructorPersonWithSetters person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birthdate, balance from people",
				new SimplePropertyRowMapper<>(ConstructorPersonWithSetters.class));
		assertThat(person.name()).isEqualTo("BUBBA");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birthDate()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithPlainSetters() throws Exception {
		Mock mock = new Mock();
		ConcretePerson person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(ConcretePerson.class));
		verifyPerson(person);

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithDataRecord() throws Exception {
		Mock mock = new Mock();
		RecordPerson person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(RecordPerson.class));
		verifyPerson(person);

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithDataFields() throws Exception {
		Mock mock = new Mock();
		FieldPerson person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(FieldPerson.class));
		verifyPerson(person);

		mock.verifyClosed();
	}

	@Test
	void staticQueryWithIncompleteDataFields() throws Exception {
		Mock mock = new Mock();
		IncompleteFieldPerson person = mock.getJdbcTemplate().queryForObject(
				"select name, age, birth_date, balance from people",
				new SimplePropertyRowMapper<>(IncompleteFieldPerson.class));
		verifyPerson(person);

		mock.verifyClosed();
	}


	protected void verifyPerson(RecordPerson person) {
		assertThat(person.name()).isEqualTo("Bubba");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));
		verifyPersonViaBeanWrapper(person);
	}

	protected void verifyPerson(FieldPerson person) {
		assertThat(person.name).isEqualTo("Bubba");
		assertThat(person.age).isEqualTo(22L);
		assertThat(person.birth_date).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
		assertThat(person.balance).isEqualTo(new BigDecimal("1234.56"));
	}

	protected void verifyPerson(IncompleteFieldPerson person) {
		assertThat(person.name).isEqualTo("Bubba");
		assertThat(person.age).isEqualTo(22L);
		assertThat(person.balance).isEqualTo(new BigDecimal("1234.56"));
	}


	record RecordPerson(String name, long age, Date birth_date, BigDecimal balance) {
	}


	static class FieldPerson {

		String name;
		long age;
		Date birth_date;
		BigDecimal balance;
	}


	static class IncompleteFieldPerson {

		String name;
		long age;
		BigDecimal balance;
	}

}
