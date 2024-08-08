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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * <b>This is the central delegate in the JDBC core package.</b>
 * It can be used directly for many data access purposes, supporting any kind
 * of JDBC operation. For a more focused and convenient facade on top of this,
 * consider {@link org.springframework.jdbc.core.simple.JdbcClient} as of 6.1.
 *
 * <p>This class simplifies the use of JDBC and helps to avoid common errors.
 * It executes core JDBC workflow, leaving application code to provide SQL
 * and extract results. This class executes SQL queries or updates, initiating
 * iteration over ResultSets and catching JDBC exceptions and translating
 * them to the common {@code org.springframework.dao} exception hierarchy.
 *
 * <p>Code using this class need only implement callback interfaces, giving
 * them a clearly defined contract. The {@link PreparedStatementCreator} callback
 * interface creates a prepared statement given a Connection, providing SQL and
 * any necessary parameters. The {@link ResultSetExtractor} interface extracts
 * values from a ResultSet. See also {@link PreparedStatementSetter} and
 * {@link RowMapper} for two popular alternative callback interfaces.
 *
 * <p>An instance of this template class is thread-safe once configured.
 * Can be used within a service implementation via direct instantiation
 * with a DataSource reference, or get prepared in an application context
 * and given to services as bean reference. Note: The DataSource should
 * always be configured as a bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p>Because this class is parameterizable by the callback interfaces and
 * the {@link org.springframework.jdbc.support.SQLExceptionTranslator}
 * interface, there should be no need to subclass it.
 *
 * <p>All SQL operations performed by this class are logged at debug level,
 * using "org.springframework.jdbc.core.JdbcTemplate" as log category.
 *
 * <p><b>NOTE: As of 6.1, there is a unified JDBC access facade available in
 * the form of {@link org.springframework.jdbc.core.simple.JdbcClient}.</b>
 * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
 * with flexible use of indexed or named parameters. It delegates to a
 * {@code JdbcTemplate}/{@code NamedParameterJdbcTemplate} for actual execution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since May 3, 2001
 * @see JdbcOperations
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see CallableStatementCreator
 * @see PreparedStatementCallback
 * @see CallableStatementCallback
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.springframework.jdbc.support.SQLExceptionTranslator
 * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {

	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";

	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";


	/** If this variable is {@code false}, we will throw exceptions on SQL warnings. */
	private boolean ignoreWarnings = true;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * fetchSize property on statements used for query processing.
	 */
	private int fetchSize = -1;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * maxRows property on statements used for query processing.
	 */
	private int maxRows = -1;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * queryTimeout property on statements used for query processing.
	 */
	private int queryTimeout = -1;

	/**
	 * If this variable is set to true, then all results checking will be bypassed for any
	 * callable statement processing. This can be used to avoid a bug in some older Oracle
	 * JDBC drivers like 10.1.0.2.
	 */
	private boolean skipResultsProcessing = false;

	/**
	 * If this variable is set to true then all results from a stored procedure call
	 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
	 * All other results processing will be take place unless the variable
	 * {@code skipResultsProcessing} is set to {@code true}.
	 */
	private boolean skipUndeclaredResults = false;

	/**
	 * If this variable is set to true then execution of a CallableStatement will return
	 * the results in a Map that uses case-insensitive names for the parameters.
	 */
	private boolean resultsMapCaseInsensitive = false;


	/**
	 * Construct a new JdbcTemplate for bean usage.
	 * <p>Note: The DataSource has to be set before using the instance.
	 * @see #setDataSource
	 */
	public JdbcTemplate() {
	}

	/**
	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
	 * <p>Note: This will not trigger initialization of the exception translator.
	 * @param dataSource the JDBC DataSource to obtain connections from
	 */
	public JdbcTemplate(DataSource dataSource) {
		setDataSource(dataSource);
		afterPropertiesSet();
	}

	/**
	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
	 * <p>Note: Depending on the "lazyInit" flag, initialization of the exception translator
	 * will be triggered.
	 * @param dataSource the JDBC DataSource to obtain connections from
	 * @param lazyInit whether to lazily initialize the SQLExceptionTranslator
	 */
	public JdbcTemplate(DataSource dataSource, boolean lazyInit) {
		setDataSource(dataSource);
		setLazyInit(lazyInit);
		afterPropertiesSet();
	}


	/**
	 * Set whether we want to ignore JDBC statement warnings ({@link SQLWarning}).
	 * <p>Default is {@code true}, swallowing and logging all warnings. Switch this flag to
	 * {@code false} to make this JdbcTemplate throw a {@link SQLWarningException} instead
	 * (or chain the {@link SQLWarning} into the primary {@link SQLException}, if any).
	 * @see Statement#getWarnings()
	 * @see java.sql.SQLWarning
	 * @see org.springframework.jdbc.SQLWarningException
	 * @see #handleWarnings(Statement)
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * Return whether we ignore SQLWarnings.
	 */
	public boolean isIgnoreWarnings() {
		return this.ignoreWarnings;
	}

	/**
	 * Set the fetch size for this JdbcTemplate. This is important for processing large
	 * result sets: Setting this higher than the default value will increase processing
	 * speed at the cost of memory consumption; setting this lower can avoid transferring
	 * row data that will never be read by the application.
	 * <p>Default is -1, indicating to use the JDBC driver's default configuration
	 * (i.e. to not pass a specific fetch size setting on to the driver).
	 * <p>Note: As of 4.3, negative values other than -1 will get passed on to the
	 * driver, since e.g. MySQL supports special behavior for {@code Integer.MIN_VALUE}.
	 * @see java.sql.Statement#setFetchSize
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Return the fetch size specified for this JdbcTemplate.
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * Set the maximum number of rows for this JdbcTemplate. This is important for
	 * processing subsets of large result sets, avoiding to read and hold the entire
	 * result set in the database or in the JDBC driver if we're never interested in
	 * the entire result in the first place (for example, when performing searches
	 * that might return a large number of matches).
	 * <p>Default is -1, indicating to use the JDBC driver's default configuration
	 * (i.e. to not pass a specific max rows setting on to the driver).
	 * <p>Note: As of 4.3, negative values other than -1 will get passed on to the
	 * driver, in sync with {@link #setFetchSize}'s support for special MySQL values.
	 * @see java.sql.Statement#setMaxRows
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * Return the maximum number of rows specified for this JdbcTemplate.
	 */
	public int getMaxRows() {
		return this.maxRows;
	}

	/**
	 * Set the query timeout for statements that this JdbcTemplate executes.
	 * <p>Default is -1, indicating to use the JDBC driver's default
	 * (i.e. to not pass a specific query timeout setting on the driver).
	 * <p>Note: Any timeout specified here will be overridden by the remaining
	 * transaction timeout when executing within a transaction that has a
	 * timeout specified at the transaction level.
	 * @see java.sql.Statement#setQueryTimeout
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * Return the query timeout for statements that this JdbcTemplate executes.
	 */
	public int getQueryTimeout() {
		return this.queryTimeout;
	}

	/**
	 * Set whether results processing should be skipped. Can be used to optimize callable
	 * statement processing when we know that no results are being passed back - the processing
	 * of out parameter will still take place. This can be used to avoid a bug in some older
	 * Oracle JDBC drivers like 10.1.0.2.
	 */
	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
		this.skipResultsProcessing = skipResultsProcessing;
	}

	/**
	 * Return whether results processing should be skipped.
	 */
	public boolean isSkipResultsProcessing() {
		return this.skipResultsProcessing;
	}

	/**
	 * Set whether undeclared results should be skipped.
	 */
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	/**
	 * Return whether undeclared results should be skipped.
	 */
	public boolean isSkipUndeclaredResults() {
		return this.skipUndeclaredResults;
	}

	/**
	 * Set whether execution of a CallableStatement will return the results in a Map
	 * that uses case-insensitive names for the parameters.
	 */
	public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
		this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
	}

	/**
	 * Return whether execution of a CallableStatement will return the results in a Map
	 * that uses case-insensitive names for the parameters.
	 */
	public boolean isResultsMapCaseInsensitive() {
		return this.resultsMapCaseInsensitive;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		try {
			// Create close-suppressing Connection proxy, also preparing returned Statements.
			Connection conToUse = createConnectionProxy(con);
			return action.doInConnection(conToUse);
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			String sql = getSql(action);
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("ConnectionCallback", sql, ex);
		}
		finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * Create a close-suppressing proxy for the given JDBC Connection.
	 * Called by the {@code execute} method.
	 * <p>The proxy also prepares returned JDBC Statements, applying
	 * statement settings such as fetch size, max rows, and query timeout.
	 * @param con the JDBC Connection to create a proxy for
	 * @return the Connection proxy
	 * @see java.sql.Connection#close()
	 * @see #execute(ConnectionCallback)
	 * @see #applyStatementSettings
	 */
	protected Connection createConnectionProxy(Connection con) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(con));
	}


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	@Nullable
	private <T> T execute(StatementCallback<T> action, boolean closeResources) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			applyStatementSettings(stmt);
			T result = action.doInStatement(stmt);
			handleWarnings(stmt);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (stmt != null) {
				handleWarnings(stmt, ex);
			}
			String sql = getSql(action);
			JdbcUtils.closeStatement(stmt);
			stmt = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("StatementCallback", sql, ex);
		}
		finally {
			if (closeResources) {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
	}

	@Override
	@Nullable
	public <T> T execute(StatementCallback<T> action) throws DataAccessException {
		return execute(action, true);
	}

	@Override
	public void execute(final String sql) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL statement [" + sql + "]");
		}

		// Callback to execute the statement.
		class ExecuteStatementCallback implements StatementCallback<Object>, SqlProvider {
			@Override
			@Nullable
			public Object doInStatement(Statement stmt) throws SQLException {
				stmt.execute(sql);
				return null;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		execute(new ExecuteStatementCallback(), true);
	}

	@Override
	@Nullable
	public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		Assert.notNull(rse, "ResultSetExtractor must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL query [" + sql + "]");
		}

		// Callback to execute the query.
		class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
			@Override
			@Nullable
			public T doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery(sql);
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return execute(new QueryStatementCallback(), true);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		class StreamStatementCallback implements StatementCallback<Stream<T>>, SqlProvider {
			@Override
			public Stream<T> doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = stmt.executeQuery(sql);
				Connection con = stmt.getConnection();
				return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
					JdbcUtils.closeResultSet(rs);
					JdbcUtils.closeStatement(stmt);
					DataSourceUtils.releaseConnection(con, getDataSource());
				});
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return result(execute(new StreamStatementCallback(), false));
	}

	@Override
	public Map<String, Object> queryForMap(String sql) throws DataAccessException {
		return result(queryForObject(sql, getColumnMapRowMapper()));
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, rowMapper);
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
		return query(sql, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
		return query(sql, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
		return result(query(sql, new SqlRowSetResultSetExtractor()));
	}

	@Override
	public int update(final String sql) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL update [" + sql + "]");
		}

		// Callback to execute the update statement.
		class UpdateStatementCallback implements StatementCallback<Integer>, SqlProvider {
			@Override
			public Integer doInStatement(Statement stmt) throws SQLException {
				int rows = stmt.executeUpdate(sql);
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return updateCount(execute(new UpdateStatementCallback(), true));
	}

	@Override
	public int[] batchUpdate(final String... sql) throws DataAccessException {
		Assert.notEmpty(sql, "SQL array must not be empty");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update of " + sql.length + " statements");
		}

		// Callback to execute the batch update.
		class BatchUpdateStatementCallback implements StatementCallback<int[]>, SqlProvider {

			@Nullable
			private String currSql;

			@Override
			public int[] doInStatement(Statement stmt) throws SQLException, DataAccessException {
				int[] rowsAffected = new int[sql.length];
				if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
					for (String sqlStmt : sql) {
						this.currSql = appendSql(this.currSql, sqlStmt);
						stmt.addBatch(sqlStmt);
					}
					try {
						rowsAffected = stmt.executeBatch();
					}
					catch (BatchUpdateException ex) {
						String batchExceptionSql = null;
						for (int i = 0; i < ex.getUpdateCounts().length; i++) {
							if (ex.getUpdateCounts()[i] == Statement.EXECUTE_FAILED) {
								batchExceptionSql = appendSql(batchExceptionSql, sql[i]);
							}
						}
						if (StringUtils.hasLength(batchExceptionSql)) {
							this.currSql = batchExceptionSql;
						}
						throw ex;
					}
				}
				else {
					for (int i = 0; i < sql.length; i++) {
						this.currSql = sql[i];
						if (!stmt.execute(sql[i])) {
							rowsAffected[i] = stmt.getUpdateCount();
						}
						else {
							throw new InvalidDataAccessApiUsageException("Invalid batch SQL statement: " + sql[i]);
						}
					}
				}
				return rowsAffected;
			}

			private String appendSql(@Nullable String sql, String statement) {
				return (StringUtils.hasLength(sql) ? sql + "; " + statement : statement);
			}

			@Override
			@Nullable
			public String getSql() {
				return this.currSql;
			}
		}

		int[] result = execute(new BatchUpdateStatementCallback(), true);
		Assert.state(result != null, "No update counts");
		return result;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	@Nullable
	private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources)
			throws DataAccessException {

		Assert.notNull(psc, "PreparedStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(psc);
			logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		PreparedStatement ps = null;
		try {
			ps = psc.createPreparedStatement(con);
			applyStatementSettings(ps);
			T result = action.doInPreparedStatement(ps);
			handleWarnings(ps);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (psc instanceof ParameterDisposer parameterDisposer) {
				parameterDisposer.cleanupParameters();
			}
			if (ps != null) {
				handleWarnings(ps, ex);
			}
			String sql = getSql(psc);
			psc = null;
			JdbcUtils.closeStatement(ps);
			ps = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("PreparedStatementCallback", sql, ex);
		}
		finally {
			if (closeResources) {
				if (psc instanceof ParameterDisposer parameterDisposer) {
					parameterDisposer.cleanupParameters();
				}
				JdbcUtils.closeStatement(ps);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
	}

	@Override
	@Nullable
	public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return execute(psc, action, true);
	}

	@Override
	@Nullable
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(new SimplePreparedStatementCreator(sql), action, true);
	}

	/**
	 * Query using a prepared statement, allowing for a PreparedStatementCreator
	 * and a PreparedStatementSetter. Most other query methods use this method,
	 * but application code will always work with either a creator or a setter.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	public <T> T query(
			PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss, final ResultSetExtractor<T> rse)
			throws DataAccessException {

		Assert.notNull(rse, "ResultSetExtractor must not be null");
		logger.debug("Executing prepared SQL query");

		return execute(psc, new PreparedStatementCallback<>() {
			@Override
			@Nullable
			public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
				ResultSet rs = null;
				try {
					if (pss != null) {
						pss.setValues(ps);
					}
					rs = ps.executeQuery();
					return rse.extractData(rs);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
					if (pss instanceof ParameterDisposer parameterDisposer) {
						parameterDisposer.cleanupParameters();
					}
				}
			}
		}, true);
	}

	@Override
	@Nullable
	public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(psc, null, rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(new SimplePreparedStatementCreator(sql), pss, rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
	}

	@Deprecated
	@Override
	@Nullable
	public <T> T query(String sql, @Nullable Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	@Nullable
	public <T> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object... args) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
		query(psc, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
		query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgTypePreparedStatementSetter(args, argTypes), rch);
	}

	@Deprecated
	@Override
	public void query(String sql, @Nullable Object[] args, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch, @Nullable Object... args) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(psc, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, pss, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Deprecated
	@Override
	public <T> List<T> query(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
	}

	/**
	 * Query using a prepared statement, allowing for a PreparedStatementCreator
	 * and a PreparedStatementSetter. Most other query methods use this method,
	 * but application code will always work with either a creator or a setter.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (e.g. through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	public <T> Stream<T> queryForStream(PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss,
			RowMapper<T> rowMapper) throws DataAccessException {

		return result(execute(psc, ps -> {
			if (pss != null) {
				pss.setValues(ps);
			}
			ResultSet rs = ps.executeQuery();
			Connection con = ps.getConnection();
			return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
				JdbcUtils.closeResultSet(rs);
				if (pss instanceof ParameterDisposer parameterDisposer) {
					parameterDisposer.cleanupParameters();
				}
				JdbcUtils.closeStatement(ps);
				DataSourceUtils.releaseConnection(con, getDataSource());
			});
		}, false));
	}

	@Override
	public <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return queryForStream(psc, null, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
		return queryForStream(new SimplePreparedStatementCreator(sql), pss, rowMapper);
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		return queryForStream(new SimplePreparedStatementCreator(sql), newArgPreparedStatementSetter(args), rowMapper);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Deprecated
	@Override
	@Nullable
	public <T> T queryForObject(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
		return DataAccessUtils.nullableSingleResult(results);
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType));
	}

	@Deprecated
	@Override
	@Nullable
	public <T> T queryForObject(String sql, @Nullable Object[] args, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	@Nullable
	public <T> T queryForObject(String sql, Class<T> requiredType, @Nullable Object... args) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return result(queryForObject(sql, args, argTypes, getColumnMapRowMapper()));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, @Nullable Object... args) throws DataAccessException {
		return result(queryForObject(sql, args, getColumnMapRowMapper()));
	}

	@Override
	public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
		return query(sql, args, argTypes, getSingleColumnRowMapper(elementType));
	}

	@Deprecated
	@Override
	public <T> List<T> queryForList(String sql, @Nullable Object[] args, Class<T> elementType) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType, @Nullable Object... args) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return query(sql, args, argTypes, getColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, @Nullable Object... args) throws DataAccessException {
		return query(sql, args, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return result(query(sql, args, argTypes, new SqlRowSetResultSetExtractor()));
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, @Nullable Object... args) throws DataAccessException {
		return result(query(sql, args, new SqlRowSetResultSetExtractor()));
	}

	protected int update(final PreparedStatementCreator psc, @Nullable final PreparedStatementSetter pss)
			throws DataAccessException {

		logger.debug("Executing prepared SQL update");

		return updateCount(execute(psc, ps -> {
			try {
				if (pss != null) {
					pss.setValues(ps);
				}
				int rows = ps.executeUpdate();
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			finally {
				if (pss instanceof ParameterDisposer parameterDisposer) {
					parameterDisposer.cleanupParameters();
				}
			}
		}, true));
	}

	@Override
	public int update(PreparedStatementCreator psc) throws DataAccessException {
		return update(psc, (PreparedStatementSetter) null);
	}

	@Override
	public int update(final PreparedStatementCreator psc, final KeyHolder generatedKeyHolder)
			throws DataAccessException {

		Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
		logger.debug("Executing SQL update and returning generated keys");

		return updateCount(execute(psc, ps -> {
			int rows = ps.executeUpdate();
			generatedKeyHolder.getKeyList().clear();
			storeGeneratedKeys(generatedKeyHolder, ps, 1);
			if (logger.isTraceEnabled()) {
				logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeyHolder.getKeyList().size() + " keys");
			}
			return rows;
		}, true));
	}

	@Override
	public int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException {
		return update(new SimplePreparedStatementCreator(sql), pss);
	}

	@Override
	public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return update(sql, newArgTypePreparedStatementSetter(args, argTypes));
	}

	@Override
	public int update(String sql, @Nullable Object... args) throws DataAccessException {
		return update(sql, newArgPreparedStatementSetter(args));
	}

	@Override
	public int[] batchUpdate(final PreparedStatementCreator psc, final BatchPreparedStatementSetter pss,
			final KeyHolder generatedKeyHolder) throws DataAccessException {

		int[] result = execute(psc, getPreparedStatementCallback(pss, generatedKeyHolder));

		Assert.state(result != null, "No result array");
		return result;
	}

	@Override
	public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "]");
		}
		int batchSize = pss.getBatchSize();
		if (batchSize == 0) {
			return new int[0];
		}

		int[] result = execute(sql, getPreparedStatementCallback(pss, null));
		Assert.state(result != null, "No result array");
		return result;
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
		return batchUpdate(sql, batchArgs, new int[0]);
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs, final int[] argTypes) throws DataAccessException {
		if (batchArgs.isEmpty()) {
			return new int[0];
		}

		return batchUpdate(
				sql,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = batchArgs.get(i);
						int colIndex = 0;
						for (Object value : values) {
							colIndex++;
							if (value instanceof SqlParameterValue paramValue) {
								StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
							}
							else {
								int colType;
								if (argTypes.length < colIndex) {
									colType = SqlTypeValue.TYPE_UNKNOWN;
								}
								else {
									colType = argTypes[colIndex - 1];
								}
								StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
							}
						}
					}
					@Override
					public int getBatchSize() {
						return batchArgs.size();
					}
				});
	}

	@Override
	public <T> int[][] batchUpdate(String sql, final Collection<T> batchArgs, final int batchSize,
			final ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {

		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
		}
		int[][] result = execute(sql, (PreparedStatementCallback<int[][]>) ps -> {
			List<int[]> rowsAffected = new ArrayList<>();
			try {
				boolean batchSupported = JdbcUtils.supportsBatchUpdates(ps.getConnection());
				int n = 0;
				for (T obj : batchArgs) {
					pss.setValues(ps, obj);
					n++;
					if (batchSupported) {
						ps.addBatch();
						if (n % batchSize == 0 || n == batchArgs.size()) {
							if (logger.isTraceEnabled()) {
								int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
								int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
								logger.trace("Sending SQL batch update #" + batchIdx + " with " + items + " items");
							}
							try {
								int[] updateCounts = ps.executeBatch();
								rowsAffected.add(updateCounts);
							}
							catch (BatchUpdateException ex) {
								throw new AggregatedBatchUpdateException(rowsAffected.toArray(int[][]::new), ex);
							}
						}
					}
					else {
						int i = ps.executeUpdate();
						rowsAffected.add(new int[] {i});
					}
				}
				int[][] result1 = new int[rowsAffected.size()][];
				for (int i = 0; i < result1.length; i++) {
					result1[i] = rowsAffected.get(i);
				}
				return result1;
			}
			finally {
				if (pss instanceof ParameterDisposer parameterDisposer) {
					parameterDisposer.cleanupParameters();
				}
			}
		});

		Assert.state(result != null, "No result array");
		return result;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
			throws DataAccessException {

		Assert.notNull(csc, "CallableStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(csc);
			logger.debug("Calling stored procedure" + (sql != null ? " [" + sql + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		CallableStatement cs = null;
		try {
			cs = csc.createCallableStatement(con);
			applyStatementSettings(cs);
			T result = action.doInCallableStatement(cs);
			handleWarnings(cs);
			return result;
		}
		catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (csc instanceof ParameterDisposer parameterDisposer) {
				parameterDisposer.cleanupParameters();
			}
			if (cs != null) {
				handleWarnings(cs, ex);
			}
			String sql = getSql(csc);
			csc = null;
			JdbcUtils.closeStatement(cs);
			cs = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("CallableStatementCallback", sql, ex);
		}
		finally {
			if (csc instanceof ParameterDisposer parameterDisposer) {
				parameterDisposer.cleanupParameters();
			}
			JdbcUtils.closeStatement(cs);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	@Nullable
	public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
		return execute(new SimpleCallableStatementCreator(callString), action);
	}

	@Override
	public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException {

		final List<SqlParameter> updateCountParameters = new ArrayList<>();
		final List<SqlParameter> resultSetParameters = new ArrayList<>();
		final List<SqlParameter> callParameters = new ArrayList<>();

		for (SqlParameter parameter : declaredParameters) {
			if (parameter.isResultsParameter()) {
				if (parameter instanceof SqlReturnResultSet) {
					resultSetParameters.add(parameter);
				}
				else {
					updateCountParameters.add(parameter);
				}
			}
			else {
				callParameters.add(parameter);
			}
		}

		Map<String, Object> result = execute(csc, cs -> {
			boolean retVal = cs.execute();
			int updateCount = cs.getUpdateCount();
			if (logger.isTraceEnabled()) {
				logger.trace("CallableStatement.execute() returned '" + retVal + "'");
				logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
			}
			Map<String, Object> resultsMap = createResultsMap();
			if (retVal || updateCount != -1) {
				resultsMap.putAll(extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount));
			}
			resultsMap.putAll(extractOutputParameters(cs, callParameters));
			return resultsMap;
		});

		Assert.state(result != null, "No result map");
		return result;
	}

	/**
	 * Extract returned ResultSets from the completed stored procedure.
	 * @param cs a JDBC wrapper for the stored procedure
	 * @param updateCountParameters the parameter list of declared update count parameters for the stored procedure
	 * @param resultSetParameters the parameter list of declared resultSet parameters for the stored procedure
	 * @return a Map that contains returned results
	 */
	protected Map<String, Object> extractReturnedResults(CallableStatement cs,
			@Nullable List<SqlParameter> updateCountParameters, @Nullable List<SqlParameter> resultSetParameters,
			int updateCount) throws SQLException {

		Map<String, Object> results = new LinkedHashMap<>(4);
		int rsIndex = 0;
		int updateIndex = 0;
		boolean moreResults;
		if (!this.skipResultsProcessing) {
			do {
				if (updateCount == -1) {
					if (resultSetParameters != null && resultSetParameters.size() > rsIndex) {
						SqlReturnResultSet declaredRsParam = (SqlReturnResultSet) resultSetParameters.get(rsIndex);
						results.putAll(processResultSet(cs.getResultSet(), declaredRsParam));
						rsIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String rsName = RETURN_RESULT_SET_PREFIX + (rsIndex + 1);
							SqlReturnResultSet undeclaredRsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
							results.putAll(processResultSet(cs.getResultSet(), undeclaredRsParam));
							rsIndex++;
						}
					}
				}
				else {
					if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
						SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
						String declaredUcName = ucParam.getName();
						results.put(declaredUcName, updateCount);
						updateIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String undeclaredName = RETURN_UPDATE_COUNT_PREFIX + (updateIndex + 1);
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnUpdateCount parameter named '" + undeclaredName + "'");
							}
							results.put(undeclaredName, updateCount);
							updateIndex++;
						}
					}
				}
				moreResults = cs.getMoreResults();
				updateCount = cs.getUpdateCount();
				if (logger.isTraceEnabled()) {
					logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
				}
			}
			while (moreResults || updateCount != -1);
		}
		return results;
	}

	/**
	 * Extract output parameters from the completed stored procedure.
	 * @param cs the JDBC wrapper for the stored procedure
	 * @param parameters parameter list for the stored procedure
	 * @return a Map that contains returned results
	 */
	protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters)
			throws SQLException {

		Map<String, Object> results = CollectionUtils.newLinkedHashMap(parameters.size());
		int sqlColIndex = 1;
		for (SqlParameter param : parameters) {
			if (param instanceof SqlOutParameter outParam) {
				Assert.state(outParam.getName() != null, "Anonymous parameters not allowed");
				SqlReturnType returnType = outParam.getSqlReturnType();
				if (returnType != null) {
					Object out = returnType.getTypeValue(cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
					results.put(outParam.getName(), out);
				}
				else {
					Object out = cs.getObject(sqlColIndex);
					if (out instanceof ResultSet resultSet) {
						if (outParam.isResultSetSupported()) {
							results.putAll(processResultSet(resultSet, outParam));
						}
						else {
							String rsName = outParam.getName();
							SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							results.putAll(processResultSet(resultSet, rsParam));
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
						}
					}
					else {
						results.put(outParam.getName(), out);
					}
				}
			}
			if (!param.isResultsParameter()) {
				sqlColIndex++;
			}
		}
		return results;
	}

	/**
	 * Process the given ResultSet from a stored procedure.
	 * @param rs the ResultSet to process
	 * @param param the corresponding stored procedure parameter
	 * @return a Map that contains returned results
	 */
	protected Map<String, Object> processResultSet(
			@Nullable ResultSet rs, ResultSetSupportingSqlParameter param) throws SQLException {

		if (rs != null) {
			try {
				if (param.getRowMapper() != null) {
					RowMapper<?> rowMapper = param.getRowMapper();
					Object data = (new RowMapperResultSetExtractor<>(rowMapper)).extractData(rs);
					return Collections.singletonMap(param.getName(), data);
				}
				else if (param.getRowCallbackHandler() != null) {
					RowCallbackHandler rch = param.getRowCallbackHandler();
					(new RowCallbackHandlerResultSetExtractor(rch)).extractData(rs);
					return Collections.singletonMap(param.getName(),
							"ResultSet returned from stored procedure was processed");
				}
				else if (param.getResultSetExtractor() != null) {
					Object data = param.getResultSetExtractor().extractData(rs);
					return Collections.singletonMap(param.getName(), data);
				}
			}
			finally {
				JdbcUtils.closeResultSet(rs);
			}
		}
		return Collections.emptyMap();
	}


	//-------------------------------------------------------------------------
	// Implementation hooks and helper methods
	//-------------------------------------------------------------------------

	/**
	 * Create a new RowMapper for reading columns as key-value pairs.
	 * @return the RowMapper to use
	 * @see ColumnMapRowMapper
	 */
	protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
		return new ColumnMapRowMapper();
	}

	/**
	 * Create a new RowMapper for reading result objects from a single column.
	 * @param requiredType the type that each result object is expected to match
	 * @return the RowMapper to use
	 * @see SingleColumnRowMapper
	 */
	protected <T> RowMapper<T> getSingleColumnRowMapper(Class<T> requiredType) {
		return new SingleColumnRowMapper<>(requiredType);
	}

	/**
	 * Create a Map instance to be used as the results map.
	 * <p>If {@link #resultsMapCaseInsensitive} has been set to true,
	 * a {@link LinkedCaseInsensitiveMap} will be created; otherwise, a
	 * {@link LinkedHashMap} will be created.
	 * @return the results Map instance
	 * @see #setResultsMapCaseInsensitive
	 * @see #isResultsMapCaseInsensitive
	 */
	protected Map<String, Object> createResultsMap() {
		if (isResultsMapCaseInsensitive()) {
			return new LinkedCaseInsensitiveMap<>();
		}
		else {
			return new LinkedHashMap<>();
		}
	}

	/**
	 * Prepare the given JDBC Statement (or PreparedStatement or CallableStatement),
	 * applying statement settings such as fetch size, max rows, and query timeout.
	 * @param stmt the JDBC Statement to prepare
	 * @throws SQLException if thrown by JDBC API
	 * @see #setFetchSize
	 * @see #setMaxRows
	 * @see #setQueryTimeout
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
	 */
	protected void applyStatementSettings(Statement stmt) throws SQLException {
		int fetchSize = getFetchSize();
		if (fetchSize != -1) {
			stmt.setFetchSize(fetchSize);
		}
		int maxRows = getMaxRows();
		if (maxRows != -1) {
			stmt.setMaxRows(maxRows);
		}
		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
	}

	/**
	 * Create a new arg-based PreparedStatementSetter using the args passed in.
	 * <p>By default, we'll create an {@link ArgumentPreparedStatementSetter}.
	 * This method allows for the creation to be overridden by subclasses.
	 * @param args object array with arguments
	 * @return the new PreparedStatementSetter to use
	 */
	protected PreparedStatementSetter newArgPreparedStatementSetter(@Nullable Object[] args) {
		return new ArgumentPreparedStatementSetter(args);
	}

	/**
	 * Create a new arg-type-based PreparedStatementSetter using the args and types passed in.
	 * <p>By default, we'll create an {@link ArgumentTypePreparedStatementSetter}.
	 * This method allows for the creation to be overridden by subclasses.
	 * @param args object array with arguments
	 * @param argTypes int array of SQLTypes for the associated arguments
	 * @return the new PreparedStatementSetter to use
	 */
	protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		return new ArgumentTypePreparedStatementSetter(args, argTypes);
	}

	/**
	 * Handle warnings before propagating a primary {@code SQLException}
	 * from executing the given statement.
	 * <p>Calls regular {@link #handleWarnings(Statement)} but catches
	 * {@link SQLWarningException} in order to chain the {@link SQLWarning}
	 * into the primary exception instead.
	 * @param stmt the current JDBC statement
	 * @param ex the primary exception after failed statement execution
	 * @since 5.3.29
	 * @see #handleWarnings(Statement)
	 * @see SQLException#setNextException
	 */
	protected void handleWarnings(Statement stmt, SQLException ex) {
		try {
			handleWarnings(stmt);
		}
		catch (SQLWarningException nonIgnoredWarning) {
			ex.setNextException(nonIgnoredWarning.getSQLWarning());
		}
		catch (SQLException warningsEx) {
			logger.debug("Failed to retrieve warnings", warningsEx);
		}
		catch (Throwable warningsEx) {
			logger.debug("Failed to process warnings", warningsEx);
		}
	}

	/**
	 * Handle the warnings for the given JDBC statement, if any.
	 * <p>Throws a {@link SQLWarningException} if we're not ignoring warnings,
	 * otherwise logs the warnings at debug level.
	 * @param stmt the current JDBC statement
	 * @throws SQLException in case of warnings retrieval failure
	 * @throws SQLWarningException for a concrete warning to raise
	 * (when not ignoring warnings)
	 * @see #setIgnoreWarnings
	 * @see #handleWarnings(SQLWarning)
	 */
	protected void handleWarnings(Statement stmt) throws SQLException, SQLWarningException {
		if (isIgnoreWarnings()) {
			if (logger.isDebugEnabled()) {
				SQLWarning warningToLog = stmt.getWarnings();
				while (warningToLog != null) {
					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		}
		else {
			handleWarnings(stmt.getWarnings());
		}
	}

	/**
	 * Throw a {@link SQLWarningException} if encountering an actual warning.
	 * @param warning the warnings object from the current statement.
	 * May be {@code null}, in which case this method does nothing.
	 * @throws SQLWarningException in case of an actual warning to be raised
	 */
	protected void handleWarnings(@Nullable SQLWarning warning) throws SQLWarningException {
		if (warning != null) {
			throw new SQLWarningException("Warning not ignored", warning);
		}
	}

	/**
	 * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (may be {@code null})
	 * @param ex the offending {@code SQLException}
	 * @return a DataAccessException wrapping the {@code SQLException} (never {@code null})
	 * @since 5.0
	 * @see #getExceptionTranslator()
	 */
	protected DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
		DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
		return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
	}


	/**
	 * Determine SQL from potential provider object.
	 * @param obj object which is potentially an SqlProvider
	 * @return the SQL string, or {@code null} if not known
	 * @see SqlProvider
	 */
	@Nullable
	private static String getSql(Object obj) {
		return (obj instanceof SqlProvider sqlProvider ? sqlProvider.getSql() : null);
	}

	private static <T> T result(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}

	private static int updateCount(@Nullable Integer result) {
		Assert.state(result != null, "No update count");
		return result;
	}

	private void storeGeneratedKeys(KeyHolder generatedKeyHolder, PreparedStatement ps, int rowsExpected)
			throws SQLException {

		List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
		ResultSet keys = ps.getGeneratedKeys();
		if (keys != null) {
			try {
				RowMapperResultSetExtractor<Map<String, Object>> rse =
						new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), rowsExpected);
				generatedKeys.addAll(result(rse.extractData(keys)));
			}
			finally {
				JdbcUtils.closeResultSet(keys);
			}
		}
	}

	private PreparedStatementCallback<int[]> getPreparedStatementCallback(BatchPreparedStatementSetter pss,
			@Nullable KeyHolder generatedKeyHolder) {
		return ps -> {
			try {
				int batchSize = pss.getBatchSize();
				InterruptibleBatchPreparedStatementSetter ipss =
						(pss instanceof InterruptibleBatchPreparedStatementSetter ibpss ? ibpss : null);
				if (generatedKeyHolder != null) {
					generatedKeyHolder.getKeyList().clear();
				}
				if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						ps.addBatch();
					}
					int[] results = ps.executeBatch();
					if (generatedKeyHolder != null) {
						storeGeneratedKeys(generatedKeyHolder, ps, batchSize);
					}
					return results;
				}
				else {
					List<Integer> rowsAffected = new ArrayList<>();
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						rowsAffected.add(ps.executeUpdate());
						if (generatedKeyHolder != null) {
							storeGeneratedKeys(generatedKeyHolder, ps, 1);
						}
					}
					int[] rowsAffectedArray = new int[rowsAffected.size()];
					for (int i = 0; i < rowsAffectedArray.length; i++) {
						rowsAffectedArray[i] = rowsAffected.get(i);
					}
					return rowsAffectedArray;
				}
			}
			finally {
				if (pss instanceof ParameterDisposer parameterDisposer) {
					parameterDisposer.cleanupParameters();
				}
			}
		};
	}


	/**
	 * Invocation handler that suppresses close calls on JDBC Connections.
	 * Also prepares returned Statement (Prepared/CallbackStatement) objects.
	 * @see java.sql.Connection#close()
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			return switch (method.getName()) {
				// Only consider equal when proxies are identical.
				case "equals" -> (proxy == args[0]);
				// Use hashCode of Connection proxy.
				case "hashCode" -> System.identityHashCode(proxy);
				// Handle close method: suppress, not valid.
				case "close" -> null;
				case "isClosed" -> false;
				// Handle getTargetConnection method: return underlying Connection.
				case "getTargetConnection" -> this.target;
				case "unwrap" ->
						(((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
				case "isWrapperFor" ->
						(((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
				default -> {
					try {
						// Invoke method on target Connection.
						Object retVal = method.invoke(this.target, args);

						// If return value is a JDBC Statement, apply statement settings
						// (fetch size, max rows, transaction timeout).
						if (retVal instanceof Statement statement) {
							applyStatementSettings(statement);
						}

						yield retVal;
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
				}
			};
		}
	}


	/**
	 * Simple adapter for PreparedStatementCreator, allowing to use a plain SQL statement.
	 */
	private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		private final String sql;

		public SimplePreparedStatementCreator(String sql) {
			Assert.notNull(sql, "SQL must not be null");
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(this.sql);
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}


	/**
	 * Simple adapter for CallableStatementCreator, allowing to use a plain SQL statement.
	 */
	private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {

		private final String callString;

		public SimpleCallableStatementCreator(String callString) {
			Assert.notNull(callString, "Call string must not be null");
			this.callString = callString;
		}

		@Override
		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			return con.prepareCall(this.callString);
		}

		@Override
		public String getSql() {
			return this.callString;
		}
	}


	/**
	 * Adapter to enable use of a RowCallbackHandler inside a ResultSetExtractor.
	 * <p>Uses a regular ResultSet, so we have to be careful when using it:
	 * We don't use it for navigating since this could lead to unpredictable consequences.
	 */
	private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<Object> {

		private final RowCallbackHandler rch;

		public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
			this.rch = rch;
		}

		@Override
		@Nullable
		public Object extractData(ResultSet rs) throws SQLException {
			while (rs.next()) {
				this.rch.processRow(rs);
			}
			return null;
		}
	}


	/**
	 * Spliterator for queryForStream adaptation of a ResultSet to a Stream.
	 * @since 5.3
	 */
	private static class ResultSetSpliterator<T> implements Spliterator<T> {

		private final ResultSet rs;

		private final RowMapper<T> rowMapper;

		private int rowNum = 0;

		public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper) {
			this.rs = rs;
			this.rowMapper = rowMapper;
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			try {
				if (this.rs.next()) {
					action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
					return true;
				}
				return false;
			}
			catch (SQLException ex) {
				throw new InvalidResultSetAccessException(ex);
			}
		}

		@Override
		@Nullable
		public Spliterator<T> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED;
		}

		public Stream<T> stream() {
			return StreamSupport.stream(this, false);
		}
	}

}
