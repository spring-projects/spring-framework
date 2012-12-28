/*
 * Copyright 2002-2009 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Thomas Risberg
 */
public class SimpleJdbcTemplateTests {

	private static final String SQL = "sql";

	private static final Object[] ARGS_ARRAY = { 24.7, "foo", new Object() };
	private static final Map<String, Object> ARGS_MAP;
	private static final MapSqlParameterSource ARGS_SOURCE;

	static {
		ARGS_MAP = new HashMap<String, Object>(3);
		ARGS_SOURCE = new MapSqlParameterSource();
		for (int i = 0; i < ARGS_ARRAY.length; i++) {
			ARGS_MAP.put(String.valueOf(i), ARGS_ARRAY[i]);
			ARGS_SOURCE.addValue(String.valueOf(i), ARGS_ARRAY[i]);
		}
	}

	private JdbcOperations operations;
	private NamedParameterJdbcOperations namedParameterOperations;
	private SimpleJdbcTemplate template;
	private SimpleJdbcTemplate namedParameterTemplate;

	@Before
	public void setup() {
		this.operations = mock(JdbcOperations.class);
		this.namedParameterOperations = mock(NamedParameterJdbcOperations.class);
		this.template = new SimpleJdbcTemplate(operations);
		this.namedParameterTemplate = new SimpleJdbcTemplate(namedParameterOperations);
	}

	@Test
	public void testQueryForIntWithoutArgs() {
		given(operations.queryForInt(SQL)).willReturn(666);
		int result = template.queryForInt(SQL);
		assertEquals(666, result);
	}

	@Test
	public void testQueryForIntWithArgs() {
		given(operations.queryForInt(SQL, new Object[] { 24, "foo" })).willReturn(666);
		int result = template.queryForInt(SQL, 24, "foo");
		assertEquals(666, result);
	}

	@Test
	public void testQueryForIntWithMap() {
		Map<String, Object> args = new HashMap<String, Object>(2);
		args.put("id", 24);
		args.put("xy", "foo");
		given(namedParameterOperations.queryForInt(SQL, args)).willReturn(666);
		int result = namedParameterTemplate.queryForInt(SQL, args);
		assertEquals(666, result);
	}

	@Test
	public void testQueryForIntWitSqlParameterSource() {
		SqlParameterSource args = new MapSqlParameterSource().addValue("id", 24).addValue("xy", "foo");
		given(namedParameterOperations.queryForInt(SQL, args)).willReturn(666);
		int result = namedParameterTemplate.queryForInt(SQL, args);
		assertEquals(666, result);
	}

	@Test
	public void testQueryForLongWithoutArgs() {
		given(operations.queryForLong(SQL)).willReturn((long) 666);
		long result = template.queryForLong(SQL);
		assertEquals(666, result);
	}

	@Test
	public void testQueryForLongWithArgs() {
		long expectedResult = 666;
		given(operations.queryForLong(SQL, ARGS_ARRAY)).willReturn(expectedResult);
		long result = template.queryForLong(SQL, ARGS_ARRAY);
		assertEquals(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithoutArgs() throws Exception {
		Date expectedResult = new Date();
		given(operations.queryForObject(SQL, Date.class)).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, Date.class);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithArgs() throws Exception {
		Date expectedResult = new Date();
		given(operations.queryForObject(SQL, ARGS_ARRAY, Date.class)
				).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, Date.class,ARGS_ARRAY);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithArgArray() throws Exception {
		Date expectedResult = new Date();
		given(operations.queryForObject(SQL, ARGS_ARRAY, Date.class)
				).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, Date.class, ARGS_ARRAY);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithMap() throws Exception {
		Date expectedResult = new Date();
		given(operations.queryForObject(SQL, ARGS_ARRAY, Date.class)
				).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, Date.class, ARGS_ARRAY);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithRowMapperAndWithoutArgs() throws Exception {
		Date expectedResult = new Date();
		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			@Override
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};
		given(operations.queryForObject(SQL, rm)).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, rm);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithRowMapperAndArgs() throws Exception {
		Date expectedResult = new Date();
		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			@Override
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};
		given(operations.queryForObject(SQL, ARGS_ARRAY, rm)
				).willReturn(expectedResult);
		Date result = template.queryForObject(SQL, rm, ARGS_ARRAY);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForObjectWithRowMapperAndMap() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			@Override
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};
		given(operations.queryForObject(sql, ARGS_ARRAY, rm)
				).willReturn(expectedResult);
		Date result = template.queryForObject(sql, rm, ARGS_ARRAY);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForListWithoutArgs() throws Exception {
		List<Map<String, Object>> expectedResult = mockListMapResult();
		given(operations.queryForList(SQL)).willReturn(expectedResult);
		List<Map<String, Object>> result = template.queryForList(SQL);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForListWithArgs() throws Exception {
		List<Map<String, Object>> expectedResult = mockListMapResult();
		given(operations.queryForList(SQL, 1, 2, 3)).willReturn(expectedResult);
		List<Map<String, Object>> result = template.queryForList(SQL, 1,2,3);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForListWithMap() throws Exception {
		List<Map<String, Object>> expectedResult = mockListMapResult();
		given(namedParameterOperations.queryForList(SQL, ARGS_MAP)).willReturn(expectedResult);
		List<Map<String, Object>> result = namedParameterTemplate.queryForList(SQL, ARGS_MAP);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForListWithSqlParameterSource() throws Exception {
		List<Map<String, Object>> expectedResult = mockListMapResult();
		given(namedParameterOperations.queryForList(SQL, ARGS_SOURCE)).willReturn(expectedResult);
		List<Map<String, Object>> result = namedParameterTemplate.queryForList(SQL, ARGS_SOURCE);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForMapWithoutArgs() throws Exception {
		Map<String, Object> expectedResult = new HashMap<String, Object>();
		given(operations.queryForMap(SQL)).willReturn(expectedResult);
		 Map<String, Object> result = template.queryForMap(SQL);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForMapWithArgs() throws Exception {
		Map<String, Object> expectedResult = new HashMap<String, Object>();
		given(operations.queryForMap(SQL, 1, 2, 3)).willReturn(expectedResult);
		Map<String, Object> result = template.queryForMap(SQL, 1,2,3);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForMapWithMap() throws Exception {
		Map<String, Object> expectedResult = new HashMap<String, Object>();
		given(namedParameterOperations.queryForMap(SQL, ARGS_MAP)).willReturn(expectedResult);
		Map<String, Object> result = namedParameterTemplate.queryForMap(SQL, ARGS_MAP);
		assertSame(expectedResult, result);
	}

	@Test
	public void testQueryForMapWithSqlParameterSource() throws Exception {
		Map<String, Object> expectedResult = new HashMap<String, Object>();
		given(namedParameterOperations.queryForMap(SQL, ARGS_SOURCE)).willReturn(expectedResult);
		Map<String, Object> result = namedParameterTemplate.queryForMap(SQL, ARGS_SOURCE);
		assertSame(expectedResult, result);
	}

	@Test
	public void testUpdateWithoutArgs() throws Exception {
		given(operations.update(SQL)).willReturn(666);
		int result = template.update(SQL);
		assertEquals(666, result);
	}

	@Test
	public void testUpdateWithArgs() throws Exception {
		given(operations.update(SQL, 1, 2, 3)).willReturn(666);
		int result = template.update(SQL, 1, 2, 3);
		assertEquals(666, result);
	}

	@Test
	public void testUpdateWithMap() throws Exception {
		given(namedParameterOperations.update(SQL, ARGS_MAP)).willReturn(666);
		int result = namedParameterTemplate.update(SQL, ARGS_MAP);
		assertEquals(666, result);
	}

	@Test
	public void testUpdateWithSqlParameterSource() throws Exception {
		given(namedParameterOperations.update(SQL, ARGS_SOURCE)).willReturn(666);
		int result = namedParameterTemplate.update(SQL, ARGS_SOURCE);
		assertEquals(666, result);
	}

	@Test
	public void testBatchUpdateWithSqlParameterSource() throws Exception {
		PreparedStatement preparedStatement = setupBatchOperation();
		final SqlParameterSource[] ids = new SqlParameterSource[2];
		ids[0] = new MapSqlParameterSource("id", 100);
		ids[1] = new MapSqlParameterSource("id", 200);
		int[] actualRowsAffected = template.batchUpdate("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(1, actualRowsAffected[0]);
		assertEquals(2, actualRowsAffected[1]);
		verify(preparedStatement).setObject(1, 100);
		verify(preparedStatement).setObject(1, 200);
		verify(preparedStatement, times(2)).addBatch();
		verify(preparedStatement).close();
	}

	@Test
	public void testBatchUpdateWithListOfObjectArrays() throws Exception {
		PreparedStatement preparedStatement = setupBatchOperation();
		List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] { 100 });
		ids.add(new Object[] { 200 });
		int[] actualRowsAffected = template.batchUpdate(SQL, ids);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(1, actualRowsAffected[0]);
		assertEquals(2, actualRowsAffected[1]);
		verify(preparedStatement).setObject(1, 100);
		verify(preparedStatement).setObject(1, 200);
		verify(preparedStatement, times(2)).addBatch();
		verify(preparedStatement).close();
	}

	@Test
	public void testBatchUpdateWithListOfObjectArraysPlusTypeInfo() throws Exception {
		int[] sqlTypes = new int[] { Types.NUMERIC };
		PreparedStatement preparedStatement = setupBatchOperation();
		List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] { 100 });
		ids.add(new Object[] { 200 });
		int[] actualRowsAffected = template.batchUpdate(SQL, ids, sqlTypes);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(1, actualRowsAffected[0]);
		assertEquals(2, actualRowsAffected[1]);
		verify(preparedStatement).setObject(1, 100, Types.NUMERIC);
		verify(preparedStatement).setObject(1, 200, Types.NUMERIC);
		verify(preparedStatement, times(2)).addBatch();
		verify(preparedStatement).close();
	}

	private PreparedStatement setupBatchOperation() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		PreparedStatement preparedStatement = mock(PreparedStatement.class);
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(dataSource.getConnection()).willReturn(connection);
		given(preparedStatement.getConnection()).willReturn(connection);
		given(preparedStatement.executeBatch()).willReturn(new int[] { 1, 2 });
		given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
		given(databaseMetaData.supportsBatchUpdates()).willReturn(true);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(connection.getMetaData()).willReturn(databaseMetaData);
		template = new SimpleJdbcTemplate(new JdbcTemplate(dataSource, false));
		return preparedStatement;
	}

	private List<Map<String, Object>> mockListMapResult() {
		return new LinkedList<Map<String, Object>>();
	}
}
