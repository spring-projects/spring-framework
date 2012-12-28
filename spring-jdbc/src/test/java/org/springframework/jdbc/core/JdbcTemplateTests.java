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

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.easymock.MockControl;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractorAdapter;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Mock object based tests for JdbcTemplate.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class JdbcTemplateTests extends AbstractJdbcTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();


	public void testBeanProperties() throws Exception {
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		assertTrue("datasource ok", template.getDataSource() == mockDataSource);
		assertTrue("ignores warnings by default", template.isIgnoreWarnings());
		template.setIgnoreWarnings(false);
		assertTrue("can set NOT to ignore warnings", !template.isIgnoreWarnings());
	}

	public void testUpdateCount() throws Exception {
		final String sql =
			"UPDATE INVOICE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		int idParam = 11111;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, idParam);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		Dispatcher d = new Dispatcher(idParam, sql);
		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int rowsAffected = template.update(d);
		assertTrue("1 update affected 1 row", rowsAffected == 1);

		/*
		d = new Dispatcher(idParam);
		rowsAffected = template.update(d);
		assertTrue("bogus update affected 0 rows", rowsAffected == 0);
		*/

		ctrlPreparedStatement.verify();
	}

	public void testBogusUpdate() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int idParam = 6666;

		// It's because Integers aren't canonical
		SQLException sex = new SQLException("bad update");

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, idParam);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		Dispatcher d = new Dispatcher(idParam, sql);
		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		try {
			template.update(d);
			fail("Bogus update should throw exception");
		}
		catch (UncategorizedDataAccessException ex) {
			// pass
			assertTrue(
				"Correct exception",
				ex instanceof UncategorizedSQLException);
			assertTrue("Root cause is correct", ex.getCause() == sex);
			//assertTrue("no update occurred", !je.getDataWasUpdated());
		}

		ctrlPreparedStatement.verify();
	}

	public void testStringsWithStaticSql() throws Exception {
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, rch);
			}
		}, false, null, null, null, null);
	}

	public void testStringsWithStaticSqlAndFetchSizeAndMaxRows() throws Exception {
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, rch);
			}
		}, false, new Integer(10), new Integer(20), new Integer(30), null);
	}

	public void testStringsWithEmptyPreparedStatementSetter() throws Exception {
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, (PreparedStatementSetter) null, rch);
			}
		}, true, null, null, null, null);
	}

	public void testStringsWithPreparedStatementSetter() throws Exception {
		final Integer argument = new Integer(99);
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, new PreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setObject(1, argument);
					}
				}, rch);
			}
		}, true, null, null, null, argument);
	}

	public void testStringsWithEmptyPreparedStatementArgs() throws Exception {
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, (Object[]) null, rch);
			}
		}, true, null, null, null, null);
	}

	public void testStringsWithPreparedStatementArgs() throws Exception {
		final Integer argument = new Integer(99);
		doTestStrings(new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch) {
				template.query(sql, new Object[] {argument}, rch);
			}
		}, true, null, null, null, argument);
	}

	private void doTestStrings(
			JdbcTemplateCallback jdbcTemplateCallback, boolean usePreparedStatement,
			Integer fetchSize, Integer maxRows, Integer queryTimeout, Object argument)
			throws Exception {

		String sql = "SELECT FORENAME FROM CUSTMR";
		String[] results = { "rod", "gary", " portia" };

		class StringHandler implements RowCallbackHandler {
			private List list = new LinkedList();
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				list.add(rs.getString(1));
			}
			public String[] getStrings() {
				return (String[]) list.toArray(new String[list.size()]);
			}
		}

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[0]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[1]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[2]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		if (fetchSize != null) {
			mockStatement.setFetchSize(fetchSize.intValue());
		}
		if (maxRows != null) {
			mockStatement.setMaxRows(maxRows.intValue());
		}
		if (queryTimeout != null) {
			mockStatement.setQueryTimeout(queryTimeout.intValue());
		}
		if (argument != null) {
			mockStatement.setObject(1, argument);
		}
		if (usePreparedStatement) {
			mockStatement.executeQuery();
		}
		else {
			mockStatement.executeQuery(sql);
		}
		ctrlStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		if (usePreparedStatement) {
			mockConnection.prepareStatement(sql);
		}
		else {
			mockConnection.createStatement();
		}
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		StringHandler sh = new StringHandler();
		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(mockDataSource);
		if (fetchSize != null) {
			template.setFetchSize(fetchSize.intValue());
		}
		if (maxRows != null) {
			template.setMaxRows(maxRows.intValue());
		}
		if (queryTimeout != null) {
			template.setQueryTimeout(queryTimeout.intValue());
		}
		jdbcTemplateCallback.doInJdbcTemplate(template, sql, sh);

		// Match
		String[] forenames = sh.getStrings();
		assertTrue("same length", forenames.length == results.length);
		for (int i = 0; i < forenames.length; i++) {
			assertTrue("Row " + i + " matches", forenames[i].equals(results[i]));
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testLeaveConnectionOpenOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		ctrlStatement = MockControl.createControl(PreparedStatement.class);
		mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.isClosed();
		ctrlConnection.setReturnValue(false, 2);
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		// if close is called entire test will fail
		mockConnection.close();
		ctrlConnection.setDefaultThrowable(new RuntimeException());

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		SingleConnectionDataSource scf = new SingleConnectionDataSource(mockDataSource.getConnection(), false);
		JdbcTemplate template2 = new JdbcTemplate(scf, false);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		template2.query(sql, rcch);

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testConnectionCallback() throws Exception {
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		template.setNativeJdbcExtractor(new PlainNativeJdbcExtractor());
		Object result = template.execute(new ConnectionCallback() {
			@Override
			public Object doInConnection(Connection con) {
				assertSame(mockConnection, con);
				return "test";
			}
		});

		assertEquals("test", result);
	}

	public void testConnectionCallbackWithStatementSettings() throws Exception {
		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockConnection.prepareStatement("some SQL");
		ctrlConnection.setReturnValue(mockStatement, 1);
		mockStatement.setFetchSize(10);
		ctrlStatement.setVoidCallable(1);
		mockStatement.setMaxRows(20);
		ctrlStatement.setVoidCallable(1);
		mockStatement.close();
		ctrlStatement.setVoidCallable(1);
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		Object result = template.execute(new ConnectionCallback() {
			@Override
			public Object doInConnection(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement("some SQL");
				ps.close();
				assertSame(mockConnection, new PlainNativeJdbcExtractor().getNativeConnection(con));
				return "test";
			}
		});

		assertEquals("test", result);
	}

	public void testCloseConnectionOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		template.query(sql, rcch);

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	/**
	 * Test that we see a runtime exception come back.
	 */
	public void testExceptionComesBack() throws Exception {
		final String sql = "SELECT ID FROM CUSTMR";
		final RuntimeException rex = new RuntimeException("What I want to see");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) {
					throw rex;
				}
			});
			fail("Should have thrown exception");
		}
		catch (RuntimeException ex) {
			assertTrue("Wanted same exception back, not " + ex, ex == rex);
		}
	}

	/**
	 * Test update with static SQL.
	 */
	public void testSqlUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.executeUpdate(sql);
		ctrlStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		int actualRowsAffected = template.update(sql);
		assertTrue("Actual rows affected is correct", actualRowsAffected == rowsAffected);
		ctrlStatement.verify();
	}

	/**
	 * Test update with dynamic SQL.
	 */
	public void testSqlUpdateWithArguments() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ? and PR = ?";
		int rowsAffected = 33;

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.setObject(1, new Integer(4));
		ctrlStatement.setVoidCallable();
		mockStatement.setObject(2, new Float(1.4142), Types.NUMERIC, 2);
		ctrlStatement.setVoidCallable();
		mockStatement.executeUpdate();
		ctrlStatement.setReturnValue(33);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockStatement);
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		int actualRowsAffected = template.update(sql,
				new Object[] {new Integer(4), new SqlParameterValue(Types.NUMERIC, 2, new Float(1.4142))});
		assertTrue("Actual rows affected is correct", actualRowsAffected == rowsAffected);
		ctrlStatement.verify();
	}

	public void testSqlUpdateEncountersSqlException() throws Exception {
		SQLException sex = new SQLException("bad update");
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.executeUpdate(sql);
		ctrlStatement.setThrowable(sex);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.update(sql);
		}
		catch (DataAccessException ex) {
			assertTrue("root cause is correct", ex.getCause() == sex);
		}

		ctrlStatement.verify();
	}

	public void testSqlUpdateWithThreadConnection() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.executeUpdate(sql);
		ctrlStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int actualRowsAffected = template.update(sql);
		assertTrue(
			"Actual rows affected is correct",
			actualRowsAffected == rowsAffected);

		ctrlStatement.verify();
	}

	public void testBatchUpdate() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.getConnection();
		ctrlStatement.setReturnValue(mockConnection);
		mockStatement.addBatch(sql[0]);
		ctrlStatement.setVoidCallable();
		mockStatement.addBatch(sql[1]);
		ctrlStatement.setVoidCallable();
		mockStatement.executeBatch();
		ctrlStatement.setReturnValue(new int[] {1, 1});
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);

		ctrlStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testBatchUpdateWithNoBatchSupport() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.getConnection();
		ctrlStatement.setReturnValue(mockConnection);
		mockStatement.execute(sql[0]);
		ctrlStatement.setReturnValue(false);
		mockStatement.getUpdateCount();
		ctrlStatement.setReturnValue(1);
		mockStatement.execute(sql[1]);
		ctrlStatement.setReturnValue(false);
		mockStatement.getUpdateCount();
		ctrlStatement.setReturnValue(1);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(false);

		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);

		ctrlStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testBatchUpdateWithNoBatchSupportAndSelect() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"SELECT * FROM NOSUCHTABLE"};

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.getConnection();
		ctrlStatement.setReturnValue(mockConnection);
		mockStatement.execute(sql[0]);
		ctrlStatement.setReturnValue(false);
		mockStatement.getUpdateCount();
		ctrlStatement.setReturnValue(1);
		mockStatement.execute(sql[1]);
		ctrlStatement.setReturnValue(true);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(false);

		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		try {
			template.batchUpdate(sql);
			fail("Shouldn't have executed batch statement with a select");
		}
		catch (DataAccessException ex) {
			// pass
			assertTrue("Check exception type", ex.getClass() == InvalidDataAccessApiUsageException.class);
		}

		ctrlStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testBatchUpdateWithPreparedStatement() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchPreparedStatementSetter setter =
			new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i)
				throws SQLException {
				ps.setInt(1, ids[i]);
			}
			@Override
			public int getBatchSize() {
				return ids.length;
			}
		};

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testInterruptibleBatchUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchPreparedStatementSetter setter =
				new InterruptibleBatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						if (i < ids.length) {
							ps.setInt(1, ids[i]);
						}
					}
					@Override
					public int getBatchSize() {
						return 1000;
					}
					@Override
					public boolean isBatchExhausted(int i) {
						return (i >= ids.length);
					}
				};

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testInterruptibleBatchUpdateWithBaseClass() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchPreparedStatementSetter setter =
				new AbstractInterruptibleBatchPreparedStatementSetter() {
					@Override
					protected boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException {
						if (i < ids.length) {
							ps.setInt(1, ids[i]);
							return true;
						}
						else {
							return false;
						}
					}
				};

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testInterruptibleBatchUpdateWithBaseClassAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected[0]);
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected[1]);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(false);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchPreparedStatementSetter setter =
				new AbstractInterruptibleBatchPreparedStatementSetter() {
					@Override
					protected boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException {
						if (i < ids.length) {
							ps.setInt(1, ids[i]);
							return true;
						}
						else {
							return false;
						}
					}
				};

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testBatchUpdateWithPreparedStatementAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected[0]);
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected[1]);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setInt(1, ids[i]);
			}
			@Override
			public int getBatchSize() {
				return ids.length;
			}
		};

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		ctrlPreparedStatement.verify();
	}

	public void testBatchUpdateFails() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		SQLException sex = new SQLException();

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		ctrlConnection.reset();
		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);
		mockConnection.close();
		ctrlConnection.setVoidCallable(2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setInt(1, ids[i]);
			}
			@Override
			public int getBatchSize() {
				return ids.length;
			}
		};

		try {
			JdbcTemplate template = new JdbcTemplate(mockDataSource);
			template.batchUpdate(sql, setter);
			fail("Should have failed because of SQLException in bulk update");
		}
		catch (DataAccessException ex) {
			assertTrue("Root cause is SQLException", ex.getCause() == sex);
		}

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
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

		int[] actualRowsAffected = template.batchUpdate(sql, ids);

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

		int[] actualRowsAffected = template.batchUpdate(sql, ids, sqlTypes);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		BatchUpdateTestHelper.verifyBatchUpdateMocks(ctrlPreparedStatement, ctrlDatabaseMetaData);
	}

	public void testBatchUpdateWithCollectionOfObjects() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Integer> ids = new ArrayList<Integer>();
		ids.add(Integer.valueOf(100));
		ids.add(Integer.valueOf(200));
		ids.add(Integer.valueOf(300));
		final int[] rowsAffected1 = new int[] { 1, 2 };
		final int[] rowsAffected2 = new int[] { 3 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setInt(1, ids.get(0));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids.get(1));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids.get(2));
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected1);
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected2);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		ParameterizedPreparedStatementSetter setter = new ParameterizedPreparedStatementSetter<Integer>() {
			@Override
			public void setValues(PreparedStatement ps, Integer argument) throws SQLException {
				ps.setInt(1, argument.intValue());
			}
		};

		JdbcTemplate template = new JdbcTemplate(mockDataSource, false);

		int[][] actualRowsAffected = template.batchUpdate(sql, ids, 2, setter);
		assertTrue("executed 2 updates", actualRowsAffected[0].length == 2);
		assertEquals(rowsAffected1[0], actualRowsAffected[0][0]);
		assertEquals(rowsAffected1[1], actualRowsAffected[0][1]);
		assertEquals(rowsAffected2[0], actualRowsAffected[1][0]);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testCouldntGetConnectionOrExceptionTranslator() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		// Expect two calls (one call after caching data product name): make get metadata fail also
		ctrlDataSource.setThrowable(sex, 2);
		replay();

		try {
			JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
			fail("Shouldn't have executed query without a connection");
		}
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getCause() == sex);
		}

		ctrlDataSource.verify();
	}

	public void testCouldntGetConnectionForOperationOrExceptionTranslator() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		// Change behavior in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		// Expect two calls (one call after caching data product name): make get Metadata fail also
		ctrlDataSource.setThrowable(sex, 2);
		replay();

		try {
			JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
			fail("Shouldn't have executed query without a connection");
		}
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getCause() == sex);
		}

		ctrlDataSource.verify();
	}

	public void testCouldntGetConnectionForOperationWithLazyExceptionTranslator() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setThrowable(sex, 1);
		replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate();
			template2.setDataSource(mockDataSource);
			template2.afterPropertiesSet();
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
			fail("Shouldn't have executed query without a connection");
		}
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getCause() == sex);
		}

		ctrlDataSource.verify();
	}

	/**
	 * Verify that afterPropertiesSet invokes exception translator.
	 */
	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitialized() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		//mockConnection.getMetaData();
		//ctrlConnection.setReturnValue(null, 1);
		//mockConnection.close();
		//ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();

		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		// Upfront call for metadata - no longer the case
		//mockDataSource.getConnection();
		//ctrlDataSource.setReturnValue(mockConnection, 1);
		// One call for operation
		mockDataSource.getConnection();
		ctrlDataSource.setThrowable(sex, 2);
		ctrlDataSource.replay();

		try {
			JdbcTemplate template = new JdbcTemplate(mockDataSource, false);
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
			fail("Shouldn't have executed query without a connection");
		}
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getCause() == sex);
		}

		ctrlDataSource.verify();
		ctrlConnection.verify();
	}

	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitializedViaBeanProperty()
			throws Exception {
		doTestCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(true);
	}

	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitializedInAfterPropertiesSet()
			throws Exception {
		doTestCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(false);
	}

	/**
	 * If beanProperty is true, initialize via exception translator bean property;
	 * if false, use afterPropertiesSet().
	 */
	private void doTestCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(boolean beanProperty)
			throws SQLException {

		SQLException sex = new SQLException("foo", "07xxx");

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		//mockConnection.getMetaData();
		//ctrlConnection.setReturnValue(null, 1);
		//mockConnection.close();
		//ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();

		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		// Upfront call for metadata - no longer the case
		//mockDataSource.getConnection();
		//ctrlDataSource.setReturnValue(mockConnection, 1);
		// One call for operation
		mockDataSource.getConnection();
		ctrlDataSource.setThrowable(sex, 2);
		ctrlDataSource.replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate();
			template2.setDataSource(mockDataSource);
			template2.setLazyInit(false);
			if (beanProperty) {
				// This will get a connection.
				template2.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(mockDataSource));
			}
			else {
				// This will cause creation of default SQL translator.
				// Note that only call should be effective.
				template2.afterPropertiesSet();
				template2.afterPropertiesSet();
			}
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query(
				"SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3",
				rcch);
			fail("Shouldn't have executed query without a connection");
		}
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getCause() == sex);
		}

		ctrlDataSource.verify();
		ctrlConnection.verify();
	}

	public void testPreparedStatementSetterSucceeds() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		int expectedRowsUpdated = 1;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setString(1, name);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(expectedRowsUpdated);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		int actualRowsUpdated =
			new JdbcTemplate(mockDataSource).update(sql, pss);
		assertTrue(
			"updated correct # of rows",
			actualRowsUpdated == expectedRowsUpdated);

		ctrlPreparedStatement.verify();
	}

	public void testPreparedStatementSetterFails() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		SQLException sex = new SQLException();

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setString(1, name);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		try {
			new JdbcTemplate(mockDataSource).update(sql, pss);
			fail("Should have failed with SQLException");
		}
		catch (DataAccessException ex) {
			assertTrue("root cause was preserved", ex.getCause() == sex);
		}

		ctrlPreparedStatement.verify();
	}

	public void testCouldntClose() throws Exception {
		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		SQLException sex = new SQLException("bar");
		mockResultSet.close();
		ctrlResultSet.setThrowable(sex);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setThrowable(sex);
		mockConnection.close();
		ctrlConnection.setThrowable(sex);

		ctrlStatement.replay();
		ctrlResultSet.replay();
		replay();

		JdbcTemplate template2 = new JdbcTemplate(mockDataSource);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		template2.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);

		ctrlStatement.verify();
		ctrlResultSet.verify();
	}

	/**
	 * Mock objects allow us to produce warnings at will
	 */
	public void testFatalWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(warnings);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate t = new JdbcTemplate(mockDataSource);
		t.setIgnoreWarnings(false);
		try {
			t.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					rs.getByte(1);
				}
			});
			fail("Should have thrown exception on warning");
		}
		catch (SQLWarningException ex) {
			// Pass
			assertTrue(
				"Root cause of warning was correct",
				ex.getCause() == warnings);
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testIgnoredWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		// Too long: truncation
		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		template.setIgnoreWarnings(true);
		template.query(sql, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs)
				throws java.sql.SQLException {
				rs.getByte(1);
			}
		});

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testSQLErrorCodeTranslation() throws Exception {
		final SQLException sex = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					throw sex;
				}
			});
			fail("Should have thrown BadSqlGrammarException");
		}
		catch (BadSqlGrammarException ex) {
			// expected
			assertTrue("Wanted same exception back, not " + ex, sex == ex.getCause());
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

	public void testSQLErrorCodeTranslationWithSpecifiedDbName() throws Exception {
		final SQLException sex = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(mockDataSource);
		template.setDatabaseProductName("MySQL");
		template.afterPropertiesSet();
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					throw sex;
				}
			});
			fail("Should have thrown BadSqlGrammarException");
		}
		catch (BadSqlGrammarException ex) {
			// expected
			assertTrue("Wanted same exception back, not " + ex, sex == ex.getCause());
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	/**
	 * Test that we see an SQLException translated using Error Code.
	 * If we provide the SQLExceptionTranslator, we shouldn't use a connection
	 * to get the metadata
	 */
	public void testUseCustomSQLErrorCodeTranslator() throws Exception {
		// Bad SQL state
		final SQLException sex = new SQLException("I have a known problem", "07000", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement, 1);
		mockConnection.close();
		ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setReturnValue(mockConnection, 1);
		ctrlDataSource.replay();
		///// end changed behaviour

		ctrlResultSet.replay();
		ctrlStatement.replay();

		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(mockDataSource);
		// Set custom exception translator
		template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		template.afterPropertiesSet();
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs)
					throws SQLException {
					throw sex;
				}
			});
			fail("Should have thrown exception");
		}
		catch (BadSqlGrammarException ex) {
			assertTrue(
				"Wanted same exception back, not " + ex,
				sex == ex.getCause());
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();

		// We didn't call superclass replay() so we need to check these ourselves
		ctrlDataSource.verify();
		ctrlConnection.verify();
	}

	public void testNativeJdbcExtractorInvoked() throws Exception {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		final ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.close();
		ctrlResultSet.setVoidCallable(2);

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		final Statement mockStatement = (Statement) ctrlStatement.getMock();
		if (debugEnabled) {
			mockStatement.getWarnings();
			ctrlStatement.setReturnValue(null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();
		MockControl ctrlStatement2 = MockControl.createControl(Statement.class);
		final Statement mockStatement2 = (Statement) ctrlStatement2.getMock();
		mockStatement2.executeQuery("my query");
		ctrlStatement2.setReturnValue(mockResultSet, 1);

		MockControl ctrlPreparedStatement =	MockControl.createControl(PreparedStatement.class);
		final PreparedStatement mockPreparedStatement =	(PreparedStatement) ctrlPreparedStatement.getMock();
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();
		MockControl ctrlPreparedStatement2 =	MockControl.createControl(PreparedStatement.class);
		final PreparedStatement mockPreparedStatement2 =	(PreparedStatement) ctrlPreparedStatement2.getMock();
		mockPreparedStatement2.executeQuery();
		ctrlPreparedStatement2.setReturnValue(mockResultSet, 1);

		MockControl ctrlReturnResultSet = MockControl.createControl(ResultSet.class);
		final ResultSet mockReturnResultSet = (ResultSet) ctrlReturnResultSet.getMock();
		mockReturnResultSet.next();
		ctrlReturnResultSet.setReturnValue(false);
		mockReturnResultSet.close();
		ctrlReturnResultSet.setVoidCallable(2);

		MockControl ctrlCallableStatement =	MockControl.createControl(CallableStatement.class);
		final CallableStatement mockCallableStatement =	(CallableStatement) ctrlCallableStatement.getMock();
		if (debugEnabled) {
			mockCallableStatement.getWarnings();
			ctrlCallableStatement.setReturnValue(null);
		}
		mockCallableStatement.close();
		ctrlCallableStatement.setVoidCallable();
		MockControl ctrlCallableStatement2 = MockControl.createControl(CallableStatement.class);
		final CallableStatement mockCallableStatement2 = (CallableStatement) ctrlCallableStatement2.getMock();
		mockCallableStatement2.execute();
		ctrlCallableStatement2.setReturnValue(true);
		mockCallableStatement2.getUpdateCount();
		ctrlCallableStatement2.setReturnValue(-1);
		mockCallableStatement2.getResultSet();
		ctrlCallableStatement2.setReturnValue(mockReturnResultSet);
		mockCallableStatement2.getMoreResults();
		ctrlCallableStatement2.setReturnValue(false);
		mockCallableStatement2.getUpdateCount();
		ctrlCallableStatement2.setReturnValue(-1);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		ctrlStatement2.replay();
		ctrlPreparedStatement.replay();
		ctrlPreparedStatement2.replay();
		ctrlReturnResultSet.replay();;
		ctrlCallableStatement.replay();
		ctrlCallableStatement2.replay();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement, 1);
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		template.setNativeJdbcExtractor(new NativeJdbcExtractor() {
			@Override
			public boolean isNativeConnectionNecessaryForNativeStatements() {
				return false;
			}
			@Override
			public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
				return false;
			}
			@Override
			public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
				return false;
			}
			@Override
			public Connection getNativeConnection(Connection con) {
				return con;
			}
			@Override
			public Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException {
				return stmt.getConnection();
			}
			@Override
			public Statement getNativeStatement(Statement stmt) {
				assertTrue(stmt == mockStatement);
				return mockStatement2;
			}
			@Override
			public PreparedStatement getNativePreparedStatement(PreparedStatement ps) {
				assertTrue(ps == mockPreparedStatement);
				return mockPreparedStatement2;
			}
			@Override
			public CallableStatement getNativeCallableStatement(CallableStatement cs) {
				assertTrue(cs == mockCallableStatement);
				return mockCallableStatement2;
			}
			@Override
			public ResultSet getNativeResultSet(ResultSet rs) {
				return rs;
			}
		});

		template.query("my query",	new ResultSetExtractor() {
			@Override
			public Object extractData(ResultSet rs2) {
				assertEquals(mockResultSet, rs2);
				return null;
			}
		});

		template.query(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection conn) {
				return mockPreparedStatement;
			}
		}, new ResultSetExtractor() {
			@Override
			public Object extractData(ResultSet rs2) {
				assertEquals(mockResultSet, rs2);
				return null;
			}
		});

		template.call(new CallableStatementCreator() {
			@Override
			public CallableStatement createCallableStatement(Connection con) {
				return mockCallableStatement;
			}
		},
		new ArrayList());

		ctrlStatement.verify();
		ctrlStatement2.verify();
		ctrlPreparedStatement.verify();
		ctrlPreparedStatement2.verify();
		ctrlCallableStatement.verify();
		ctrlCallableStatement2.verify();
	}

	public void testStaticResultSetClosed() throws Exception {
		MockControl ctrlResultSet;
		ResultSet mockResultSet;
		MockControl ctrlStatement;
		Statement mockStatement;
		MockControl ctrlResultSet2;
		ResultSet mockResultSet2;
		MockControl ctrlPreparedStatement;
		PreparedStatement mockPreparedStatement;

		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		ctrlStatement = MockControl.createControl(Statement.class);
		mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.executeQuery("my query");
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		ctrlResultSet2 = MockControl.createControl(ResultSet.class);
		mockResultSet2 = (ResultSet) ctrlResultSet2.getMock();
		mockResultSet2.close();
		ctrlResultSet2.setVoidCallable();

		ctrlPreparedStatement =	MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement =	(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet2);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		mockConnection.prepareStatement("my query");
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		ctrlResultSet2.replay();
		ctrlPreparedStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		try {
			template.query("my query", new ResultSetExtractor() {
				@Override
				public Object extractData(ResultSet rs) {
					throw new InvalidDataAccessApiUsageException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		try {
			template.query(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					return con.prepareStatement("my query");
				}
				public String getSql() {
					return null;
				}
			}, new ResultSetExtractor() {
				@Override
				public Object extractData(ResultSet rs2) {
					throw new InvalidDataAccessApiUsageException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		// verify confirms if test is successful by checking if close() called
		ctrlResultSet.verify();
		ctrlStatement.verify();
		ctrlResultSet2.verify();
		ctrlPreparedStatement.verify();
	}

	public void testExecuteClosed() throws Exception {
		MockControl ctrlResultSet;
		ResultSet mockResultSet;
		MockControl ctrlCallable;
		CallableStatement mockCallable;

		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		ctrlCallable = MockControl.createControl(CallableStatement.class);
		mockCallable = (CallableStatement) ctrlCallable.getMock();
		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet);
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("my query");
		ctrlConnection.setReturnValue(mockCallable);

		ctrlResultSet.replay();
		ctrlCallable.replay();
		replay();

		List params = new ArrayList();
		params.add(new SqlReturnResultSet("", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) {
				throw new InvalidDataAccessApiUsageException("");
			}

		}));

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.call(new CallableStatementCreator() {
				@Override
				public CallableStatement createCallableStatement(Connection conn) throws SQLException {
					return conn.prepareCall("my query");
				}
			}, params);
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		// verify confirms if test is successful by checking if close() called
		ctrlResultSet.verify();
		ctrlCallable.verify();
	}

	public void testCaseInsensitiveResultsMap() throws Exception {

		MockControl ctrlCallable;
		CallableStatement mockCallable;

		ctrlCallable = MockControl.createControl(CallableStatement.class);
		mockCallable = (CallableStatement) ctrlCallable.getMock();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(1);
		ctrlCallable.setReturnValue("X");
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("my query");
		ctrlConnection.setReturnValue(mockCallable);

		ctrlCallable.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		assertTrue("default should have been NOT case insensitive", !template.isResultsMapCaseInsensitive());

		template.setResultsMapCaseInsensitive(true);
		assertTrue("now it should have been set to case insensitive", template.isResultsMapCaseInsensitive());

		List params = new ArrayList();
		params.add(new SqlOutParameter("a", 12));

		Map out = template.call(new CallableStatementCreator() {
			@Override
			public CallableStatement createCallableStatement(Connection conn) throws SQLException {
				return conn.prepareCall("my query");
			}
		}, params);
		assertTrue("this should have been a LinkedCaseInsensitiveMap", out instanceof LinkedCaseInsensitiveMap);
		assertNotNull("we should have gotten the result with upper case", out.get("A"));
		assertNotNull("we should have gotten the result with lower case", out.get("a"));

		ctrlCallable.verify();
	}


	private static class PlainNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

		@Override
		protected Connection doGetNativeConnection(Connection con) throws SQLException {
			return con;
		}
	}


	private static interface JdbcTemplateCallback {

		void doInJdbcTemplate(JdbcTemplate template, String sql, RowCallbackHandler rch);
	}


	private static class Dispatcher implements PreparedStatementCreator, SqlProvider {

		private int id;
		private String sql;

		public Dispatcher(int id, String sql) {
			this.id = id;
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, id);
			return ps;
		}

		@Override
		public String getSql() {
			return sql;
		}
	}

}
