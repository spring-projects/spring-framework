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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.easymock.MockControl;
import org.easymock.internal.ArrayMatcher;

import org.springframework.jdbc.core.BatchUpdateTestHelper;
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
public class SimpleJdbcTemplateTests extends TestCase {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();


	public void testQueryForIntWithoutArgs() {
		String sql = "SELECT COUNT(0) FROM BAR";
		int expectedResult = 666;

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForInt(sql);
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);

		assertSame(jo, jth.getJdbcOperations());

		int result = jth.queryForInt(sql);
		assertEquals(expectedResult, result);

		mc.verify();
	}

	public void testQueryForIntWithArgs() {
		String sql = "SELECT COUNT(0) FROM BAR WHERE ID=? AND XY=?";
		int expectedResult = 666;
		int arg1 = 24;
		String arg2 = "foo";

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForInt(sql, new Object[]{arg1, arg2});
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		int result = jth.queryForInt(sql, arg1, arg2);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForIntWithMap() {
		String sql = "SELECT COUNT(0) FROM BAR WHERE ID=:id AND XY=:xy";
		int expectedResult = 666;
		int arg1 = 24;
		String arg2 = "foo";

		MockControl mc = MockControl.createControl(NamedParameterJdbcOperations.class);
		NamedParameterJdbcOperations npjo = (NamedParameterJdbcOperations) mc.getMock();
		Map<String, Object> args = new HashMap<String, Object>(2);
		args.put("id", arg1);
		args.put("xy", arg2);
		npjo.queryForInt(sql, args);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(npjo);
		int result = jth.queryForInt(sql, args);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForIntWitSqlParameterSource() {
		String sql = "SELECT COUNT(0) FROM BAR WHERE ID=:id AND XY=:xy";
		int expectedResult = 666;
		int arg1 = 24;
		String arg2 = "foo";

		MockControl mc = MockControl.createControl(NamedParameterJdbcOperations.class);
		NamedParameterJdbcOperations npjo = (NamedParameterJdbcOperations) mc.getMock();
		SqlParameterSource args = new MapSqlParameterSource().addValue("id", arg1).addValue("xy", arg2);
		npjo.queryForInt(sql, args);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(npjo);
		int result = jth.queryForInt(sql, args);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForLongWithoutArgs() {
		String sql = "SELECT COUNT(0) FROM BAR";
		long expectedResult = 666;

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForLong(sql);
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		long result = jth.queryForLong(sql);
		assertEquals(expectedResult, result);

		mc.verify();
	}

	public void testQueryForLongWithArgs() {
		String sql = "SELECT COUNT(0) FROM BAR WHERE ID=? AND XY=?";
		long expectedResult = 666;
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForLong(sql, new Object[]{arg1, arg2, arg3});
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		long result = jth.queryForLong(sql, arg1, arg2, arg3);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithoutArgs() throws Exception {
		String sql = "SELECT SYSDATE FROM DUAL";
		Date expectedResult = new Date();

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, Date.class);
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, Date.class);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithArgs() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, new Object[]{arg1, arg2, arg3}, Date.class);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, Date.class, arg1, arg2, arg3);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithArgArray() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, new Object[]{arg1, arg2, arg3}, Date.class);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Object args = new Object[] {arg1, arg2, arg3};
		Date result = jth.queryForObject(sql, Date.class, args);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithMap() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, new Object[]{arg1, arg2, arg3}, Date.class);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, Date.class, arg1, arg2, arg3);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithRowMapperAndWithoutArgs() throws Exception {
		String sql = "SELECT SYSDATE FROM DUAL";
		Date expectedResult = new Date();

		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, rm);
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, rm);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithRowMapperAndArgs() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, new Object[]{arg1, arg2, arg3}, rm);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, rm, arg1, arg2, arg3);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForObjectWithRowMapperAndMap() throws Exception {
		String sql = "SELECT SOMEDATE FROM BAR WHERE ID=? AND XY=?";
		Date expectedResult = new Date();
		double arg1 = 24.7;
		String arg2 = "foo";
		Object arg3 = new Object();

		ParameterizedRowMapper<Date> rm = new ParameterizedRowMapper<Date>() {
			public Date mapRow(ResultSet rs, int rowNum) {
				return new Date();
			}
		};

		MockControl mc = MockControl.createControl(JdbcOperations.class);
		JdbcOperations jo = (JdbcOperations) mc.getMock();
		jo.queryForObject(sql, new Object[]{arg1, arg2, arg3}, rm);
		mc.setDefaultMatcher(new ArrayMatcher());
		mc.setReturnValue(expectedResult);
		mc.replay();

		SimpleJdbcTemplate jth = new SimpleJdbcTemplate(jo);
		Date result = jth.queryForObject(sql, rm, arg1, arg2, arg3);
		assertEquals(expectedResult, result);
		mc.verify();
	}

	public void testQueryForListWithoutArgs() throws Exception {
		testDelegation("queryForList", new Object[]{"sql"}, new Object[]{}, Collections.singletonList(new Object()));
	}

	public void testQueryForListWithArgs() throws Exception {
		testDelegation("queryForList", new Object[]{"sql"}, new Object[]{1, 2, 3}, new LinkedList<Object>());
	}

	public void testQueryForListWithMap() throws Exception {
		HashMap<String, Integer> args = new HashMap<String, Integer>(3);
		args.put("1", 1);
		args.put("2", 2);
		args.put("3", 3);
		testDelegation("queryForList", new Object[]{"sql"}, new Object[]{args}, new LinkedList<Object>());
	}

	public void testQueryForListWithSqlParameterSource() throws Exception {
		MapSqlParameterSource args = new MapSqlParameterSource();
		args.addValue("1", 1);
		args.addValue("2", 2);
		args.addValue("3", 3);
		testDelegation("queryForList", new Object[]{"sql"}, new Object[]{args}, new LinkedList<Object>());
	}

	public void testQueryForMapWithoutArgs() throws Exception {
		testDelegation("queryForMap", new Object[]{"sql"}, new Object[]{}, new HashMap<Object, Object>());
	}

	public void testQueryForMapWithArgs() throws Exception {
		testDelegation("queryForMap", new Object[]{"sql"}, new Object[]{1, 2, 3}, new HashMap<Object, Object>());
		// TODO test generic type
	}

	public void testQueryForMapWithMap() throws Exception {
		HashMap<String, Integer> args = new HashMap<String, Integer>(3);
		args.put("1", 1);
		args.put("2", 2);
		args.put("3", 3);
		testDelegation("queryForMap", new Object[]{"sql"}, new Object[]{args}, new HashMap<Object, Object>());
	}

	public void testQueryForMapWithSqlParameterSource() throws Exception {
		MapSqlParameterSource args = new MapSqlParameterSource();
		args.addValue("1", 1);
		args.addValue("2", 2);
		args.addValue("3", 3);
		testDelegation("queryForMap", new Object[]{"sql"}, new Object[]{args}, new HashMap<Object, Object>());
	}

	public void testUpdateWithoutArgs() throws Exception {
		testDelegation("update", new Object[]{"sql"}, new Object[]{}, 666);
	}

	public void testUpdateWithArgs() throws Exception {
		testDelegation("update", new Object[]{"sql"}, new Object[]{1, 2, 3}, 666);
	}

	public void testUpdateWithMap() throws Exception {
		HashMap<String, Integer> args = new HashMap<String, Integer>(3);
		args.put("1", 1);
		args.put("2", 2);
		args.put("3", 3);
		testDelegation("update", new Object[]{"sql"}, new Object[]{args}, 666);
	}

	public void testUpdateWithSqlParameterSource() throws Exception {
		MapSqlParameterSource args = new MapSqlParameterSource();
		args.addValue("1", 1);
		args.addValue("2", 2);
		args.addValue("3", 3);
		testDelegation("update", new Object[]{"sql"}, new Object[]{args}, 666);
	}

	private Object testDelegation(String methodName, Object[] typedArgs, Object[] varargs, Object expectedResult) throws Exception {
		Class<?>[] unifiedTypes;
		Object[] unifiedArgs;
		Class<?>[] unifiedTypes2;
		Object[] unifiedArgs2;
		boolean namedParameters = false;

		if (varargs != null && varargs.length > 0) {
			// Allow for Map
			if (varargs[0].getClass().equals(HashMap.class)) {
				unifiedTypes = new Class[typedArgs.length + 1];
				unifiedArgs = new Object[typedArgs.length + 1];
				for (int i = 0; i < typedArgs.length; i++) {
					unifiedTypes[i] = typedArgs[i].getClass();
					unifiedArgs[i] = typedArgs[i];
				}
				unifiedTypes[unifiedTypes.length - 1] = Map.class;
				unifiedArgs[unifiedArgs.length - 1] = varargs[0];
				unifiedTypes2 = unifiedTypes;
				unifiedArgs2 = unifiedArgs;
				namedParameters = true;
			}
			else if (varargs[0].getClass().equals(MapSqlParameterSource.class)) {
				unifiedTypes = new Class[typedArgs.length + 1];
				unifiedArgs = new Object[typedArgs.length + 1];
				for (int i = 0; i < typedArgs.length; i++) {
					unifiedTypes[i] = typedArgs[i].getClass();
					unifiedArgs[i] = typedArgs[i];
				}
				unifiedTypes[unifiedTypes.length - 1] = SqlParameterSource.class;
				unifiedArgs[unifiedArgs.length - 1] = varargs[0];
				unifiedTypes2 = unifiedTypes;
				unifiedArgs2 = unifiedArgs;
				namedParameters = true;
			}
			else {
				// Allow for varargs.length
				unifiedTypes = new Class[typedArgs.length + 1];
				unifiedArgs = new Object[typedArgs.length + 1];
				for (int i = 0; i < unifiedTypes.length - 1; i++) {
					unifiedTypes[i] = typedArgs[i].getClass();
					unifiedArgs[i] = typedArgs[i];
				}
				unifiedTypes[unifiedTypes.length - 1] = Object[].class;
				unifiedArgs[unifiedTypes.length - 1] = varargs;
			}

			unifiedTypes2 = unifiedTypes;
			unifiedArgs2 = unifiedArgs;
		}
		else {
			unifiedTypes = new Class[typedArgs.length];
			unifiedTypes2 = new Class[typedArgs.length + 1];
			unifiedArgs = new Object[typedArgs.length];
			unifiedArgs2 = new Object[typedArgs.length + 1];
			for (int i = 0; i < typedArgs.length; i++) {
				unifiedTypes[i] = unifiedTypes2[i] = typedArgs[i].getClass();
				unifiedArgs[i] = unifiedArgs2[i] = typedArgs[i];
			}
			unifiedTypes2[unifiedTypes2.length - 1] = Object[].class;
			unifiedArgs2[unifiedArgs2.length - 1] = new Object[]{};
		}

		MockControl mc;
		JdbcOperations jo = null;
		NamedParameterJdbcOperations npjo = null;
		Method joMethod = null;
		SimpleJdbcTemplate jth = null;

		if (namedParameters) {
			mc = MockControl.createControl(NamedParameterJdbcOperations.class);
			npjo = (NamedParameterJdbcOperations) mc.getMock();
			joMethod = NamedParameterJdbcOperations.class.getMethod(methodName, unifiedTypes);
			joMethod.invoke(npjo, unifiedArgs);
			jth = new SimpleJdbcTemplate(npjo);
		}
		else {
			mc = MockControl.createControl(JdbcOperations.class);
			jo = (JdbcOperations) mc.getMock();
			joMethod = JdbcOperations.class.getMethod(methodName, unifiedTypes);
			joMethod.invoke(jo, unifiedArgs);
			jth = new SimpleJdbcTemplate(jo);
		}

		mc.setDefaultMatcher(new ArrayMatcher());

		if (joMethod.getReturnType().isPrimitive()) {
			// TODO bit of a hack with autoboxing passing up Integer when the return
			// type is an int
			mc.setReturnValue(((Integer) expectedResult).intValue());
		}
		else {
			mc.setReturnValue(expectedResult);
		}
		mc.replay();

		Method jthMethod = SimpleJdbcTemplate.class.getMethod(methodName, unifiedTypes2);
		Object result = jthMethod.invoke(jth, unifiedArgs2);

		assertEquals(expectedResult, result);

		mc.verify();

		return result;
	}

	public void testBatchUpdateWithSqlParameterSource() throws Exception {

		final String sqlToUse = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id";
		final SqlParameterSource[] ids = new SqlParameterSource[2];
		ids[0] = new MapSqlParameterSource("id", 100);
		ids[1] = new MapSqlParameterSource("id", 200);
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlDataSource = MockControl.createControl(DataSource.class);
		DataSource mockDataSource = (DataSource) ctrlDataSource.getMock();
		MockControl ctrlConnection = MockControl.createControl(Connection.class);
		Connection mockConnection = (Connection) ctrlConnection.getMock();
		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		BatchUpdateTestHelper.prepareBatchUpdateMocks(sqlToUse, ids, null, rowsAffected, ctrlDataSource, mockDataSource, ctrlConnection,
				mockConnection, ctrlPreparedStatement, mockPreparedStatement, ctrlDatabaseMetaData,
				mockDatabaseMetaData);

		BatchUpdateTestHelper.replayBatchUpdateMocks(ctrlDataSource, ctrlConnection, ctrlPreparedStatement, ctrlDatabaseMetaData);

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
		SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(template);

		int[] actualRowsAffected = simpleJdbcTemplate.batchUpdate(sql, ids);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		BatchUpdateTestHelper.verifyBatchUpdateMocks(ctrlPreparedStatement, ctrlDatabaseMetaData);
	}

	public void testBatchUpdateWithListOfObjectArrays() throws Exception {

		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlDataSource = MockControl.createControl(DataSource.class);
		DataSource mockDataSource = (DataSource) ctrlDataSource.getMock();
		MockControl ctrlConnection = MockControl.createControl(Connection.class);
		Connection mockConnection = (Connection) ctrlConnection.getMock();
		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		BatchUpdateTestHelper.prepareBatchUpdateMocks(sql, ids, null, rowsAffected, ctrlDataSource, mockDataSource, ctrlConnection,
				mockConnection, ctrlPreparedStatement, mockPreparedStatement, ctrlDatabaseMetaData,
				mockDatabaseMetaData);

		BatchUpdateTestHelper.replayBatchUpdateMocks(ctrlDataSource, ctrlConnection, ctrlPreparedStatement, ctrlDatabaseMetaData);

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
		SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(template);

		int[] actualRowsAffected = simpleJdbcTemplate.batchUpdate(sql, ids);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		BatchUpdateTestHelper.verifyBatchUpdateMocks(ctrlPreparedStatement, ctrlDatabaseMetaData);
	}

	public void testBatchUpdateWithListOfObjectArraysPlusTypeInfo() throws Exception {

		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] sqlTypes = new int[] {Types.NUMERIC};
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlDataSource = MockControl.createControl(DataSource.class);
		DataSource mockDataSource = (DataSource) ctrlDataSource.getMock();
		MockControl ctrlConnection = MockControl.createControl(Connection.class);
		Connection mockConnection = (Connection) ctrlConnection.getMock();
		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		BatchUpdateTestHelper.prepareBatchUpdateMocks(sql, ids, sqlTypes, rowsAffected, ctrlDataSource, mockDataSource, ctrlConnection,
				mockConnection, ctrlPreparedStatement, mockPreparedStatement, ctrlDatabaseMetaData,
				mockDatabaseMetaData);

		BatchUpdateTestHelper.replayBatchUpdateMocks(ctrlDataSource, ctrlConnection, ctrlPreparedStatement, ctrlDatabaseMetaData);

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
		SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(template);

		int[] actualRowsAffected = simpleJdbcTemplate.batchUpdate(sql, ids, sqlTypes);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		BatchUpdateTestHelper.verifyBatchUpdateMocks(ctrlPreparedStatement, ctrlDatabaseMetaData);
	}

}
