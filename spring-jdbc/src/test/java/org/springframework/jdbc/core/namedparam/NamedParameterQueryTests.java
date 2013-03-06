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

package org.springframework.jdbc.core.namedparam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thomas Risberg
 * @author Phillip Webb
 */
public class NamedParameterQueryTests {

	private DataSource dataSource;

	private Connection connection;

	private PreparedStatement preparedStatement;

	private ResultSet resultSet;

	private ResultSetMetaData resultSetMetaData;

	private NamedParameterJdbcTemplate template;

	@Before
	public void setUp() throws Exception {
		connection = mock(Connection.class);
		dataSource = mock(DataSource.class);
		preparedStatement = mock(PreparedStatement.class);
		resultSet = mock(ResultSet.class);
		resultSetMetaData = mock(ResultSetMetaData.class);
		template = new NamedParameterJdbcTemplate(dataSource);
		given(dataSource.getConnection()).willReturn(connection);
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("age");
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
	}

	@After
	public void verifyClose() throws Exception {
		verify(preparedStatement).close();
		verify(resultSet).close();
		verify(connection).close();
	}

	@Test
	public void testQueryForListWithParamMap() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getObject(1)).willReturn(11, 12);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		List<Map<String, Object>> li = template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", parms);

		assertEquals("All rows returned", 2, li.size());
		assertEquals("First row is Integer", 11,
				((Integer) ((Map) li.get(0)).get("age")).intValue());
		assertEquals("Second row is Integer", 12,
				((Integer) ((Map) li.get(1)).get("age")).intValue());

		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndEmptyResult() throws Exception {
		given(resultSet.next()).willReturn(false);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		List<Map<String, Object>> li = template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", parms);

		assertEquals("All rows returned", 0, li.size());
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndSingleRowAndColumn() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		List<Map<String, Object>> li = template.queryForList(
				"SELECT AGE FROM CUSTMR WHERE ID < :id", parms);

		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11,
				((Integer) ((Map) li.get(0)).get("age")).intValue());
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForListWithParamMapAndIntegerElementAndSingleRowAndColumn()
			throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(11);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		List<Integer> li = template.queryForList("SELECT AGE FROM CUSTMR WHERE ID < :id",
				parms, Integer.class);

		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, li.get(0).intValue());
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForMapWithParamMapAndSingleRowAndColumn() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		Map map = template.queryForMap("SELECT AGE FROM CUSTMR WHERE ID < :id", parms);

		assertEquals("Row is Integer", 11, ((Integer) map.get("age")).intValue());
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndRowMapper() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		Object o = template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				parms, new RowMapper<Object>() {
					@Override
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getInt(1);
					}
				});

		assertTrue("Correct result type", o instanceof Integer);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithMapAndInteger() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Map<String, Object> parms = new HashMap<String, Object>();
		parms.put("id", 3);
		Object o = template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				parms, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndInteger() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		Object o = template.queryForObject("SELECT AGE FROM CUSTMR WHERE ID = :id",
				parms, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndList() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID IN (:ids)";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)";
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("ids", Arrays.asList(new Object[] { 3, 4 }));
		Object o = template.queryForObject(sql, parms, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(connection).prepareStatement(sqlToUse);
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForObjectWithParamMapAndListOfExpressionLists() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		List<Object[]> l1 = new ArrayList<Object[]>();
		l1.add(new Object[] { 3, "Rod" });
		l1.add(new Object[] { 4, "Juergen" });
		parms.addValue("multiExpressionList", l1);
		Object o = template.queryForObject(
				"SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN (:multiExpressionList)",
				parms, Integer.class);

		assertTrue("Correct result type", o instanceof Integer);
		verify(connection).prepareStatement(
				"SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN ((?, ?), (?, ?))");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForIntWithParamMap() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", 3);
		int i = template.queryForInt("SELECT AGE FROM CUSTMR WHERE ID = :id", parms);

		assertEquals("Return of an int", 22, i);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
	}

	@Test
	public void testQueryForLongWithParamBean() throws Exception {
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getLong(1)).willReturn(87L);

		BeanPropertySqlParameterSource parms = new BeanPropertySqlParameterSource(
				new ParameterBean(3));

		long l = template.queryForLong("SELECT AGE FROM CUSTMR WHERE ID = :id", parms);

		assertEquals("Return of a long", 87, l);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3, Types.INTEGER);
	}

	static class ParameterBean {

		private int id;

		public ParameterBean(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

}
