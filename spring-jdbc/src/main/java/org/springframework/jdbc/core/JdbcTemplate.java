/*
 * Copyright 2002-2023 the original author or authors.
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

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <b>This is the central delegate in the JDBC core package.</b>
 * It simplifies the use of JDBC and helps to avoid common errors.
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
 * <p>
 * Jdbc模板(JdbcTemplate)
 * <p><b>这是JDBC核心包中的中心委托</b>
 * <p>它简化了JDBC的使用，并有助于避免常见错误。它执行核心JDBC工作流，让应用程序代码提供SQL并提取结果。
 * 此类执行SQL查询或更新，启动对ResultSets进行迭代，捕获JDBC异常并进行转换它们被添加到通用的{@code org.springframework.dao}异常层次结构中。
 * <p>使用此类的代码只需要实现回调接口他们签订了一份明确的合同。
 * {@link PreparedStatementCreator}回调接口在给定Connection的情况下创建一个准备好的语句，提供SQL和任何必要的参数。
 * {@link ResultSetExtractor}接口提取结果集中的值。另请参阅{@link PreparedStatementSetter}和 {@link RowMapper}用于两个流行的替代回调接口。
 * <p>一旦配置，此模板类的实例就是线程安全的。可以通过直接实例化在服务实现中使用使用DataSource引用，或在应用程序上下文中准备
 * 并作为bean引用提供给服务。注意：数据源应该在第一种情况下，始终在应用程序上下文中配置为bean在第二种情况下直接提供给服务。
 * <p>因为这个类可以通过回调接口和{@link org.springframework.jdbc.support.SQLExceptionTranslator}接口，不需要对其进行子类化。
 * <p>此类执行的所有SQL操作都在调试级别记录，使用"org.springframework.jdbc.core.JdbcTemplate"作为日志类别。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
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
 * @since May 3, 2001
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {

	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";

	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";


	/**
	 * If this variable is {@code false}, we will throw exceptions on SQL warnings.
	 * <p>忽略警告
	 * <p>如果此变量为{@code false}，我们将在SQL警告时抛出异常。
	 */
	private boolean ignoreWarnings = true;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * fetchSize property on statements used for query processing.
	 * <p>如果此变量设置为非负值，则将用于设置查询处理的语句上的fetchSize属性。
	 */
	private int fetchSize = -1;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * maxRows property on statements used for query processing.
	 * <p>如果此变量设置为非负值，则将用于设置查询处理的语句上的maxRows属性。
	 */
	private int maxRows = -1;

	/**
	 * If this variable is set to a non-negative value, it will be used for setting the
	 * queryTimeout property on statements used for query processing.
	 * <p>如果此变量设置为非负值，则将用于设置查询处理的语句上的queryTimeout属性。
	 */
	private int queryTimeout = -1;

	/**
	 * If this variable is set to true, then all results checking will be bypassed for any
	 * callable statement processing. This can be used to avoid a bug in some older Oracle
	 * JDBC drivers like 10.1.0.2.
	 * <p>如果此变量设置为true，则任何可调用语句处理都将绕过所有结果检查。这可以用来避免一些旧的Oracle JDBC驱动程序（如10.1.0.2）中的错误。
	 */
	private boolean skipResultsProcessing = false;

	/**
	 * If this variable is set to true then all results from a stored procedure call
	 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
	 * All other results processing will be take place unless the variable
	 * {@code skipResultsProcessing} is set to {@code true}.
	 * <p>如果将此变量设置为true，则将绕过存储过程调用中没有相应SqlOutParameter声明的所有结果。
	 * 除非变量{@code skipResultsProcessing}设置为{@code true}，否则将进行所有其他结果处理。
	 */
	private boolean skipUndeclaredResults = false;

	/**
	 * If this variable is set to true then execution of a CallableStatement will return
	 * the results in a Map that uses case-insensitive names for the parameters.
	 * <p>如果此变量设置为true，则CallableStatement的执行将返回结果是一个Map，它为参数使用不区分大小写的名称。
	 */
	private boolean resultsMapCaseInsensitive = false;


	/**
	 * Construct a new JdbcTemplate for bean usage.
	 * <p>Note: The DataSource has to be set before using the instance.
	 * <p>为bean使用构建一个新的JdbcTemplate。
	 * <p>注意：在使用实例之前，必须设置DataSource。
	 *
	 * @see #setDataSource
	 */
	public JdbcTemplate() {
	}

	/**
	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
	 * <p>Note: This will not trigger initialization of the exception translator.
	 * <p>构造一个新的JdbcTemplate，给定一个DataSource来获取连接。
	 * <p>注意：这不会触发异常转换器的初始化。
	 *
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
	 * <p>构造一个新的JdbcTemplate，给定一个DataSource来获取连接。
	 * <p>注意：根据“lazyInit”标志，初始化异常转换器将被触发。
	 *
	 * @param dataSource the JDBC DataSource to obtain connections from
	 * @param lazyInit   whether to lazily initialize the SQLExceptionTranslator
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
	 *
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
	 *
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
	 *
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
	 *
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
	// 处理普通的java.sql.Connection方法
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
		} catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			String sql = getSql(action);
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("ConnectionCallback", sql, ex);
		} finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * Create a close-suppressing proxy for the given JDBC Connection.
	 * Called by the {@code execute} method.
	 * <p>The proxy also prepares returned JDBC Statements, applying
	 * statement settings such as fetch size, max rows, and query timeout.
	 *
	 * <p>创建连接代理(createConnectionProxy)
	 * <p>为给定的JDBC连接创建一个关闭抑制代理。由{@code execute}方法调用。
	 * <p> 代理还准备返回的JDBC语句，应用语句设置，如获取大小、最大行数和查询超时。
	 *
	 * @param con the JDBC Connection to create a proxy for
	 * @return the Connection proxy
	 * @see java.sql.Connection#close()
	 * @see #execute(ConnectionCallback)
	 * @see #applyStatementSettings
	 */
	protected Connection createConnectionProxy(Connection con) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[]{ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(con));
	}


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	// 处理静态SQL的方法
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
		} catch (SQLException ex) {
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
		} finally {
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
				} finally {
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
					} catch (BatchUpdateException ex) {
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
				} else {
					for (int i = 0; i < sql.length; i++) {
						this.currSql = sql[i];
						if (!stmt.execute(sql[i])) {
							rowsAffected[i] = stmt.getUpdateCount();
						} else {
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
	// 处理预处理语句的方法
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
		// 获取数据库连接
		Connection con = DataSourceUtils.getConnection(obtainDataSource());
		PreparedStatement ps = null;
		try {
			ps = psc.createPreparedStatement(con);
			// 应用用户设定的输入参数
			applyStatementSettings(ps);
			// 调用回调函数
			T result = action.doInPreparedStatement(ps);
			// 警告处理
			handleWarnings(ps);
			return result;
		} catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			// 释放数据空连接避免当异常转换器没有被初始化初始化的时候出现潜在的连接池死锁
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			if (ps != null) {
				handleWarnings(ps, ex);
			}
			String sql = getSql(psc);
			psc = null;
			JdbcUtils.closeStatement(ps);
			ps = null;
			// 资源释放
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw translateException("PreparedStatementCallback", sql, ex);
		} finally {
			if (closeResources) {
				if (psc instanceof ParameterDisposer) {
					((ParameterDisposer) psc).cleanupParameters();
				}
				JdbcUtils.closeStatement(ps);
				// 资源释放
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
	 * <p>使用预处理语句进行查询，允许使用PreparedStatementCreator以及一个预结算人。
	 * 大多数其他查询方法都使用这种方法，但是应用程序代码将始终与创建者或设置者一起工作。
	 *
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param pss a callback that knows how to set values on the prepared statement.
	 *            If this is {@code null}, the SQL will be assumed to contain no bind parameters.
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

		return execute(psc, new PreparedStatementCallback<T>() {
			@Override
			@Nullable
			public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
				ResultSet rs = null;
				try {
					if (pss != null) {
						// 设置 preparedStatement 所需要的参数
						pss.setValues(ps);
					}
					//  执行SQL
					rs = ps.executeQuery();
					// 调用回调函数 将结果进行封装并转换至POJO
					return rse.extractData(rs);
				} finally {
					JdbcUtils.closeResultSet(rs);
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
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
	 * <p>使用预处理语句进行查询，允许使用PreparedStatementCreator以及一个预结算人。
	 * 大多数其他查询方法都使用这种方法，但是应用程序代码将始终与创建者或设置者一起工作。
	 *
	 * @param psc       a callback that creates a PreparedStatement given a Connection
	 * @param pss       a callback that knows how to set values on the prepared statement.
	 *                  If this is {@code null}, the SQL will be assumed to contain no bind parameters.
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
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
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
	public <T> T queryForObject(String sql, @Nullable Object[] args, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
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
		// 回调函数, 用于处理通用方法execute无法处理的个性化处理方法
		return updateCount(execute(psc, ps -> {
			try {
				if (pss != null) {
					// 设置 preparedStatement 所需要的参数
					pss.setValues(ps);
				}
				//  执行SQL
				int rows = ps.executeUpdate();
				if (logger.isTraceEnabled()) {
					logger.trace("SQL update affected " + rows + " rows");
				}
				return rows;
			} finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
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
			List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
			generatedKeys.clear();
			ResultSet keys = ps.getGeneratedKeys();
			if (keys != null) {
				try {
					RowMapperResultSetExtractor<Map<String, Object>> rse =
							new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), 1);
					generatedKeys.addAll(result(rse.extractData(keys)));
				} finally {
					JdbcUtils.closeResultSet(keys);
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeys.size() + " keys");
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
	public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "]");
		}

		int[] result = execute(sql, (PreparedStatementCallback<int[]>) ps -> {
			try {
				int batchSize = pss.getBatchSize();
				InterruptibleBatchPreparedStatementSetter ipss =
						(pss instanceof InterruptibleBatchPreparedStatementSetter ?
								(InterruptibleBatchPreparedStatementSetter) pss : null);
				if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						ps.addBatch();
					}
					return ps.executeBatch();
				} else {
					List<Integer> rowsAffected = new ArrayList<>();
					for (int i = 0; i < batchSize; i++) {
						pss.setValues(ps, i);
						if (ipss != null && ipss.isBatchExhausted(i)) {
							break;
						}
						rowsAffected.add(ps.executeUpdate());
					}
					int[] rowsAffectedArray = new int[rowsAffected.size()];
					for (int i = 0; i < rowsAffectedArray.length; i++) {
						rowsAffectedArray[i] = rowsAffected.get(i);
					}
					return rowsAffectedArray;
				}
			} finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		});

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
							if (value instanceof SqlParameterValue) {
								SqlParameterValue paramValue = (SqlParameterValue) value;
								StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
							} else {
								int colType;
								if (argTypes.length < colIndex) {
									colType = SqlTypeValue.TYPE_UNKNOWN;
								} else {
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
							rowsAffected.add(ps.executeBatch());
						}
					} else {
						int i = ps.executeUpdate();
						rowsAffected.add(new int[]{i});
					}
				}
				int[][] result1 = new int[rowsAffected.size()][];
				for (int i = 0; i < result1.length; i++) {
					result1[i] = rowsAffected.get(i);
				}
				return result1;
			} finally {
				if (pss instanceof ParameterDisposer) {
					((ParameterDisposer) pss).cleanupParameters();
				}
			}
		});

		Assert.state(result != null, "No result array");
		return result;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	// 处理可调用语句的方法
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
		} catch (SQLException ex) {
			// Release Connection early, to avoid potential connection pool deadlock
			// in the case when the exception translator hasn't been initialized yet.
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
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
		} finally {
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
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
				} else {
					updateCountParameters.add(parameter);
				}
			} else {
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
	 * <p>从已完成的存储过程中提取返回的ResultSets
	 *
	 * @param cs                    a JDBC wrapper for the stored procedure
	 * @param updateCountParameters the parameter list of declared update count parameters for the stored procedure
	 * @param resultSetParameters   the parameter list of declared resultSet parameters for the stored procedure
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
					} else {
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
				} else {
					if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
						SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
						String declaredUcName = ucParam.getName();
						results.put(declaredUcName, updateCount);
						updateIndex++;
					} else {
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
	 * <p>从已完成的存储过程中提取输出参数
	 *
	 * @param cs         the JDBC wrapper for the stored procedure
	 * @param parameters parameter list for the stored procedure
	 * @return a Map that contains returned results
	 */
	protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters)
			throws SQLException {

		Map<String, Object> results = CollectionUtils.newLinkedHashMap(parameters.size());
		int sqlColIndex = 1;
		for (SqlParameter param : parameters) {
			if (param instanceof SqlOutParameter) {
				SqlOutParameter outParam = (SqlOutParameter) param;
				Assert.state(outParam.getName() != null, "Anonymous parameters not allowed");
				SqlReturnType returnType = outParam.getSqlReturnType();
				if (returnType != null) {
					Object out = returnType.getTypeValue(cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
					results.put(outParam.getName(), out);
				} else {
					Object out = cs.getObject(sqlColIndex);
					if (out instanceof ResultSet) {
						if (outParam.isResultSetSupported()) {
							results.putAll(processResultSet((ResultSet) out, outParam));
						} else {
							String rsName = outParam.getName();
							SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							results.putAll(processResultSet((ResultSet) out, rsParam));
							if (logger.isTraceEnabled()) {
								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
						}
					} else {
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
	 * <p>处理存储过程中的给定ResultSet
	 *
	 * @param rs    the ResultSet to process
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
				} else if (param.getRowCallbackHandler() != null) {
					RowCallbackHandler rch = param.getRowCallbackHandler();
					(new RowCallbackHandlerResultSetExtractor(rch)).extractData(rs);
					return Collections.singletonMap(param.getName(),
							"ResultSet returned from stored procedure was processed");
				} else if (param.getResultSetExtractor() != null) {
					Object data = param.getResultSetExtractor().extractData(rs);
					return Collections.singletonMap(param.getName(), data);
				}
			} finally {
				JdbcUtils.closeResultSet(rs);
			}
		}
		return Collections.emptyMap();
	}


	//-------------------------------------------------------------------------
	// Implementation hooks and helper methods
	// 实现钩子和辅助方法
	//-------------------------------------------------------------------------

	/**
	 * Create a new RowMapper for reading columns as key-value pairs.
	 * <p>创建一个新的RowMapper，用于将列作为键值对读取
	 *
	 * @return the RowMapper to use
	 * @see ColumnMapRowMapper
	 */
	protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
		return new ColumnMapRowMapper();
	}

	/**
	 * Create a new RowMapper for reading result objects from a single column.
	 * <p>创建一个新的RowMapper，用于从单个列读取结果对象
	 *
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
	 * <p>创建一个用作结果图的Map实例。
	 * <p>如果{@link #resultsMapCaseInsensitive}已设置为true，将创建一个{@link LinkedCaseInsensitiveMap}；
	 * 否则，将创建一个{@link LinkedHashMap}。
	 *
	 * @return the results Map instance
	 * @see #setResultsMapCaseInsensitive
	 * @see #isResultsMapCaseInsensitive
	 */
	protected Map<String, Object> createResultsMap() {
		if (isResultsMapCaseInsensitive()) {
			return new LinkedCaseInsensitiveMap<>();
		} else {
			return new LinkedHashMap<>();
		}
	}

	/**
	 * Prepare the given JDBC Statement (or PreparedStatement or CallableStatement),
	 * applying statement settings such as fetch size, max rows, and query timeout.
	 * <p>准备给定的JDBC语句(或PreparedStatement或CallableStatement),应用语句设置,
	 * 如获取大小、最大行数和查询超时
	 *
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
			// setFetchSize 给JDBC驱动程序一个提示，告诉它应该有多少行
			// 当需要更多行时，从数据库中获取<code>结果集</code>对象由此<code>语句</code]生成。
			// 如果指定的值为零，则忽略提示。默认值为零。
			// 主要为了减少网络交互次数设计. 访问ResultSet时，JDBC驱动程序将尝试获取fetchSize行。
			// 如果每次只读取一行数据, 则会产生大量开销.setFetchSize当调用rs.next()时，JDBC驱动程序将获取fetchSize行。
			// 这样下次调用rs.next(), 可以直接从内存中获取而不需要网络交换, 减少网络开销.
			// 该设置可能会被某些JDBC驱动忽略, 而且设置过大也会造成内存上升.
			stmt.setFetchSize(fetchSize);
		}
		int maxRows = getMaxRows();
		if (maxRows != -1) {
			// setMaxRows 将此Statement对象产生的所有ResultSet对象可以包含的最大航海术限制设置，如果超过这个值，将不会返回所有的行。
			// 如果设置为0，则表示没有限制。默认值为0。
			stmt.setMaxRows(maxRows);
		}
		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
	}

	/**
	 * Create a new arg-based PreparedStatementSetter using the args passed in.
	 * <p>By default, we'll create an {@link ArgumentPreparedStatementSetter}.
	 * This method allows for the creation to be overridden by subclasses.
	 * <p>使用传入的参数创建一个新的基于参数的PreparedStateSetter。
	 * <p>默认情况下，我们将创建一个{@link ArgumentPreparedStatementSetter}。
	 * 此方法允许子类覆盖创建。
	 *
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
	 *
	 * <p>新的参数类型准备语句Setter(newArgTypePreparedStatementSetter)
	 * <p>使用传入的参数和类型创建一个新的基于参数类型的PreparedStateSetter.
	 * <p>默认情况下，我们将创建一个{@link ArgumentTypePreparedStatementSetter}.
	 * 此方法允许子类覆盖创建。
	 *
	 * @param args     object array with arguments
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
	 *
	 * @param stmt the current JDBC statement
	 * @param ex   the primary exception after failed statement execution
	 * @see #handleWarnings(Statement)
	 * @see SQLException#setNextException
	 * @since 5.3.29
	 */
	protected void handleWarnings(Statement stmt, SQLException ex) {
		try {
			handleWarnings(stmt);
		} catch (SQLWarningException nonIgnoredWarning) {
			ex.setNextException(nonIgnoredWarning.getSQLWarning());
		} catch (SQLException warningsEx) {
			logger.debug("Failed to retrieve warnings", warningsEx);
		} catch (Throwable warningsEx) {
			logger.debug("Failed to process warnings", warningsEx);
		}
	}

	/**
	 * Handle the warnings for the given JDBC statement, if any.
	 * <p>Throws a {@link SQLWarningException} if we're not ignoring warnings,
	 * otherwise logs the warnings at debug level.
	 *
	 * <p>处理给定JDBC语句的警告（如果有的话）。
	 * <p>如果我们没有忽略警告，则抛出{@link SQLWarningException}，否则，在调试级别记录警告。
	 * <p>这里用到了一个类SQLWarning，SQLWarning提供关于数据库访问警告信息的异常。这些
	 * 警告直接链接到导致报告警告的方法所在的对象。警告可以从Connection、Statement和
	 * ResutSet对象中获得。试图在已经关闭的连接上获取警告将导致抛出异常。类似地，试图在已
	 * 经关闭的语上或已经关闭的结果集上获取警告也将导致抛出异常。注意，关闭语句时还会关
	 * 闭它可能生成的结果集。
	 * <p>很多人不是很理解什么情况下会产生警告而不是异常，在这里给读者提示个最常见的警告
	 * DataTruncation：DataTruncation直接继承SQLWarning，由于某种原因意外地截断数据值时会以
	 * DataTruncation警告形式报告异常。
	 * <p>对于警告的处理方式并不是直接抛出异常，出现警告很可能会出现数据错误，但是，并不
	 * 一定会影响程序执行，所以用户可以自已设置处理警告的方式，如默认的是忽略警告，当出现
	 * 警告时只打印警告日志，而另一种方式只直接抛出异常。
	 *
	 * @param stmt the current JDBC statement
	 * @throws SQLException        in case of warnings retrieval failure
	 * @throws SQLWarningException for a concrete warning to raise
	 *                             (when not ignoring warnings)
	 * @see #setIgnoreWarnings
	 * @see #handleWarnings(SQLWarning)
	 */
	protected void handleWarnings(Statement stmt) throws SQLException, SQLWarningException {
		// 当设置为忽略警告时, 只尝试打印日志
		if (isIgnoreWarnings()) {
			if (logger.isDebugEnabled()) {
				// 如果日志开启的情况下打印日志
				SQLWarning warningToLog = stmt.getWarnings();
				while (warningToLog != null) {
					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		} else {
			handleWarnings(stmt.getWarnings());
		}
	}

	/**
	 * Throw a {@link SQLWarningException} if encountering an actual warning.
	 *
	 * @param warning the warnings object from the current statement.
	 *                May be {@code null}, in which case this method does nothing.
	 * @throws SQLWarningException in case of an actual warning to be raised
	 */
	protected void handleWarnings(@Nullable SQLWarning warning) throws SQLWarningException {
		if (warning != null) {
			throw new SQLWarningException("Warning not ignored", warning);
		}
	}

	/**
	 * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
	 * 将给定的 {@link SQLException} 转换为通用{@link DataAccessException}
	 *
	 * @param task readable text describing the task being attempted
	 * @param sql  the SQL query or update that caused the problem (may be {@code null})
	 * @param ex   the offending {@code SQLException}
	 * @return a DataAccessException wrapping the {@code SQLException} (never {@code null})
	 * @see #getExceptionTranslator()
	 * @since 5.0
	 */
	protected DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
		DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
		return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
	}


	/**
	 * Determine SQL from potential provider object.
	 * <p>从潜在提供程序对象确定SQL
	 *
	 * @param sqlProvider object which is potentially an SqlProvider
	 * @return the SQL string, or {@code null} if not known
	 * @see SqlProvider
	 */
	@Nullable
	private static String getSql(Object sqlProvider) {
		if (sqlProvider instanceof SqlProvider) {
			return ((SqlProvider) sqlProvider).getSql();
		} else {
			return null;
		}
	}

	private static <T> T result(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}

	private static int updateCount(@Nullable Integer result) {
		Assert.state(result != null, "No update count");
		return result;
	}


	/**
	 * Invocation handler that suppresses close calls on JDBC Connections.
	 * Also prepares returned Statement (Prepared/CallbackStatement) objects.
	 * <p>抑制JDBC连接上的关闭调用的调用处理程序。还准备返回的语句（Prepared/callback语句）对象。
	 *
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

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of PersistenceManager proxy.
					return System.identityHashCode(proxy);
				case "close":
					// Handle close method: suppress, not valid.
					return null;
				case "isClosed":
					return false;
				case "getTargetConnection":
					// Handle getTargetConnection method: return underlying Connection.
					return this.target;
				case "unwrap":
					return (((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
				case "isWrapperFor":
					return (((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
			}

			// Invoke method on target Connection.
			try {
				Object retVal = method.invoke(this.target, args);

				// If return value is a JDBC Statement, apply statement settings
				// (fetch size, max rows, transaction timeout).
				if (retVal instanceof Statement) {
					applyStatementSettings(((Statement) retVal));
				}

				return retVal;
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Simple adapter for PreparedStatementCreator, allowing to use a plain SQL statement.
	 * PreparedStatementCreator的简单适配器，允许使用普通SQL语句
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
	 * CallableStatementCreator的简单适配器，允许使用普通SQL语句
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
	 *
	 * <p>行回调处理程序结果集提取器(RowCallbackHandlerResultSetExtractor)
	 * <p>适配器，用于在ResultSetExtractor中启用 RowCallbackHandler。
	 * <p>使用常规的ResultSet，因此我们在使用它时必须小心：
	 * 我们不使用它进行导航，因为这可能会导致不可预测的后果。
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
	 * Spliterator用于queryForStream将ResultSet自适应为Stream。
	 *
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
			} catch (SQLException ex) {
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
