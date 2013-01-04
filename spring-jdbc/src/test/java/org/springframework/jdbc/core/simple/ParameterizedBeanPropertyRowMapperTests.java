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

package org.springframework.jdbc.core.simple;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.AbstractRowMapperTests;
import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.Person;

/**
 * @author Thomas Risberg
 */
public class ParameterizedBeanPropertyRowMapperTests extends AbstractRowMapperTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testOverridingDifferentClassDefinedForMapping() {
		ParameterizedBeanPropertyRowMapper mapper = ParameterizedBeanPropertyRowMapper.newInstance(Person.class);
		thrown.expect(InvalidDataAccessApiUsageException.class);
		mapper.setMappedClass(Long.class);
	}

	@Test
	public void testOverridingSameClassDefinedForMapping() {
		ParameterizedBeanPropertyRowMapper<Person> mapper = ParameterizedBeanPropertyRowMapper.newInstance(Person.class);
		mapper.setMappedClass(Person.class);
	}

	@Test
	public void testStaticQueryWithRowMapper() throws Exception {
		Mock mock = new Mock();
		List<Person> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				ParameterizedBeanPropertyRowMapper.newInstance(Person.class));
		assertEquals(1, result.size());
		verifyPerson(result.get(0));
		mock.verifyClosed();
	}

	@Test
	public void testMappingWithInheritance() throws Exception {
		Mock mock = new Mock();
		List<ConcretePerson> result = mock.getJdbcTemplate().query(
				"select name, age, birth_date, balance from people",
				ParameterizedBeanPropertyRowMapper.newInstance(ConcretePerson.class));
		assertEquals(1, result.size());
		verifyConcretePerson(result.get(0));
		mock.verifyClosed();
	}

}
