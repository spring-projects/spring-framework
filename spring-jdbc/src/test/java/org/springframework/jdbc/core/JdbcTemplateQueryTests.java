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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 19.12.2004
 */
public class JdbcTemplateQueryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Connection connection;
	private DataSource dataSource;
	private Statement statement;
	private PreparedStatement preparedStatement;
	private ResultSet resultSet;
	private ResultSetMetaData resultSetMetaData;
	private JdbcTemplate template;

	@Before
	public void setUp() throws Exception {
		this.connection = mock(Connection.class);
		this.dataSource = mock(DataSource.class);
		this.statement = mock(Statement.class);
		this.preparedStatement = mock(PreparedStatement.class);
		this.resultSet = mock(ResultSet.class);
		this.resultSetMetaData = mock(ResultSetMetaData.class);
		this.template = new JdbcTemplate(this.dataSource);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSetMetaData.getColumnCount()).willReturn(1);
		given(this.resultSetMetaData.getColumnLabel(1)).willReturn("age");
		given(this.connection.createStatement()).willReturn(this.statement);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.statement.executeQuery(anyString())).willReturn(this.resultSet);
	}

	@Test
	public void testQueryForList() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getObject(1)).willReturn(11, 12);
		List li = this.template.queryForList(sql);
		assertEquals("All rows returned", 2, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
		assertEquals("Second row is Integer", 12, ((Integer)((Map)li.get(1)).get("age")).intValue());
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForListWithEmptyResult() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(false);
		List li = this.template.queryForList(sql);
		assertEquals("All rows returned", 0, li.size());
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForListWithSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		List li = this.template.queryForList(sql);
		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForListWithIntegerElement() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(11);
		List li = this.template.queryForList(sql, Integer.class);
		assertEquals("All rows returned", 1, li.size());
		assertEquals("Element is Integer", 11, ((Integer) li.get(0)).intValue());
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForMapWithSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		Map map = this.template.queryForMap(sql);
		assertEquals("Wow is Integer", 11, ((Integer) map.get("age")).intValue());
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectThrowsIncorrectResultSizeForMoreThanOneRow() throws Exception {
		String sql = "select pass from t_account where first_name='Alef'";
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getString(1)).willReturn("pass");
		this.thrown.expect(IncorrectResultSizeDataAccessException.class);
		try {
			this.template.queryForObject(sql, String.class);
		} finally {
			verify(this.resultSet).close();
			verify(this.statement).close();
		}
	}

	@Test
	public void testQueryForObjectWithRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, new RowMapper<Integer>() {
			@Override
			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}
		});
		assertTrue("Correct result type", o instanceof Integer);
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectWithString() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getString(1)).willReturn("myvalue");
		assertEquals("myvalue", this.template.queryForObject(sql, String.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectWithBigInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn("22");
		assertEquals(new BigInteger("22"), this.template.queryForObject(sql, BigInteger.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectWithBigDecimal() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getBigDecimal(1)).willReturn(new BigDecimal(22.5));
		assertEquals(new BigDecimal(22.5), this.template.queryForObject(sql, BigDecimal.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectWithInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		assertEquals(new Integer(22), this.template.queryForObject(sql, Integer.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForObjectWithIntegerAndNull() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(0);
		given(this.resultSet.wasNull()).willReturn(true);
		assertNull(this.template.queryForObject(sql, Integer.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForInt() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		int i = this.template.queryForInt(sql);
		assertEquals("Return of an int", 22, i);
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForLong() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);
		long l = this.template.queryForLong(sql);
		assertEquals("Return of a long", 87, l);
		verify(this.resultSet).close();
		verify(this.statement).close();
	}

	@Test
	public void testQueryForListWithArgs() throws Exception {
		doTestQueryForListWithArgs("SELECT AGE FROM CUSTMR WHERE ID < ?");
	}

	@Test
	public void testQueryForListIsNotConfusedByNamedParameterPrefix() throws Exception {
		doTestQueryForListWithArgs("SELECT AGE FROM PREFIX:CUSTMR WHERE ID < ?");
	}

	private void doTestQueryForListWithArgs(String sql) throws Exception {
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getObject(1)).willReturn(11, 12);
		List li = this.template.queryForList(sql, new Object[] {new Integer(3)});
		assertEquals("All rows returned", 2, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
		assertEquals("Second row is Integer", 12, ((Integer)((Map)li.get(1)).get("age")).intValue());
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForListWithArgsAndEmptyResult() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(false);
		List li = this.template.queryForList(sql, new Object[] {new Integer(3)});
		assertEquals("All rows returned", 0, li.size());
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForListWithArgsAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		List li = this.template.queryForList(sql, new Object[] {new Integer(3)});
		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForListWithArgsAndIntegerElementAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(11);
		List li = this.template.queryForList(sql, new Object[] {new Integer(3)}, Integer.class);
		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, ((Integer) li.get(0)).intValue());
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForMapWithArgsAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		Map map = this.template.queryForMap(sql, new Object[] {new Integer(3)});
		assertEquals("Row is Integer", 11, ((Integer) map.get("age")).intValue());
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForObjectWithArgsAndRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, new Object[] {new Integer(3)}, new RowMapper<Integer>() {
			@Override
			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}
		});
		assertTrue("Correct result type", o instanceof Integer);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForObjectWithArgsAndInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, new Object[] {new Integer(3)}, Integer.class);
		assertTrue("Correct result type", o instanceof Integer);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForIntWithArgs() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		int i = this.template.queryForInt(sql, new Object[] {new Integer(3)});
		assertEquals("Return of an int", 22, i);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testQueryForLongWithArgs() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);
		long l = this.template.queryForLong(sql, new Object[] {new Integer(3)});
		assertEquals("Return of a long", 87, l);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

}
