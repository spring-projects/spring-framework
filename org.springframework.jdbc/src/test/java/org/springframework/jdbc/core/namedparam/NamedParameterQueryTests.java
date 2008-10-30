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

package org.springframework.jdbc.core.namedparam;

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

import org.easymock.MockControl;

import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Thomas Risberg
 */
public class NamedParameterQueryTests extends AbstractJdbcTests {

	private MockControl ctrlPreparedStatement;
	private PreparedStatement mockPreparedStatement;
	private MockControl ctrlResultSet;
	private ResultSet mockResultSet;
	private MockControl ctrlResultSetMetaData;
	private ResultSetMetaData mockResultSetMetaData;

	protected void setUp() throws Exception {
		super.setUp();
		ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
	}

	protected void replay() {
		super.replay();
		ctrlPreparedStatement.replay();
		ctrlResultSet.replay();
		ctrlResultSetMetaData.replay();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (false && shouldVerify()) {
			ctrlPreparedStatement.verify();
			ctrlResultSet.verify();
			ctrlResultSetMetaData.verify();
		}
	}

	public void testQueryForListWithParamMap() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID < ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1, 2);
		mockResultSetMetaData.getColumnLabel(1);
		ctrlResultSetMetaData.setReturnValue("age", 2);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 2);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(11));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(12));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		List li = template.queryForList(sql, parms);
		assertEquals("All rows returned", 2, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
		assertEquals("Second row is Integer", 12, ((Integer)((Map)li.get(1)).get("age")).intValue());
	}

	public void testQueryForListWithParamMapAndEmptyResult() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID < ?";

		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		List li = template.queryForList(sql, parms);
		assertEquals("All rows returned", 0, li.size());
	}

	public void testQueryForListWithParamMapAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID < ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);
		mockResultSetMetaData.getColumnLabel(1);
		ctrlResultSetMetaData.setReturnValue("age", 1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(11));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		List li = template.queryForList(sql, parms);
		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, ((Integer)((Map)li.get(0)).get("age")).intValue());
	}

	public void testQueryForListWithParamMapAndIntegerElementAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID < ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(11);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		List li = template.queryForList(sql, parms, Integer.class);
		assertEquals("All rows returned", 1, li.size());
		assertEquals("First row is Integer", 11, ((Integer) li.get(0)).intValue());
	}

	public void testQueryForMapWithParamMapAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID < ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);
		mockResultSetMetaData.getColumnLabel(1);
		ctrlResultSetMetaData.setReturnValue("age", 1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(11));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		Map map = template.queryForMap(sql, parms);
		assertEquals("Row is Integer", 11, ((Integer) map.get("age")).intValue());
	}

	public void testQueryForObjectWithParamMapAndRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID = ?";

		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(22);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		Object o = template.queryForObject(sql, parms, new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new Integer(rs.getInt(1));
			}
		});
		assertTrue("Correct result type", o instanceof Integer);
	}

	public void testQueryForObjectWithMapAndInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID = ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(22);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		Map parms = new HashMap();
		parms.put("id", new Integer(3));

		Object o = template.queryForObject(sql, parms, Integer.class);
		assertTrue("Correct result type", o instanceof Integer);
	}

	public void testQueryForObjectWithParamMapAndInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID = ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(22);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		Object o = template.queryForObject(sql, parms, Integer.class);
		assertTrue("Correct result type", o instanceof Integer);
	}

	public void testQueryForObjectWithParamMapAndList() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID IN (:ids)";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(22);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(2, new Integer(4));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("ids", Arrays.asList(new Object[] {new Integer(3), new Integer(4)}));

		Object o = template.queryForObject(sql, parms, Integer.class);
		assertTrue("Correct result type", o instanceof Integer);
	}

	public void testQueryForObjectWithParamMapAndListOfExpressionLists() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN (:multiExpressionList)";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN ((?, ?), (?, ?))";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(22);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "Rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(3, new Integer(4));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(4, "Juergen");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		List l1 = new ArrayList();
		l1.add(new Object[] {new Integer(3), "Rod"});
		l1.add(new Object[] {new Integer(4), "Juergen"});
		parms.addValue("multiExpressionList", l1);

		Object o = template.queryForObject(sql, parms, Integer.class);
		assertTrue("Correct result type", o instanceof Integer);
	}

	public void testQueryForIntWithParamMap() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID = ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getDouble(1);
		ctrlResultSet.setReturnValue(22.0d);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);

		MapSqlParameterSource parms = new MapSqlParameterSource();
		parms.addValue("id", new Integer(3));

		int i = template.queryForInt(sql, parms);
		assertEquals("Return of an int", 22, i);
	}

	public void testQueryForLongWithParamBean() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = :id";
		String sqlToUse = "SELECT AGE FROM CUSTMR WHERE ID = ?";

		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getDouble(1);
		ctrlResultSet.setReturnValue(87.0d);
		mockResultSet.wasNull();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(3), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(mockDataSource);
		BeanPropertySqlParameterSource parms = new BeanPropertySqlParameterSource(new ParameterBean(3));

		long l = template.queryForLong(sql, parms);
		assertEquals("Return of a long", 87, l);
	}


	private static class ParameterBean {

		private int id;

		public ParameterBean(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

}
