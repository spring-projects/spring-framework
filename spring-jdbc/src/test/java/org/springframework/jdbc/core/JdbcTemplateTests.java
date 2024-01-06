/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core;

import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for JdbcTemplate.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
class JdbcTemplateTests {

	private DataSource dataSource = mock();

	private Connection connection = mock();

	private Statement statement = mock();

	private PreparedStatement preparedStatement = mock();

	private CallableStatement callableStatement = mock();

	private ResultSet resultSet = mock();

	private JdbcTemplate template = new JdbcTemplate(this.dataSource);


	@BeforeEach
	void setup() throws Exception {
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.connection.prepareCall(anyString())).willReturn(this.callableStatement);
		given(this.statement.getConnection()).willReturn(this.connection);
		given(this.statement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.preparedStatement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.preparedStatement.getConnection()).willReturn(this.connection);
		given(this.callableStatement.getResultSet()).willReturn(this.resultSet);
	}


	@Test
	void testBeanProperties() {
		assertThat(this.template.getDataSource()).as("datasource ok").isSameAs(this.dataSource);
		assertThat(this.template.isIgnoreWarnings()).as("ignores warnings by default").isTrue();
		this.template.setIgnoreWarnings(false);
		boolean condition = !this.template.isIgnoreWarnings();
		assertThat(condition).as("can set NOT to ignore warnings").isTrue();
	}

	@Test
	void testUpdateCount() throws Exception {
		final String sql = "UPDATE INVOICE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		int idParam = 11111;
		given(this.preparedStatement.executeUpdate()).willReturn(1);
		Dispatcher d = new Dispatcher(idParam, sql);
		int rowsAffected = this.template.update(d);
		assertThat(rowsAffected).as("1 update affected 1 row").isEqualTo(1);
		verify(this.preparedStatement).setInt(1, idParam);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testBogusUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int idParam = 6666;

		// It's because Integers aren't canonical
		SQLException sqlException = new SQLException("bad update");
		given(this.preparedStatement.executeUpdate()).willThrow(sqlException);

		Dispatcher d = new Dispatcher(idParam, sql);
		assertThatExceptionOfType(UncategorizedSQLException.class)
				.isThrownBy(() -> this.template.update(d))
				.withCause(sqlException);
		verify(this.preparedStatement).setInt(1, idParam);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testStringsWithStaticSql() throws Exception {
		doTestStrings(null, null, null, null, JdbcTemplate::query);
	}

	@Test
	void testStringsWithStaticSqlAndFetchSizeAndMaxRows() throws Exception {
		doTestStrings(10, 20, 30, null, JdbcTemplate::query);
	}

	@Test
	void testStringsWithEmptyPreparedStatementSetter() throws Exception {
		doTestStrings(null, null, null, null, (template, sql, rch) ->
				template.query(sql, (PreparedStatementSetter) null, rch));
	}

	@Test
	void testStringsWithPreparedStatementSetter() throws Exception {
		final Integer argument = 99;
		doTestStrings(null, null, null, argument, (template, sql, rch) ->
			template.query(sql, ps -> ps.setObject(1, argument), rch));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testStringsWithEmptyPreparedStatementArgs() throws Exception {
		doTestStrings(null, null, null, null,
				(template, sql, rch) -> template.query(sql, (Object[]) null, rch));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testStringsWithPreparedStatementArgs() throws Exception {
		final Integer argument = 99;
		doTestStrings(null, null, null, argument,
				(template, sql, rch) -> template.query(sql, new Object[] {argument}, rch));
	}

	private void doTestStrings(Integer fetchSize, Integer maxRows, Integer queryTimeout,
			Object argument, JdbcTemplateCallback jdbcTemplateCallback) throws Exception {

		String sql = "SELECT FORENAME FROM CUSTMR";
		String[] results = {"rod", "gary", " portia"};

		class StringHandler implements RowCallbackHandler {
			private List<String> list = new ArrayList<>();
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				this.list.add(rs.getString(1));
			}
			public String[] getStrings() {
				return StringUtils.toStringArray(this.list);
			}
		}

		given(this.resultSet.next()).willReturn(true, true, true, false);
		given(this.resultSet.getString(1)).willReturn(results[0], results[1], results[2]);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		StringHandler sh = new StringHandler();
		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(this.dataSource);
		if (fetchSize != null) {
			template.setFetchSize(fetchSize);
		}
		if (maxRows != null) {
			template.setMaxRows(maxRows);
		}
		if (queryTimeout != null) {
			template.setQueryTimeout(queryTimeout);
		}
		jdbcTemplateCallback.doInJdbcTemplate(template, sql, sh);

		// Match
		String[] forenames = sh.getStrings();
		assertThat(forenames).as("same length").hasSameSizeAs(results);
		for (int i = 0; i < forenames.length; i++) {
			assertThat(forenames[i]).as("Row " + i + " matches").isEqualTo(results[i]);
		}

		if (fetchSize != null) {
			verify(this.preparedStatement).setFetchSize(fetchSize);
		}
		if (maxRows != null) {
			verify(this.preparedStatement).setMaxRows(maxRows);
		}
		if (queryTimeout != null) {
			verify(this.preparedStatement).setQueryTimeout(queryTimeout);
		}
		if (argument != null) {
			verify(this.preparedStatement).setObject(1, argument);
		}
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testLeaveConnectionOpenOnRequest() throws Exception {
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
	void testConnectionCallback() {
		String result = this.template.execute((ConnectionCallback<String>) con -> {
			assertThat(con).isInstanceOf(ConnectionProxy.class);
			assertThat(((ConnectionProxy) con).getTargetConnection()).isSameAs(JdbcTemplateTests.this.connection);
			return "test";
		});
		assertThat(result).isEqualTo("test");
	}

	@Test
	void testConnectionCallbackWithStatementSettings() throws Exception {
		String result = this.template.execute((ConnectionCallback<String>) con -> {
			PreparedStatement ps = con.prepareStatement("some SQL");
			ps.setFetchSize(10);
			ps.setMaxRows(20);
			ps.close();
			return "test";
		});

		assertThat(result).isEqualTo("test");
		verify(this.preparedStatement).setFetchSize(10);
		verify(this.preparedStatement).setMaxRows(20);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testCloseConnectionOnRequest() throws Exception {
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
	void testExceptionComesBack() throws Exception {
		final String sql = "SELECT ID FROM CUSTMR";
		final RuntimeException runtimeException = new RuntimeException("Expected");

		given(this.resultSet.next()).willReturn(true);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		try {
			assertThatRuntimeException()
				.isThrownBy(() ->
					this.template.query(sql, (RowCallbackHandler) rs -> {
						throw runtimeException;
					}))
				.withMessage(runtimeException.getMessage());
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
	void testSqlUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		given(this.statement.executeUpdate(sql)).willReturn(rowsAffected);
		given(this.connection.createStatement()).willReturn(this.statement);

		int actualRowsAffected = this.template.update(sql);
		assertThat(actualRowsAffected).as("Actual rows affected is correct").isEqualTo(rowsAffected);
		verify(this.statement).close();
		verify(this.connection).close();
	}

	/**
	 * Test update with dynamic SQL.
	 */
	@Test
	void testSqlUpdateWithArguments() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ? and PR = ?";
		int rowsAffected = 33;
		given(this.preparedStatement.executeUpdate()).willReturn(rowsAffected);

		int actualRowsAffected = this.template.update(sql,
				4, new SqlParameterValue(Types.NUMERIC, 2, 1.4142f));
		assertThat(actualRowsAffected).as("Actual rows affected is correct").isEqualTo(rowsAffected);
		verify(this.preparedStatement).setObject(1, 4);
		verify(this.preparedStatement).setObject(2, 1.4142f, Types.NUMERIC, 2);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testSqlUpdateEncountersSqlException() throws Exception {
		SQLException sqlException = new SQLException("bad update");
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";

		given(this.statement.executeUpdate(sql)).willThrow(sqlException);
		given(this.connection.createStatement()).willReturn(this.statement);

		assertThatExceptionOfType(DataAccessException.class)
				.isThrownBy(() -> this.template.update(sql))
				.withCause(sqlException);
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testSqlUpdateWithThreadConnection() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		given(this.statement.executeUpdate(sql)).willReturn(rowsAffected);
		given(this.connection.createStatement()).willReturn(this.statement);

		int actualRowsAffected = this.template.update(sql);
		assertThat(actualRowsAffected).as("Actual rows affected is correct").isEqualTo(rowsAffected);

		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testBatchUpdate() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		given(this.statement.executeBatch()).willReturn(new int[] {1, 1});
		mockDatabaseMetaData(true);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);

		verify(this.statement).addBatch(sql[0]);
		verify(this.statement).addBatch(sql[1]);
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithBatchFailure() throws Exception {
		final String[] sql = {"A", "B", "C", "D"};
		given(this.statement.executeBatch()).willThrow(
				new BatchUpdateException(new int[] {1, Statement.EXECUTE_FAILED, 1, Statement.EXECUTE_FAILED}));
		mockDatabaseMetaData(true);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		try {
			template.batchUpdate(sql);
		}
		catch (UncategorizedSQLException ex) {
			assertThat(ex.getSql()).isEqualTo("B; D");
		}
	}

	@Test
	void testBatchUpdateWithNoBatchSupport() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 2"};

		given(this.statement.execute(sql[0])).willReturn(false);
		given(this.statement.getUpdateCount()).willReturn(1, 1);
		given(this.statement.execute(sql[1])).willReturn(false);

		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql);
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);

		verify(this.statement, never()).addBatch(anyString());
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithNoBatchSupportAndSelect() throws Exception {
		final String[] sql = {"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 1",
				"SELECT * FROM NOSUCHTABLE"};

		given(this.statement.execute(sql[0])).willReturn(false);
		given(this.statement.getUpdateCount()).willReturn(1);
		given(this.statement.execute(sql[1])).willReturn(true);
		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.statement);

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				template.batchUpdate(sql));
		verify(this.statement, never()).addBatch(anyString());
		verify(this.statement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithPreparedStatement() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithPreparedStatementWithEmptyData() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {};
		final int[] rowsAffected = new int[] {};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
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

		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertThat(actualRowsAffected.length == 0).as("executed 0 updates").isTrue();

		verify(this.preparedStatement, never()).executeBatch();
	}

	@Test
	void testInterruptibleBatchUpdate() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
		final int[] rowsAffected = new int[] {1, 2};

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
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testInterruptibleBatchUpdateWithBaseClass() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
		final int[] rowsAffected = new int[] {1, 2};

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
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testInterruptibleBatchUpdateWithBaseClassAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
		final int[] rowsAffected = new int[] {1, 2};

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
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, never()).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithPreparedStatementAndNoBatchSupport() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
		final int[] rowsAffected = new int[] {1, 2};

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
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, never()).addBatch();
		verify(this.preparedStatement).setInt(1, ids[0]);
		verify(this.preparedStatement).setInt(1, ids[1]);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testBatchUpdateFails() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] {100, 200};
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

		try {
			assertThatExceptionOfType(DataAccessException.class)
					.isThrownBy(() -> this.template.batchUpdate(sql, setter))
					.withCause(sqlException);
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
	void testBatchUpdateWithEmptyList() {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, Collections.emptyList());
		assertThat(actualRowsAffected).as("executed 0 updates").isEmpty();
	}

	@Test
	void testBatchUpdateWithListOfObjectArrays() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<>(2);
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);
		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = template.batchUpdate(sql, ids);
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setObject(1, 100);
		verify(this.preparedStatement).setObject(1, 200);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithListOfObjectArraysPlusTypeInfo() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Object[]> ids = new ArrayList<>(2);
		ids.add(new Object[] {100});
		ids.add(new Object[] {200});
		final int[] sqlTypes = new int[] {Types.NUMERIC};
		final int[] rowsAffected = new int[] {1, 2};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		mockDatabaseMetaData(true);
		this.template = new JdbcTemplate(this.dataSource, false);

		int[] actualRowsAffected = this.template.batchUpdate(sql, ids, sqlTypes);
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
		verify(this.preparedStatement, times(2)).addBatch();
		verify(this.preparedStatement).setObject(1, 100, sqlTypes[0]);
		verify(this.preparedStatement).setObject(1, 200, sqlTypes[0]);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testBatchUpdateWithCollectionOfObjects() throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final List<Integer> ids = Arrays.asList(100, 200, 300);
		final int[] rowsAffected1 = new int[] {1, 2};
		final int[] rowsAffected2 = new int[] {3};

		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected1, rowsAffected2);
		mockDatabaseMetaData(true);

		ParameterizedPreparedStatementSetter<Integer> setter = (ps, argument) -> ps.setInt(1, argument);
		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);

		int[][] actualRowsAffected = template.batchUpdate(sql, ids, 2, setter);
		assertThat(actualRowsAffected[0]).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0][0]).isEqualTo(rowsAffected1[0]);
		assertThat(actualRowsAffected[0][1]).isEqualTo(rowsAffected1[1]);
		assertThat(actualRowsAffected[1][0]).isEqualTo(rowsAffected2[0]);

		verify(this.preparedStatement, times(3)).addBatch();
		verify(this.preparedStatement).setInt(1, ids.get(0));
		verify(this.preparedStatement).setInt(1, ids.get(1));
		verify(this.preparedStatement).setInt(1, ids.get(2));
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testCouldNotGetConnectionForOperationOrExceptionTranslator() throws SQLException {
		SQLException sqlException = new SQLException("foo", "07xxx");
		given(this.dataSource.getConnection()).willThrow(sqlException);
		JdbcTemplate template = new JdbcTemplate(this.dataSource, false);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();

		assertThatExceptionOfType(CannotGetJdbcConnectionException.class)
				.isThrownBy(() -> template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch))
				.withCause(sqlException);
	}

	@Test
	void testCouldNotGetConnectionForOperationWithLazyExceptionTranslator() throws SQLException {
		SQLException sqlException = new SQLException("foo", "07xxx");
		given(this.dataSource.getConnection()).willThrow(sqlException);
		this.template = new JdbcTemplate();
		this.template.setDataSource(this.dataSource);
		this.template.afterPropertiesSet();
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();

		assertThatExceptionOfType(CannotGetJdbcConnectionException.class)
				.isThrownBy(() -> this.template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch))
				.withCause(sqlException);
	}

	@Test
	void testCouldNotGetConnectionInOperationWithExceptionTranslatorInitializedViaBeanProperty()
			throws SQLException {

		doTestCouldNotGetConnectionInOperationWithExceptionTranslatorInitialized(true);
	}

	@Test
	void testCouldNotGetConnectionInOperationWithExceptionTranslatorInitializedInAfterPropertiesSet()
			throws SQLException {

		doTestCouldNotGetConnectionInOperationWithExceptionTranslatorInitialized(false);
	}

	/**
	 * If beanProperty is true, initialize via exception translator bean property;
	 * if false, use afterPropertiesSet().
	 */
	private void doTestCouldNotGetConnectionInOperationWithExceptionTranslatorInitialized(boolean beanProperty)
			throws SQLException {

		SQLException sqlException = new SQLException("foo", "07xxx");
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
		assertThatExceptionOfType(CannotGetJdbcConnectionException.class)
				.isThrownBy(() -> this.template.query("SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3", rcch))
				.withCause(sqlException);
	}

	@Test
	void testPreparedStatementSetterSucceeds() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		int expectedRowsUpdated = 1;

		given(this.preparedStatement.executeUpdate()).willReturn(expectedRowsUpdated);

		PreparedStatementSetter pss = ps -> ps.setString(1, name);
		int actualRowsUpdated = new JdbcTemplate(this.dataSource).update(sql, pss);
		assertThat(expectedRowsUpdated).as("updated correct # of rows").isEqualTo(actualRowsUpdated);
		verify(this.preparedStatement).setString(1, name);
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testPreparedStatementSetterFails() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		SQLException sqlException = new SQLException();
		given(this.preparedStatement.executeUpdate()).willThrow(sqlException);

		PreparedStatementSetter pss = ps -> ps.setString(1, name);
		assertThatExceptionOfType(DataAccessException.class)
				.isThrownBy(() -> new JdbcTemplate(this.dataSource).update(sql, pss))
				.withCause(sqlException);
		verify(this.preparedStatement).setString(1, name);
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testCouldNotClose() throws Exception {
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
	void testFatalWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		given(this.resultSet.next()).willReturn(false);
		given(this.preparedStatement.getWarnings()).willReturn(warnings);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		JdbcTemplate t = new JdbcTemplate(this.dataSource);
		t.setIgnoreWarnings(false);

		ResultSetExtractor<Byte> extractor = rs -> rs.getByte(1);
		assertThatExceptionOfType(SQLWarningException.class)
				.isThrownBy(() -> t.query(sql, extractor))
				.withCause(warnings);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testIgnoredWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		given(this.resultSet.next()).willReturn(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);
		given(this.preparedStatement.getWarnings()).willReturn(warnings);

		// Too long: truncation

		this.template.setIgnoreWarnings(true);
		RowCallbackHandler rch = rs -> rs.getByte(1);
		this.template.query(sql, rch);

		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testSQLErrorCodeTranslation() throws Exception {
		final SQLException sqlException = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		given(this.resultSet.next()).willReturn(true);
		mockDatabaseMetaData(false);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		this.template.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(this.dataSource));

		assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
				this.template.query(sql, (RowCallbackHandler) rs -> {
					throw sqlException;
				}))
				.withCause(sqlException);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testSQLErrorCodeTranslationWithSpecifiedDatabaseName() throws Exception {
		final SQLException sqlException = new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";

		given(this.resultSet.next()).willReturn(true);
		given(this.connection.createStatement()).willReturn(this.preparedStatement);

		this.template.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator("MySQL"));

		assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
				this.template.query(sql, (RowCallbackHandler) rs -> {
					throw sqlException;
				}))
				.withCause(sqlException);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	/**
	 * Test that we see an SQLException translated using Error Code.
	 * If we provide the SQLExceptionTranslator, we shouldn't use a connection
	 * to get the metadata
	 */
	@Test
	void testUseCustomExceptionTranslator() throws Exception {
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

		assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
				template.query(sql, (RowCallbackHandler) rs -> {
					throw sqlException;
				}))
				.withCause(sqlException);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testStaticResultSetClosed() throws Exception {
		ResultSet resultSet2 = mock();
		reset(this.preparedStatement);
		given(this.preparedStatement.executeQuery()).willReturn(resultSet2);
		given(this.connection.createStatement()).willReturn(this.statement);

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				this.template.query("my query", (ResultSetExtractor<Object>) rs -> {
					throw new InvalidDataAccessApiUsageException("");
				}));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				this.template.query(con -> con.prepareStatement("my query"), (ResultSetExtractor<Object>) rs2 -> {
					throw new InvalidDataAccessApiUsageException("");
				}));

		verify(this.resultSet).close();
		verify(resultSet2).close();
		verify(this.preparedStatement).close();
		verify(this.connection, atLeastOnce()).close();
	}

	@Test
	void testExecuteClosed() throws Exception {
		given(this.resultSet.next()).willReturn(true);
		given(this.callableStatement.execute()).willReturn(true);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);

		SqlParameter param = new SqlReturnResultSet("", (RowCallbackHandler) rs -> {
			throw new InvalidDataAccessApiUsageException("");
		});

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				this.template.call(conn -> conn.prepareCall("my query"), Collections.singletonList(param)));
		verify(this.resultSet).close();
		verify(this.callableStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testCaseInsensitiveResultsMap() throws Exception {
		given(this.callableStatement.execute()).willReturn(false);
		given(this.callableStatement.getUpdateCount()).willReturn(-1);
		given(this.callableStatement.getObject(1)).willReturn("X");

		boolean condition = !this.template.isResultsMapCaseInsensitive();
		assertThat(condition).as("default should have been NOT case insensitive").isTrue();

		this.template.setResultsMapCaseInsensitive(true);
		assertThat(this.template.isResultsMapCaseInsensitive()).as("now it should have been set to case insensitive").isTrue();

		Map<String, Object> out = this.template.call(
				conn -> conn.prepareCall("my query"), Collections.singletonList(new SqlOutParameter("a", 12)));

		assertThat(out).isInstanceOf(LinkedCaseInsensitiveMap.class);
		assertThat(out.get("A")).as("we should have gotten the result with upper case").isNotNull();
		assertThat(out.get("a")).as("we should have gotten the result with lower case").isNotNull();
		verify(this.callableStatement).close();
		verify(this.connection).close();
	}

	@Test  // SPR-16578
	public void testEquallyNamedColumn() throws SQLException {
		given(this.connection.createStatement()).willReturn(this.statement);

		ResultSetMetaData metaData = mock();
		given(metaData.getColumnCount()).willReturn(2);
		given(metaData.getColumnLabel(1)).willReturn("x");
		given(metaData.getColumnLabel(2)).willReturn("X");
		given(this.resultSet.getMetaData()).willReturn(metaData);

		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn("first value");
		given(this.resultSet.getObject(2)).willReturn("second value");

		Map<String, Object> map = this.template.queryForMap("my query");
		assertThat(map).hasSize(1);
		assertThat(map.get("x")).isEqualTo("first value");
	}

	@Test
	void testBatchUpdateReturnsGeneratedKeys_whenDatabaseSupportsBatchUpdates() throws SQLException {
		final int[] rowsAffected = new int[] {1, 2};
		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.supportsBatchUpdates()).willReturn(true);
		given(this.connection.getMetaData()).willReturn(databaseMetaData);
		ResultSet generatedKeysResultSet = mock(ResultSet.class);
		ResultSetMetaData rsmd = mock(ResultSetMetaData.class);
		given(rsmd.getColumnCount()).willReturn(1);
		given(rsmd.getColumnLabel(1)).willReturn("someId");
		given(generatedKeysResultSet.getMetaData()).willReturn(rsmd);
		given(generatedKeysResultSet.getObject(1)).willReturn(123, 456);
		given(generatedKeysResultSet.next()).willReturn(true, true, false);
		given(this.preparedStatement.getGeneratedKeys()).willReturn(generatedKeysResultSet);

		int[] values = new int[]{100, 200};
		BatchPreparedStatementSetter bpss = new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setObject(i, values[i]);
			}

			@Override
			public int getBatchSize() {
				return 2;
			}
		};

		KeyHolder keyHolder = new GeneratedKeyHolder();
		this.template.batchUpdate(con -> con.prepareStatement(""), bpss, keyHolder);

		assertThat(keyHolder.getKeyList()).containsExactly(
				Collections.singletonMap("someId", 123),
				Collections.singletonMap("someId", 456));
	}

	@Test
	void testBatchUpdateReturnsGeneratedKeys_whenDatabaseDoesNotSupportBatchUpdates() throws SQLException {
		final int[] rowsAffected = new int[] {1, 2};
		given(this.preparedStatement.executeBatch()).willReturn(rowsAffected);
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.supportsBatchUpdates()).willReturn(false);
		given(this.connection.getMetaData()).willReturn(databaseMetaData);
		ResultSetMetaData rsmd = mock(ResultSetMetaData.class);
		given(rsmd.getColumnCount()).willReturn(1);
		given(rsmd.getColumnLabel(1)).willReturn("someId");
		ResultSet generatedKeysResultSet1 = mock(ResultSet.class);
		given(generatedKeysResultSet1.getMetaData()).willReturn(rsmd);
		given(generatedKeysResultSet1.getObject(1)).willReturn(123);
		given(generatedKeysResultSet1.next()).willReturn(true, false);
		ResultSet generatedKeysResultSet2 = mock(ResultSet.class);
		given(generatedKeysResultSet2.getMetaData()).willReturn(rsmd);
		given(generatedKeysResultSet2.getObject(1)).willReturn(456);
		given(generatedKeysResultSet2.next()).willReturn(true, false);
		given(this.preparedStatement.getGeneratedKeys()).willReturn(generatedKeysResultSet1, generatedKeysResultSet2);

		int[] values = new int[]{100, 200};
		BatchPreparedStatementSetter bpss = new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setObject(i, values[i]);
			}

			@Override
			public int getBatchSize() {
				return 2;
			}
		};

		KeyHolder keyHolder = new GeneratedKeyHolder();
		this.template.batchUpdate(con -> con.prepareStatement(""), bpss, keyHolder);

		assertThat(keyHolder.getKeyList()).containsExactly(
				Collections.singletonMap("someId", 123),
				Collections.singletonMap("someId", 456));
	}

	private void mockDatabaseMetaData(boolean supportsBatchUpdates) throws SQLException {
		DatabaseMetaData databaseMetaData = mock();
		given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
		given(databaseMetaData.supportsBatchUpdates()).willReturn(supportsBatchUpdates);
		given(this.connection.getMetaData()).willReturn(databaseMetaData);
	}


	private interface JdbcTemplateCallback {

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
