/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * @author Juergen Hoeller
 * @since 02.08.2004
 */
public class RowMapperTests extends TestCase {

	private Connection connection;
	private Statement statement;
	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	private JdbcTemplate template;

	private List<TestBean> result;

	@Before
	public void setUp() throws SQLException {
		connection = mock(Connection.class);
		statement = mock(Statement.class);
		preparedStatement = mock(PreparedStatement.class);
		resultSet = mock(ResultSet.class);
		given(connection.createStatement()).willReturn(statement);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(statement.executeQuery(anyString())).willReturn(resultSet);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getString(1)).willReturn("tb1", "tb2");
		given(resultSet.getInt(2)).willReturn(1, 2);
		template = new JdbcTemplate();
		template.setDataSource(new SingleConnectionDataSource(connection, false));
		template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		template.afterPropertiesSet();
	}

	@After
	public void verifyClosed() throws Exception {
		verify(resultSet).close();
		verify(connection).close();
	}

	@After
	public void verifyResults() {
		assertTrue(result != null);
		assertEquals(2, result.size());
		assertEquals("tb1", result.get(0).getName());
		assertEquals("tb2", result.get(1).getName());
		assertEquals(1, result.get(0).getAge());
		assertEquals(2, result.get(1).getAge());
	}

	@Test
	public void testStaticQueryWithRowMapper() throws SQLException {
		result = template.query("some SQL", new TestRowMapper());
		verify(statement).close();
	}

	@Test
	public void testPreparedStatementCreatorWithRowMapper() throws SQLException {
		result = template.query(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection con)
					throws SQLException {
				return preparedStatement;
			}
		}, new TestRowMapper());
		verify(preparedStatement).close();
	}

	@Test
	public void testPreparedStatementSetterWithRowMapper() throws SQLException {
		result = template.query("some SQL", new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, "test");
			}
		}, new TestRowMapper());
		verify(preparedStatement).setString(1, "test");
		verify(preparedStatement).close();
	}

	@Test
	public void testQueryWithArgsAndRowMapper() throws SQLException {
		result = template.query("some SQL",
				new Object[] { "test1", "test2" },
				new TestRowMapper());
		preparedStatement.setString(1, "test1");
		preparedStatement.setString(2, "test2");
		preparedStatement.close();
	}

	@Test
	public void testQueryWithArgsAndTypesAndRowMapper() throws SQLException {
		result = template.query("some SQL",
				new Object[] { "test1", "test2" },
				new int[] { Types.VARCHAR, Types.VARCHAR },
				new TestRowMapper());
		verify(preparedStatement).setString(1, "test1");
		verify(preparedStatement).setString(2, "test2");
		verify(preparedStatement).close();
	}

	private static class TestRowMapper implements RowMapper<TestBean> {
		@Override
		public TestBean mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new TestBean(rs.getString(1), rs.getInt(2));
		}
	}

}
