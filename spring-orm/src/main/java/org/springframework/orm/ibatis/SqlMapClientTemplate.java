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

package org.springframework.orm.ibatis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.util.Assert;

/**
 * Helper class that simplifies data access via the iBATIS
 * {@link com.ibatis.sqlmap.client.SqlMapClient} API, converting checked
 * SQLExceptions into unchecked DataAccessExceptions, following the
 * {@code org.springframework.dao} exception hierarchy.
 * Uses the same {@link org.springframework.jdbc.support.SQLExceptionTranslator}
 * mechanism as {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * <p>The main method of this class executes a callback that implements a
 * data access action. Furthermore, this class provides numerous convenience
 * methods that mirror {@link com.ibatis.sqlmap.client.SqlMapExecutor}'s
 * execution methods.
 *
 * <p>It is generally recommended to use the convenience methods on this template
 * for plain query/insert/update/delete operations. However, for more complex
 * operations like batch updates, a custom SqlMapClientCallback must be implemented,
 * usually as anonymous inner class. For example:
 *
 * <pre class="code">
 * getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
 * 	 public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
 * 		 executor.startBatch();
 * 		 executor.update("insertSomething", "myParamValue");
 * 		 executor.update("insertSomethingElse", "myOtherParamValue");
 * 		 executor.executeBatch();
 * 		 return null;
 *      }
 * });</pre>
 *
 * The template needs a SqlMapClient to work on, passed in via the "sqlMapClient"
 * property. A Spring context typically uses a {@link SqlMapClientFactoryBean}
 * to build the SqlMapClient. The template an additionally be configured with a
 * DataSource for fetching Connections, although this is not necessary if a
 * DataSource is specified for the SqlMapClient itself (typically through
 * SqlMapClientFactoryBean's "dataSource" property).
 *
 * @author Juergen Hoeller
 * @since 24.02.2004
 * @see #execute
 * @see #setSqlMapClient
 * @see #setDataSource
 * @see #setExceptionTranslator
 * @see SqlMapClientFactoryBean#setDataSource
 * @see com.ibatis.sqlmap.client.SqlMapClient#getDataSource
 * @see com.ibatis.sqlmap.client.SqlMapExecutor
 * @deprecated as of Spring 3.2, in favor of the native Spring support
 * in the Mybatis follow-up project (http://code.google.com/p/mybatis/)
 */
@Deprecated
public class SqlMapClientTemplate extends JdbcAccessor implements SqlMapClientOperations {

	private SqlMapClient sqlMapClient;


	/**
	 * Create a new SqlMapClientTemplate.
	 */
	public SqlMapClientTemplate() {
	}

	/**
	 * Create a new SqlMapTemplate.
	 * @param sqlMapClient iBATIS SqlMapClient that defines the mapped statements
	 */
	public SqlMapClientTemplate(SqlMapClient sqlMapClient) {
		setSqlMapClient(sqlMapClient);
		afterPropertiesSet();
	}

	/**
	 * Create a new SqlMapTemplate.
	 * @param dataSource JDBC DataSource to obtain connections from
	 * @param sqlMapClient iBATIS SqlMapClient that defines the mapped statements
	 */
	public SqlMapClientTemplate(DataSource dataSource, SqlMapClient sqlMapClient) {
		setDataSource(dataSource);
		setSqlMapClient(sqlMapClient);
		afterPropertiesSet();
	}


	/**
	 * Set the iBATIS Database Layer SqlMapClient that defines the mapped statements.
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClient = sqlMapClient;
	}

	/**
	 * Return the iBATIS Database Layer SqlMapClient that this template works with.
	 */
	public SqlMapClient getSqlMapClient() {
		return this.sqlMapClient;
	}

	/**
	 * If no DataSource specified, use SqlMapClient's DataSource.
	 * @see com.ibatis.sqlmap.client.SqlMapClient#getDataSource()
	 */
	@Override
	public DataSource getDataSource() {
		DataSource ds = super.getDataSource();
		return (ds != null ? ds : this.sqlMapClient.getDataSource());
	}

	@Override
	public void afterPropertiesSet() {
		if (this.sqlMapClient == null) {
			throw new IllegalArgumentException("Property 'sqlMapClient' is required");
		}
		super.afterPropertiesSet();
	}


	/**
	 * Execute the given data access action on a SqlMapExecutor.
	 * @param action callback object that specifies the data access action
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException in case of SQL Maps errors
	 */
	public <T> T execute(SqlMapClientCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		Assert.notNull(this.sqlMapClient, "No SqlMapClient specified");

		// We always need to use a SqlMapSession, as we need to pass a Spring-managed
		// Connection (potentially transactional) in. This shouldn't be necessary if
		// we run against a TransactionAwareDataSourceProxy underneath, but unfortunately
		// we still need it to make iBATIS batch execution work properly: If iBATIS
		// doesn't recognize an existing transaction, it automatically executes the
		// batch for every single statement...

		SqlMapSession session = this.sqlMapClient.openSession();
		if (logger.isDebugEnabled()) {
			logger.debug("Opened SqlMapSession [" + session + "] for iBATIS operation");
		}
		Connection ibatisCon = null;

		try {
			Connection springCon = null;
			DataSource dataSource = getDataSource();
			boolean transactionAware = (dataSource instanceof TransactionAwareDataSourceProxy);

			// Obtain JDBC Connection to operate on...
			try {
				ibatisCon = session.getCurrentConnection();
				if (ibatisCon == null) {
					springCon = (transactionAware ?
							dataSource.getConnection() : DataSourceUtils.doGetConnection(dataSource));
					session.setUserConnection(springCon);
					if (logger.isDebugEnabled()) {
						logger.debug("Obtained JDBC Connection [" + springCon + "] for iBATIS operation");
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Reusing JDBC Connection [" + ibatisCon + "] for iBATIS operation");
					}
				}
			}
			catch (SQLException ex) {
				throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
			}

			// Execute given callback...
			try {
				return action.doInSqlMapClient(session);
			}
			catch (SQLException ex) {
				throw getExceptionTranslator().translate("SqlMapClient operation", null, ex);
			}
			finally {
				try {
					if (springCon != null) {
						if (transactionAware) {
							springCon.close();
						}
						else {
							DataSourceUtils.doReleaseConnection(springCon, dataSource);
						}
					}
				}
				catch (Throwable ex) {
					logger.debug("Could not close JDBC Connection", ex);
				}
			}

			// Processing finished - potentially session still to be closed.
		}
		finally {
			// Only close SqlMapSession if we know we've actually opened it
			// at the present level.
			if (ibatisCon == null) {
				session.close();
			}
		}
	}

	/**
	 * Execute the given data access action on a SqlMapExecutor,
	 * expecting a List result.
	 * @param action callback object that specifies the data access action
	 * @return the List result
	 * @throws DataAccessException in case of SQL Maps errors
	 * @deprecated as of Spring 3.0 - not really needed anymore with generic
	 * {@link #execute} method
	 */
	@Deprecated
	public List executeWithListResult(SqlMapClientCallback<List> action) throws DataAccessException {
		return execute(action);
	}

	/**
	 * Execute the given data access action on a SqlMapExecutor,
	 * expecting a Map result.
	 * @param action callback object that specifies the data access action
	 * @return the Map result
	 * @throws DataAccessException in case of SQL Maps errors
	 * @deprecated as of Spring 3.0 - not really needed anymore with generic
	 * {@link #execute} method
	 */
	@Deprecated
	public Map executeWithMapResult(SqlMapClientCallback<Map> action) throws DataAccessException {
		return execute(action);
	}


	@Override
	public Object queryForObject(String statementName) throws DataAccessException {
		return queryForObject(statementName, null);
	}

	@Override
	public Object queryForObject(final String statementName, final Object parameterObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject);
			}
		});
	}

	@Override
	public Object queryForObject(
			final String statementName, final Object parameterObject, final Object resultObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject, resultObject);
			}
		});
	}

	@Override
	public List queryForList(String statementName) throws DataAccessException {
		return queryForList(statementName, null);
	}

	@Override
	public List queryForList(final String statementName, final Object parameterObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<List>() {
			@Override
			public List doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForList(statementName, parameterObject);
			}
		});
	}

	@Override
	public List queryForList(String statementName, int skipResults, int maxResults)
			throws DataAccessException {

		return queryForList(statementName, null, skipResults, maxResults);
	}

	@Override
	public List queryForList(
			final String statementName, final Object parameterObject, final int skipResults, final int maxResults)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<List>() {
			@Override
			public List doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForList(statementName, parameterObject, skipResults, maxResults);
			}
		});
	}

	@Override
	public void queryWithRowHandler(String statementName, RowHandler rowHandler)
			throws DataAccessException {

		queryWithRowHandler(statementName, null, rowHandler);
	}

	@Override
	public void queryWithRowHandler(
			final String statementName, final Object parameterObject, final RowHandler rowHandler)
			throws DataAccessException {

		execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				executor.queryWithRowHandler(statementName, parameterObject, rowHandler);
				return null;
			}
		});
	}

	@Override
	public Map queryForMap(
			final String statementName, final Object parameterObject, final String keyProperty)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Map>() {
			@Override
			public Map doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForMap(statementName, parameterObject, keyProperty);
			}
		});
	}

	@Override
	public Map queryForMap(
			final String statementName, final Object parameterObject, final String keyProperty, final String valueProperty)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Map>() {
			@Override
			public Map doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForMap(statementName, parameterObject, keyProperty, valueProperty);
			}
		});
	}

	@Override
	public Object insert(String statementName) throws DataAccessException {
		return insert(statementName, null);
	}

	@Override
	public Object insert(final String statementName, final Object parameterObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.insert(statementName, parameterObject);
			}
		});
	}

	@Override
	public int update(String statementName) throws DataAccessException {
		return update(statementName, null);
	}

	@Override
	public int update(final String statementName, final Object parameterObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Integer>() {
			@Override
			public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.update(statementName, parameterObject);
			}
		});
	}

	@Override
	public void update(String statementName, Object parameterObject, int requiredRowsAffected)
			throws DataAccessException {

		int actualRowsAffected = update(statementName, parameterObject);
		if (actualRowsAffected != requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(
					statementName, requiredRowsAffected, actualRowsAffected);
		}
	}

	@Override
	public int delete(String statementName) throws DataAccessException {
		return delete(statementName, null);
	}

	@Override
	public int delete(final String statementName, final Object parameterObject)
			throws DataAccessException {

		return execute(new SqlMapClientCallback<Integer>() {
			@Override
			public Integer doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.delete(statementName, parameterObject);
			}
		});
	}

	@Override
	public void delete(String statementName, Object parameterObject, int requiredRowsAffected)
			throws DataAccessException {

		int actualRowsAffected = delete(statementName, parameterObject);
		if (actualRowsAffected != requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(
					statementName, requiredRowsAffected, actualRowsAffected);
		}
	}

}
