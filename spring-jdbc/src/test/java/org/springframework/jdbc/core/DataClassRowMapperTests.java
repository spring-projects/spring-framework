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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.test.ConstructorPerson;
import org.springframework.jdbc.core.test.ConstructorPersonWithGenerics;
import org.springframework.jdbc.core.test.ConstructorPersonWithSetters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 5.3
 */
public class DataClassRowMapperTests extends AbstractRowMapperTests {

	@Test
	public void testStaticQueryWithDataClass() throws Exception {
		Mock mock = new Mock();
		List<ConstructorPerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new DataClassRowMapper<>(ConstructorPerson.class));
		assertThat(result.size()).isEqualTo(1);
		verifyPerson(result.get(0));

		mock.verifyClosed();
	}

	@Test
	public void testStaticQueryWithDataClassAndGenerics() throws Exception {
		Mock mock = new Mock();
		List<ConstructorPersonWithGenerics> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				new DataClassRowMapper<>(ConstructorPersonWithGenerics.class));
		assertThat(result.size()).isEqualTo(1);
		ConstructorPersonWithGenerics person = result.get(0);
		assertThat(person.name()).isEqualTo("Bubba");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birthDate()).usingComparator(Date::compareTo).isEqualTo(new java.util.Date(1221222L));
		assertThat(person.balance()).isEqualTo(Collections.singletonList(new BigDecimal("1234.56")));

		mock.verifyClosed();
	}

	@Test
	public void testStaticQueryWithDataClassAndSetters() throws Exception {
		Mock mock = new Mock(MockType.FOUR);
		List<ConstructorPersonWithSetters> result = mock.getJdbcTemplate().query(
				"select name, age, birthdate, balance from people",
				new DataClassRowMapper<>(ConstructorPersonWithSetters.class));
		assertThat(result.size()).isEqualTo(1);
		ConstructorPersonWithSetters person = result.get(0);
		assertThat(person.name()).isEqualTo("BUBBA");
		assertThat(person.age()).isEqualTo(22L);
		assertThat(person.birthDate()).usingComparator(Date::compareTo).isEqualTo(new java.util.Date(1221222L));
		assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));

		mock.verifyClosed();
	}

}
