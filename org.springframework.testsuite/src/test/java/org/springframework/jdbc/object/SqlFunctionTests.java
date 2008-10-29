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

package org.springframework.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.logging.LogFactory;
import org.easymock.MockControl;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Trevor Cook
 */
public class SqlFunctionTests extends AbstractJdbcTests {

	private static final String FUNCTION = "select count(id) from mytable";
	private static final String FUNCTION_INT =
		"select count(id) from mytable where myparam = ?";
	private static final String FUNCTION_MIXED =
		"select count(id) from mytable where myparam = ? and mystring = ?";

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

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

	protected void tearDown() throws Exception {
		super.tearDown();
		if (shouldVerify()) {
			ctrlPreparedStatement.verify();
			ctrlResultSet.verify();
			ctrlResultSetMetaData.verify();
		}
	}

	protected void replay() {
		super.replay();
		ctrlPreparedStatement.replay();
		ctrlResultSet.replay();
		ctrlResultSetMetaData.replay();
	}


	public void testFunction() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(14));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction();
		function.setDataSource(mockDataSource);
		function.setSql(FUNCTION);
		function.compile();

		int count = function.run();
		assertTrue("Function returned value 14", count == 14);
	}

	public void testTooManyRows() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1, 2);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 2);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(14), 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(15), 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction(mockDataSource, FUNCTION);
		function.compile();

		try {
			int count = function.run();
			fail("Shouldn't continue when too many rows returned");
		}
		catch (IncorrectResultSizeDataAccessException idaauex) {
			// OK 
		}
	}

	public void testFunctionInt() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(14));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION_INT);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction(mockDataSource, FUNCTION_INT);
		function.setTypes(new int[] { Types.INTEGER });
		function.compile();

		int count = function.run(1);
		assertTrue("Function returned value 14", count == 14);
	}

	public void testFunctionMixed() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(14));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION_MIXED);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction(
				mockDataSource, FUNCTION_MIXED, new int[] { Types.INTEGER, Types.VARCHAR });
		function.compile();

		int count = function.run(new Object[] { new Integer(1), "rod" });
		assertTrue("Function returned value 14", count == 14);
	}

	public void testFunctionWithStringResult() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue("14");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION_MIXED);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction( mockDataSource, FUNCTION_MIXED);
		function.setTypes(new int[] { Types.INTEGER, Types.VARCHAR });
		function.compile();

		String result = (String) function.runGeneric(new Object[] { new Integer(1), "rod" });
		assertTrue("Function returned value 14", "14".equals(result));
	}

	public void testFunctionWithStringConvertedResult() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);

		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData, 1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue("14");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(FUNCTION_MIXED);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlFunction function = new SqlFunction( mockDataSource, FUNCTION_MIXED);
		function.setTypes(new int[] { Types.INTEGER, Types.VARCHAR });
		function.setResultType(String.class);
		function.compile();

		String result = (String) function.runGeneric(new Object[] { new Integer(1), "rod" });
		assertTrue("Function returned value 14", "14".equals(result));
	}

}
