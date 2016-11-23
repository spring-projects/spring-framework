/*
 * Copyright 2002-2016 the original author or authors.
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.DatePerson;
import org.springframework.jdbc.core.test.Person;
import org.springframework.jdbc.core.test.SpacePerson;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Mock object based abstract class for RowMapper tests.
 * Initializes mock objects and verifies results.
 *
 * @author Thomas Risberg
 */
public abstract class AbstractRowMapperTests {

	protected void verifyPerson(Person bean) throws Exception {
		assertEquals("Bubba", bean.getName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirth_date());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifyPerson(ConcretePerson bean) throws Exception {
		assertEquals("Bubba", bean.getName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirth_date());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifyPerson(SpacePerson bean) {
		assertEquals("Bubba", bean.getLastName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.sql.Timestamp(1221222L).toLocalDateTime(), bean.getBirthDate());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifyPerson(DatePerson bean) {
		assertEquals("Bubba", bean.getLastName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.sql.Date(1221222L).toLocalDate(), bean.getBirthDate());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}


	protected enum MockType {ONE, TWO, THREE};


	protected static class Mock {

		private Connection connection;

		private ResultSetMetaData resultSetMetaData;

		private ResultSet resultSet;

		private Statement statement;

		private JdbcTemplate jdbcTemplate;

		public Mock() throws Exception {
			this(MockType.ONE);
		}

		@SuppressWarnings("unchecked")
		public Mock(MockType type) throws Exception {
			connection = mock(Connection.class);
			statement = mock(Statement.class);
			resultSet = mock(ResultSet.class);
			resultSetMetaData = mock(ResultSetMetaData.class);

			given(connection.createStatement()).willReturn(statement);
			given(statement.executeQuery(anyString())).willReturn(resultSet);
			given(resultSet.getMetaData()).willReturn(resultSetMetaData);

			given(resultSet.next()).willReturn(true, false);
			given(resultSet.getString(1)).willReturn("Bubba");
			given(resultSet.getLong(2)).willReturn(22L);
			given(resultSet.getTimestamp(3)).willReturn(new Timestamp(1221222L));
			given(resultSet.getObject(anyInt(), any(Class.class))).willThrow(new SQLFeatureNotSupportedException());
			given(resultSet.getDate(3)).willReturn(new java.sql.Date(1221222L));
			given(resultSet.getBigDecimal(4)).willReturn(new BigDecimal("1234.56"));
			given(resultSet.wasNull()).willReturn(type == MockType.TWO);

			given(resultSetMetaData.getColumnCount()).willReturn(4);
			given(resultSetMetaData.getColumnLabel(1)).willReturn(
					type == MockType.THREE ? "Last Name" : "name");
			given(resultSetMetaData.getColumnLabel(2)).willReturn("age");
			given(resultSetMetaData.getColumnLabel(3)).willReturn("birth_date");
			given(resultSetMetaData.getColumnLabel(4)).willReturn("balance");

			jdbcTemplate = new JdbcTemplate();
			jdbcTemplate.setDataSource(new SingleConnectionDataSource(connection, false));
			jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
			jdbcTemplate.afterPropertiesSet();
		}

		public JdbcTemplate getJdbcTemplate() {
			return jdbcTemplate;
		}

		public void verifyClosed() throws Exception {
			verify(resultSet).close();
			verify(statement).close();
		}
	}

}
