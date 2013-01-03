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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.tests.Matchers.exceptionCause;

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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
 * @author Phillip Webb
 */
public class JdbcTemplateTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Connection connection;
	private DataSource dataSource;
	private PreparedStatement preparedStatement;
	private Statement statement;
	private ResultSet resultSet;
	private JdbcTemplate template;
	private CallableStatement callableStatement;

	@Before
	public void setup() throws Exception {
		this.connection = mock(Connection.class);
		this.dataSource = mock(DataSource.class);
		this.preparedStatement = mock(PreparedStatement.class);
		this.statement = mock(Statement.class);
		this.resultSet = mock(ResultSet.class);
		this.template = new JdbcTemplate(this.dataSource);
		this.callableStatement = mock(CallableStatement.class);
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.preparedStatement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.preparedStatement.getConnection()).willReturn(this.connection);
		given(this.statement.getConnection()).willReturn(this.connection);
		given(this.statement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.connection.prepareCall(anyString())).willReturn(this.callableStatement);
		given(this.callableStatement.getResultSet()).willReturn(this.resultSet);
	}

	@Test
	public void testBeanProperties() throws Exception {
		assertTrue("datasource ok", this.template.getDataSource() == this.dataSource);
		assertTrue("ignores warnings by default", this.template.isIgnoreWarnings());
		this.template.setIgnoreWarnings(false);
		assertTrue("can set NOT to ignore warnings", !this.template.isIgnoreWarnings());
	}

	@Test
	public void testUpdateCount() throws Exception {
		final String sql = "UPDATE INVOICE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		int idParam = 11111;
		given(this.preparedStatement.executeUpdate()).willReturn(1);
		Dispatcher d = new Dispatcher(idParam, sql);
		int rowsAffected = this.template.update(d);
		assertTrue("1 update affected 1 row", rowsAffected == 1);
		verify(this.preparedStatement).setInt(1, idParam);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testBogusUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int idParam = 6666;

		// It's because Integers aren't canonical
		SQLException sqlException = new SQLException("bad update");
		given(this.preparedStatement.executeUpdate()).willThrow(sqlException);

		Dispatcher d = new Dispatcher(idParam, sql);
		this.thrown.expect(UncategorizedSQLException.class);
		this.thrown.expect(exceptionCause(equalTo(sqlException)));
		try {
			this.template.update(d);
		}
		finally {
			verify(this.preparedStatement).setInt(1, idParam);
			verify(this.preparedStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testStringsWithStaticSql() throws Exception {
		doTestStrings(false, null, null, null, null, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, rch);
			}
		});
	}

	@Test
	public void testStringsWithStaticSqlAndFetchSizeAndMaxRows() throws Exception {
		doTestStrings(false, 10, 20, 30, null, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, rch);
			}
		});
	}

	@Test
	public void testStringsWithEmptyPreparedStatementSetter() throws Exception {
		doTestStrings(true, null, null, null, null, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, (PreparedStatementSetter) null, rch);
			}
		});
	}

	@Test
	public void testStringsWithPreparedStatementSetter() throws Exception {
		final Integer argument = 99;
		doTestStrings(true, null, null, null, argument, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, new PreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setObject(1, argument);
					}
				}, rch);
			}
		});
	}

	@Test
	public void testStringsWithEmptyPreparedStatementArgs() throws Exception {
		doTestStrings(true, null, null, null, null, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, (Object[]) null, rch);
			}
		});
	}

	@Test
	public void testStringsWithPreparedStatementArgs() throws Exception {
		final Integer argument = 99;
		doTestStrings(true, null, null, null, argument, new JdbcTemplateCallback() {
			@Override
			public void doInJdbcTemplate(JdbcTemplate template, String sql,
					RowCallbackHandler rch) {
				template.query(sql, new Object[] { argument }, rch);
			}
		});
	}

	private void doTestStrings(
			boolean usePreparedStatement,
			Integer fetchSize, Integer maxRows, Integer queryTimeout, Object argument,
			JdbcTemplateCallback jdbcTemplateCallback)
			throws Exception {

		String sql = "SELECT FORENAME FROM CUSTMR";
		String[] results = { "rod", "gary", " portia" };

		class StringHandler implements RowCallbackHandler {
			private List<String> list = new LinkedList<String>();
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				this.list.add(rs.getString(1));
			}
			public String[] getStrings() {
				return this.list.toArray(new String[this.list.size()]);
			}
		}

		given(this.resultSet.next()).willReturn(true, true, true, false);
		given(this.resultSet.getString(1)).willReturn(results[0], results[1], results[2]);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		StringHandler sh = new StringHandler();
		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(this.dataSource);
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

		if (fetchSize != null) {
			verify(this.preparedStatement).setFetchSize(fetchSize.intValue());
		}
		if (maxRows != null) {
			verify(this.preparedStatement).setMaxRows(maxRows.intValue());
		}
		if (queryTimeout != null) {
			verify(this.preparedStatement).setQueryTimeout(queryTimeout.intValue());
		}
		if (argument != null) {
			verify(this.preparedStatement).setObject(1, argument);
		}
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testLeaveConnectionOpenOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		given(this.resultSet.next()).willReturn(false);
		given(this.connection.isClosed()).willReturn(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);
		// if close is called entire test will fail
		willThrow(new RuntimeException()).given(this.connection).close();

		SingleConnectionDataSource scf = new SingleConnectionDataSource(this.dataSource.getConnection(), false);
		this.template = new JdbcTemplate(scf, false);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.template.query(sql, rcch);

		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
	}

	@Test
	public void testConnectionCallback() throws Exception {
		this.template.setNativeJdbcExtractor(new PlainNativeJdbcExtractor());
		String result = this.template.execute(new ConnectionCallback<String>() {
			@Override
			public String doInConnection(Connection con) {
				assertSame(JdbcTemplateTests.this.connection, con);
				return "test";
			}
		});
		assertEquals("test", result);
	}

	@Test
	public void testConnectionCallbackWithStatementSettings() throws Exception {
		String result = this.template.execute(new ConnectionCallback<String>() {
			@Override
			public String doInConnection(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement("some SQL");
				ps.setFetchSize(10);
				ps.setMaxRows(20);
				ps.close();
				assertSame(JdbcTemplateTests.this.connection, new PlainNativeJdbcExtractor().getNativeConnection(con));
				return "test";
			}
		});

		assertEquals("test", result);
		verify(this.preparedStatement).setFetchSize(10);
		verify(this.preparedStatement).setMaxRows(20);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testCloseConnectionOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		given(this.resultSet.next()).willReturn(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.template.query(sql, rcch);

		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	/**
	 * Test that we see a runtime exception come back.
	 */
	@Test
	public void testExceptionComesBack() throws Exception {
		final String sql = "SELECT ID FROM CUSTMR";
		final RuntimeException runtimeException = new RuntimeException("Expected");

		given(this.resultSet.next()).willReturn(true);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		this.thrown.expect(sameInstance(runtimeException));
		try {
			this.template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) {
					throw runtimeException;
				}
			});
		}
		finally {
			verify(this.resultSet).close();
			verify(this.preparedStatement).close();
			verify(this.connection).close();
		}
	}

	/**
	 * Test update with static SQL.
	 */
	@Test
	public void testSqlUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		given(this.statement.executeUpdate(sql)).willReturn(rowsAffected);
		given(this.connection.createStatement()).willReturn(this.statement);

		int actualRowsAffected = this.template.update(sql);
		assertTrue("Actual rows affected is correct", actualRowsAffected == rowsAffected);
		verify(this.statement).close();
		verify(this.connection).close();
	}

	/**
	 * Test update with dynamic SQL.
	 */
	@Test
	public void testSqlUpdateWithArguments() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ? and PR = ?";
		int rowsAffected = 33;
		given(this.preparedStatement.executeUpdate()).willReturn(rowsAffected);

		int actualRowsAffected = this.template.update(sql,
				new Object[] {4, new SqlParameterValue(Types.NUMERIC, 2, new Float(1.4142))});
		assertTrue("Actual rows affected is correct", actualRowsAffected == rowsAffected);
		verify(this.preparedStatement).setObject(1, 4);
		verify(this.preparedStatement).setObject(2, new Float(1.4142), Types.NUMERIC, 2);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testSqlUpdateEncountersSqlException() throws Exception {
		SQLException sqlException = new SQLException("bad update");
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";

		given(this.statement.executeUpdate(sql)).willThrow(sqlException);
		given(this.connection.createStatement()).willReturn(this.statement);

		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			this.template.update(sql);
		}
		finally {
			verify(this.statement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testSqlUpdateWithThreadConnection() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		given(this.statement.executeUpdate(sql)).willReturn(rowsAffected);
		given(this.connection.createStatement()).willReturn(this.statement);

		int actualRowsAffected = this.template.update(sql);
		assertTrue("Actual rows affected is correct", actualRowsAffected == rowsAffected);

		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	public void testBatchUpdate() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		given(this.statement.executeBatch()).willReturn(new int[] {1, 1});
		mockDatabaseMetaData(true);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);

		verify(this.statement).addBatch(sql[0]);
		verify(this.statement).addBatch(sql[1]);
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithNoBatchSupport() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		given(this.statement.execute(sql[0])).willReturn(false);
		given(this.statement.getUpdateCount()).willReturn(1, 1);
		given(this.statement.execute(sql[1])).willReturn(false);

		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);

		verify(this.statement, never()).addBatch(anyString());
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithNoBatchSupportAndSelect() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"SELECT * FROM NOSUCHTABLE"};

		given(this.statement.execute(sql[0])).willReturn(false);
		given(this.statement.getUpdateCount()).willReturn(1);
		given(this.statement.execute(sql[1])).willReturn(true);
		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		try {
			template.batchUpdate(sql);
		}
		finally {
			verify(this.statement, never()).addBatch(anyString());
			verify(this.statement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testBatchUpdateWithPreparedStatement() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);

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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testInterruptibleBatchUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);

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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testInterruptibleBatchUpdateWithBaseClass() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);

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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testInterruptibleBatchUpdateWithBaseClassAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);
		mockDatabaseMetaData(false);

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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, never()).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithPreparedStatementAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);

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

		int[] actualRowsAffected = this.template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, never()).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testBatchUpdateFails() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		SQLException sqlException = new SQLException();

		given(this.preparedStatement.executeBatch()).willThrow(sqlException);
		mockDatabaseMetaData(true);

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

		this.thrown.expect(DataAccessException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			this.template.batchUpdate(sql, setter);
		}
		finally {
			verify(this.preparedStatement, times(2)).addBatch();
			verify(this.preparedStatement).setInt(1, ids[0]);
			verify(this.preparedStatement).setInt(1, ids[1]);
			verify(this.preparedStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testBatchUpdateWithListOfObjectArrays() throws Exception {

		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, ids);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setObject(1, 100);
		verify(this.preparedStatement).setObject(1, 200);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithListOfObjectArraysPlusTypeInfo() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<Object[]>();
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] sqlTypes = new int[] {Types.NUMERIC};
		final int[] rowsAffected = new int[] { 1, 2 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);

		this.template = new JdbcTemplate(this.dataSource, false);
		int[] actualRowsAffected = this.template.batchUpdate(sql, ids, sqlTypes);

		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);
		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setObject(1, 100, sqlTypes[0]);
		verify(this.preparedStatement).setObject(1, 200, sqlTypes[0]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testBatchUpdateWithCollectionOfObjects() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Integer> ids = Arrays.asList(100, 200, 300);
		final int[] rowsAffected1 = new int[] { 1, 2 };
		final int[] rowsAffected2 = new int[] { 3 };

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected1, rowsAffected2);
		mockDatabaseMetaData(true);

		ParameterizedPreparedStatementSetter<Integer> setter = new ParameterizedPreparedStatementSetter<Integer>() {
			@Override
			public void setValues(PreparedStatement ps, Integer argument) throws SQLException {
				ps.setInt(1, argument.intValue());
			}
		};

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[][] actualRowsAffected = template.batchUpdate(sql, ids, 2, setter);
		assertTrue("executed 2 updates", actualRowsAffected[0].length == 2);
		assertEquals(rowsAffected1[0], actualRowsAffected[0][0]);
		assertEquals(rowsAffected1[1], actualRowsAffected[0][1]);
		assertEquals(rowsAffected2[0], actualRowsAffected[1][0]);

		verify(this.preparedStatement, times(3)).addBatch();
		verify(this.preparedStatement).setInt(1, ids.get(0));
		verify(this.preparedStatement).setInt(1, ids.get(1));
		verify(this.preparedStatement).setInt(1, ids.get(2));
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testCouldntGetConnectionForOperationOrExceptionTranslator() throws SQLException {
		SQLException sqlException = new SQLException("foo", "07xxx");
		this.dataSource = mock(DataSource.class);
		given(this.dataSource.getConnection()).willThrow(sqlException);
		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.thrown.expect(CannotGetJdbcConnectionException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
	}

	@Test
	public void testCouldntGetConnectionForOperationWithLazyExceptionTranslator() throws SQLException {
		SQLException sqlException = new SQLException("foo", "07xxx");
		this.dataSource = mock(DataSource.class);
		given(this.dataSource.getConnection()).willThrow(sqlException);
		this.template = new JdbcTemplate();
		this.template.setDataSource(this.dataSource);
		this.template.afterPropertiesSet();
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.thrown.expect(CannotGetJdbcConnectionException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		this.template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
	}

	@Test
	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitializedViaBeanProperty()
			throws Exception {
		doTestCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(true);
	}

	@Test
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
		SQLException sqlException = new SQLException("foo", "07xxx");
		this.dataSource = mock(DataSource.class);
		given(this.dataSource.getConnection()).willThrow(sqlException);
		this.template = new JdbcTemplate();
		this.template.setDataSource(this.dataSource);
		this.template.setLazyInit(false);
		if (beanProperty) {
			// This will get a connection.
			this.template.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(this.dataSource));
		}
		else {
			// This will cause creation of default SQL translator.
			this.template.afterPropertiesSet();
		}
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.thrown.expect(CannotGetJdbcConnectionException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		this.template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
	}

	@Test
	public void testPreparedStatementSetterSucceeds() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		int expectedRowsUpdated = 1;

		given(this.preparedStatement.executeUpdate()).willReturn(expectedRowsUpdated);

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		int actualRowsUpdated = new JdbcTemplate(this.dataSource).update(sql, pss);
		assertTrue("updated correct # of rows", actualRowsUpdated == expectedRowsUpdated);
		verify(this.preparedStatement).setString(1, name);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testPreparedStatementSetterFails() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		SQLException sqlException = new SQLException();
		given(this.preparedStatement.executeUpdate()).willThrow(sqlException);

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		this.thrown.expect(DataAccessException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			new JdbcTemplate(this.dataSource).update(sql, pss);
		}
		finally {
			verify(this.preparedStatement).setString(1, name);
			verify(this.preparedStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testCouldntClose() throws Exception {
		SQLException sqlException = new SQLException("bar");
		given(this.connection.createStatement()).willReturn(this.statement);
		given(this.resultSet.next()).willReturn(false);
		willThrow(sqlException).given(this.resultSet).close();
		willThrow(sqlException).given(this.statement).close();
		willThrow(sqlException).given(this.connection).close();

		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		this.template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch);
		verify(this.connection).close();
	}

	/**
	 * Mock objects allow us to produce warnings at will
	 */
	@Test
	public void testFatalWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		given(this.resultSet.next()).willReturn(false);
		given(this.preparedStatement.getWarnings()).willReturn(warnings);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		JdbcTemplate t = new JdbcTemplate(this.dataSource);
		t.setIgnoreWarnings(false);
		this.thrown.expect(SQLWarningException.class);
		this.thrown.expect(exceptionCause(sameInstance(warnings)));
		try {
			t.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					rs.getByte(1);
				}
			});
		}
		finally {
			verify(this.resultSet).close();
			verify(this.preparedStatement).close();
			verify(this.connection).close();
		}
	}

	@Test
	public void testIgnoredWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		given(this.resultSet.next()).willReturn(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);
		given(this.preparedStatement.getWarnings()).willReturn(warnings);

		// Too long: truncation

		this.template.setIgnoreWarnings(true);
		this.template.query(sql, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws java.sql.SQLException {
				rs.getByte(1);
			}
		});

		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	public void testSQLErrorCodeTranslation() throws Exception {
		final SQLException sqlException = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		given(this.resultSet.next()).willReturn(true);
		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		this.thrown.expect(BadSqlGrammarException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			this.template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					throw sqlException;
				}
			});
			fail("Should have thrown BadSqlGrammarException");
		}
		finally {
			verify(this.resultSet).close();
			verify(this.preparedStatement).close();
			verify(this.connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testSQLErrorCodeTranslationWithSpecifiedDbName() throws Exception {
		final SQLException sqlException = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		given(this.resultSet.next()).willReturn(true);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(this.dataSource);
		template.setDatabaseProductName("MySQL");
		template.afterPropertiesSet();

		this.thrown.expect(BadSqlGrammarException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					throw sqlException;
				}
			});
		}
		finally {
			verify(this.resultSet).close();
			verify(this.preparedStatement).close();
			verify(this.connection).close();
		}
	}

	/**
	 * Test that we see an SQLException translated using Error Code.
	 * If we provide the SQLExceptionTranslator, we shouldn't use a connection
	 * to get the metadata
	 */
	@Test
	public void testUseCustomSQLErrorCodeTranslator() throws Exception {
		// Bad SQL state
		final SQLException sqlException = new SQLException("I have a known problem", "07000", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		given(this.resultSet.next()).willReturn(true);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(this.dataSource);
		// Set custom exception translator
		template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		template.afterPropertiesSet();

		this.thrown.expect(BadSqlGrammarException.class);
		this.thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			template.query(sql, new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
					throw sqlException;
				}
			});
		}
		finally {
			verify(this.resultSet).close();
			verify(this.preparedStatement).close();
			verify(this.connection).close();
		}
	}

	@Test
	public void testNativeJdbcExtractorInvoked() throws Exception {

		final Statement statement2 = mock(Statement.class);
		given(statement2.executeQuery(anyString())).willReturn(this.resultSet);

		final PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
		given(preparedStatement2.executeQuery()).willReturn(this.resultSet);

		final ResultSet returnResultSet = mock(ResultSet.class);
		given(returnResultSet.next()).willReturn(false);

		final CallableStatement callableStatement = mock(CallableStatement.class);
		final CallableStatement callableStatement2 = mock(CallableStatement.class);
		given(callableStatement2.execute()).willReturn(true);
		given(callableStatement2.getUpdateCount()).willReturn(-1);
		given(callableStatement2.getResultSet()).willReturn(returnResultSet);
		given(callableStatement2.getUpdateCount()).willReturn(-1);

		given(this.connection.createStatement()).willReturn(this.statement);

		this.template.setNativeJdbcExtractor(new NativeJdbcExtractor() {
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
				assertTrue(stmt == JdbcTemplateTests.this.statement);
				return statement2;
			}
			@Override
			public PreparedStatement getNativePreparedStatement(PreparedStatement ps) {
				assertTrue(ps == JdbcTemplateTests.this.preparedStatement);
				return preparedStatement2;
			}
			@Override
			public CallableStatement getNativeCallableStatement(CallableStatement cs) {
				assertTrue(cs == callableStatement);
				return callableStatement2;
			}
			@Override
			public ResultSet getNativeResultSet(ResultSet rs) {
				return rs;
			}
		});

		this.template.query("my query", new ResultSetExtractor<Object>() {
			@Override
			public Object extractData(ResultSet rs2) {
				assertEquals(JdbcTemplateTests.this.resultSet, rs2);
				return null;
			}
		});

		this.template.query(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection conn) {
				return JdbcTemplateTests.this.preparedStatement;
			}
		}, new ResultSetExtractor<Object>() {
			@Override
			public Object extractData(ResultSet rs2) {
				assertEquals(JdbcTemplateTests.this.resultSet, rs2);
				return null;
			}
		});

		this.template.call(new CallableStatementCreator() {
			@Override
			public CallableStatement createCallableStatement(Connection con) {
				return callableStatement;
			}
		}, new ArrayList<SqlParameter>());

		verify(this.resultSet, times(2)).close();
		verify(this.statement).close();
		verify(this.preparedStatement).close();
		verify(returnResultSet).close();
		verify(callableStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testStaticResultSetClosed() throws Exception {
		ResultSet resultSet2 = mock(ResultSet.class);
		reset(this.preparedStatement);
		given(this.preparedStatement.executeQuery()).willReturn(resultSet2);
		given(this.connection.createStatement()).willReturn(this.statement);

		try {
			this.template.query("my query", new ResultSetExtractor<Object>() {
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
			this.template.query(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con)
						throws SQLException {
					return con.prepareStatement("my query");
				}
			}, new ResultSetExtractor<Object>() {
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

		verify(this.resultSet).close();
		verify(resultSet2).close();
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	public void testExecuteClosed() throws Exception {
		given(this.resultSet.next()).willReturn(true);
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);

		List<SqlParameter> params = new ArrayList<SqlParameter>();
		params.add(new SqlReturnResultSet("", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) {
				throw new InvalidDataAccessApiUsageException("");
			}

		}));

		this.thrown.expect(InvalidDataAccessApiUsageException.class);
		try {
			this.template.call(new CallableStatementCreator() {
				@Override
				public CallableStatement createCallableStatement(Connection conn)
						throws SQLException {
					return conn.prepareCall("my query");
				}
			}, params);
		}
		finally {
			verify(this.resultSet).close();
			verify(this.callableStatement).close();
			verify(this.connection).close();
		}
	}

	@Test
	public void testCaseInsensitiveResultsMap() throws Exception {

		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(1)).willReturn("X");

		assertTrue("default should have been NOT case insensitive",
				!this.template.isResultsMapCaseInsensitive());

		this.template.setResultsMapCaseInsensitive(true);
		assertTrue("now it should have been set to case insensitive",
				this.template.isResultsMapCaseInsensitive());

		List<SqlParameter> params = new ArrayList<SqlParameter>();
		params.add(new SqlOutParameter("a", 12));

		Map out = this.template.call(new CallableStatementCreator() {
			@Override
			public CallableStatement createCallableStatement(Connection conn)
					throws SQLException {
				return conn.prepareCall("my query");
			}
		}, params);

		assertThat(out, instanceOf(LinkedCaseInsensitiveMap.class));
		assertNotNull("we should have gotten the result with upper case", out.get("A"));
		assertNotNull("we should have gotten the result with lower case", out.get("a"));
		verify(this.callableStatement).close();
		verify(this.connection).close();
	}

	private void mockDatabaseMetaData(boolean supportsBatchUpdates) throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
		given(databaseMetaData.supportsBatchUpdates()).willReturn(supportsBatchUpdates);
		given(this.connection.getMetaData()).willReturn(databaseMetaData);
	}

	private static class PlainNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

		@Override
		protected Connection doGetNativeConnection(Connection connection) throws SQLException {
			return connection;
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
		public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
			PreparedStatement ps = connection.prepareStatement(this.sql);
			ps.setInt(1, this.id);
			return ps;
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}
}
