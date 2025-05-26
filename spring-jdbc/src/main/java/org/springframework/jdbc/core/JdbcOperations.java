/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Interface specifying a basic set of JDBC operations.
 *
 * <p>Implemented by {@link JdbcTemplate}. Not often used directly, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Alternatively, the standard JDBC infrastructure can be mocked.
 * However, mocking this interface constitutes significantly less work.
 * As an alternative to a mock objects approach to testing data access code,
 * consider the powerful integration testing support provided via the
 * <em>Spring TestContext Framework</em>, in the {@code spring-test} artifact.
 *
 * <p><b>NOTE: As of 6.1, there is a unified JDBC access facade available in
 * the form of {@link org.springframework.jdbc.core.simple.JdbcClient}.</b>
 * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
 * with flexible use of indexed or named parameters. It delegates to
 * {@code JdbcOperations}/{@code NamedParameterJdbcOperations} for actual execution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
 */
public interface JdbcOperations {

	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC Connection. This allows for implementing arbitrary
	 * data access operations, within Spring's managed JDBC environment:
	 * that is, participating in Spring-managed transactions and converting
	 * JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param action a callback object that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(ConnectionCallback<T> action) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC Statement. This allows for implementing arbitrary data
	 * access operations on a single Statement, within Spring's managed JDBC
	 * environment: that is, participating in Spring-managed transactions and
	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(StatementCallback<T> action) throws DataAccessException;

	/**
	 * Issue a single SQL execute, typically a DDL statement.
	 * @param sql static SQL to execute
	 * @throws DataAccessException if there is any problem
	 */
	void execute(String sql) throws DataAccessException;

	/**
	 * Execute a query given static SQL, reading the ResultSet with a
	 * ResultSetExtractor.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code query} method with {@code null} as argument array.
	 * @param sql the SQL query to execute
	 * @param rse a callback that will extract all rows of results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, ResultSetExtractor, Object...)
	 */
	<T> @Nullable T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * Execute a query given static SQL, reading the ResultSet on a per-row
	 * basis with a RowCallbackHandler.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code query} method with {@code null} as argument array.
	 * @param sql the SQL query to execute
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, RowCallbackHandler, Object...)
	 */
	void query(String sql, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * Execute a query given static SQL, mapping each row to a result object
	 * via a RowMapper.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code query} method with {@code null} as argument array.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, RowMapper, Object...)
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Execute a query given static SQL, mapping each row to a result object
	 * via a RowMapper, and turning it into an iterable and closeable Stream.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code query} method with {@code null} as argument array.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (for example, through a try-with-resources clause)
	 * @throws DataAccessException if there is any problem executing the query
	 * @since 5.3
	 * @see #queryForStream(String, RowMapper, Object...)
	 */
	<T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Execute a query given static SQL, mapping a single result row to a
	 * result object via a RowMapper.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@link #queryForObject(String, RowMapper, Object...)} method with
	 * {@code null} as argument array.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForObject(String, RowMapper, Object...)
	 */
	<T> @Nullable T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Execute a query for a result object, given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@link #queryForObject(String, Class, Object...)} method with
	 * {@code null} as argument array.
	 * <p>This method is useful for running static SQL with a known outcome.
	 * The query is expected to be a single row/single column query; the returned
	 * result will be directly mapped to the corresponding object type.
	 * @param sql the SQL query to execute
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForObject(String, Class, Object...)
	 */
	<T> @Nullable T queryForObject(String sql, Class<T> requiredType) throws DataAccessException;

	/**
	 * Execute a query for a result map, given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@link #queryForMap(String, Object...)} method with {@code null}
	 * as argument array.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map (one entry for each column, using the column name as the key).
	 * @param sql the SQL query to execute
	 * @return the result Map (one entry per column, with column name as key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForMap(String, Object...)
	 * @see ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql) throws DataAccessException;

	/**
	 * Execute a query for a result list, given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code queryForList} method with {@code null} as argument array.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * result objects, each of them matching the specified element type.
	 * @param sql the SQL query to execute
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForList(String, Class, Object...)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException;

	/**
	 * Execute a query for a result list, given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code queryForList} method with {@code null} as argument array.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * Maps (one entry for each column using the column name as the key).
	 * Each element in the list will be of the form returned by this interface's
	 * {@code queryForMap} methods.
	 * @param sql the SQL query to execute
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForList(String, Object...)
	 */
	List<Map<String, Object>> queryForList(String sql) throws DataAccessException;

	/**
	 * Execute a query for an SqlRowSet, given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
	 * execute a static query with a PreparedStatement, use the overloaded
	 * {@code queryForRowSet} method with {@code null} as argument array.
	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
	 * is used.
	 * @param sql the SQL query to execute
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String, Object...)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql) throws DataAccessException;

	/**
	 * Issue a single SQL update operation (such as an insert, update or delete statement).
	 * @param sql static SQL to execute
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem.
	 */
	int update(String sql) throws DataAccessException;

	/**
	 * Issue multiple SQL updates on a single JDBC Statement using batching.
	 * <p>Will fall back to separate updates on a single Statement if the JDBC
	 * driver does not support batch updates.
	 * @param sql defining an array of SQL statements that will be executed.
	 * @return an array of the number of rows affected by each statement
	 * @throws DataAccessException if there is any problem executing the batch
	 */
	int[] batchUpdate(String... sql) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC PreparedStatement. This allows for implementing arbitrary
	 * data access operations on a single Statement, within Spring's managed JDBC
	 * environment: that is, participating in Spring-managed transactions and
	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC PreparedStatement. This allows for implementing arbitrary
	 * data access operations on a single Statement, within Spring's managed JDBC
	 * environment: that is, participating in Spring-managed transactions and
	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param sql the SQL to execute
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

	/**
	 * Query using a prepared statement, reading the ResultSet with a ResultSetExtractor.
	 * <p>A PreparedStatementCreator can either be implemented directly or
	 * configured through a PreparedStatementCreatorFactory.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	<T> @Nullable T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * Query using a prepared statement, reading the ResultSet with a ResultSetExtractor.
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of arguments
	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	<T> @Nullable T query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of arguments
	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rse a callback that will extract results
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, ResultSetExtractor, Object...)}
	 */
	@Deprecated(since = "5.3")
	<T> @Nullable T query(String sql, @Nullable Object @Nullable [] args, ResultSetExtractor<T> rse) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of arguments
	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
	 * @param sql the SQL query to execute
	 * @param rse a callback that will extract results
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return an arbitrary result object, as returned by the ResultSetExtractor
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	<T> @Nullable T query(String sql, ResultSetExtractor<T> rse, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query using a prepared statement, reading the ResultSet on a per-row basis
	 * with a RowCallbackHandler.
	 * <p>A PreparedStatementCreator can either be implemented directly or
	 * configured through a PreparedStatementCreatorFactory.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a
	 * PreparedStatementSetter implementation that knows how to bind values to the
	 * query, reading the ResultSet on a per-row basis with a RowCallbackHandler.
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 */
	void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, reading the ResultSet on a per-row basis
	 * with a RowCallbackHandler.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	void query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, reading the ResultSet on a per-row basis
	 * with a RowCallbackHandler.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rch a callback that will extract results, one row at a time
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, RowCallbackHandler, Object...)}
	 */
	@Deprecated(since = "5.3")
	void query(String sql, @Nullable Object @Nullable [] args, RowCallbackHandler rch) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, reading the ResultSet on a per-row basis
	 * with a RowCallbackHandler.
	 * @param sql the SQL query to execute
	 * @param rch a callback that will extract results, one row at a time
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	void query(String sql, RowCallbackHandler rch, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query using a prepared statement, mapping each row to a result object
	 * via a RowMapper.
	 * <p>A PreparedStatementCreator can either be implemented directly or
	 * configured through a PreparedStatementCreatorFactory.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 */
	<T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a
	 * PreparedStatementSetter implementation that knows how to bind values
	 * to the query, mapping each row to a result object via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 */
	<T> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, mapping each row to a result object
	 * via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @see java.sql.Types
	 */
	<T> List<T> query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, mapping each row to a result object
	 * via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rowMapper a callback that will map one object per row
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #query(String, RowMapper, Object...)}
	 */
	@Deprecated(since = "5.3")
	<T> List<T> query(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, mapping each row to a result object
	 * via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result List, containing mapped objects
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	<T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query using a prepared statement, mapping each row to a result object
	 * via a RowMapper, and turning it into an iterable and closeable Stream.
	 * <p>A PreparedStatementCreator can either be implemented directly or
	 * configured through a PreparedStatementCreatorFactory.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (for example, through a try-with-resources clause)
	 * @throws DataAccessException if there is any problem
	 * @see PreparedStatementCreatorFactory
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a
	 * PreparedStatementSetter implementation that knows how to bind values
	 * to the query, mapping each row to a result object via a RowMapper,
	 * and turning it into an iterable and closeable Stream.
	 * @param sql the SQL query to execute
	 * @param pss a callback that knows how to set values on the prepared statement.
	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this callback may be used to set the
	 * fetch size and other performance options.
	 * @param rowMapper a callback that will map one object per row
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (for example, through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, mapping each row to a result object
	 * via a RowMapper, and turning it into an iterable and closeable Stream.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result Stream, containing mapped objects, needing to be
	 * closed once fully processed (for example, through a try-with-resources clause)
	 * @throws DataAccessException if the query fails
	 * @since 5.3
	 */
	<T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list
	 * of arguments to bind to the query, mapping a single result row to a
	 * result object via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 */
	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list
	 * of arguments to bind to the query, mapping a single result row to a
	 * result object via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param rowMapper a callback that will map one object per row
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @deprecated as of 5.3, in favor of {@link #queryForObject(String, RowMapper, Object...)}
	 */
	@Deprecated(since = "5.3")
	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list
	 * of arguments to bind to the query, mapping a single result row to a
	 * result object via a RowMapper.
	 * @param sql the SQL query to execute
	 * @param rowMapper a callback that will map one object per row
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the single mapped object (may be {@code null} if the given
	 * {@link RowMapper} returned {@code null})
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 */
	<T> @Nullable T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result object.
	 * <p>The query is expected to be a single row/single column query; the returned
	 * result will be directly mapped to the corresponding object type.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see #queryForObject(String, Class)
	 * @see java.sql.Types
	 */
	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result object.
	 * <p>The query is expected to be a single row/single column query; the returned
	 * result will be directly mapped to the corresponding object type.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param requiredType the type that the result object is expected to match
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @see #queryForObject(String, Class)
	 * @deprecated as of 5.3, in favor of {@link #queryForObject(String, Class, Object...)}
	 */
	@Deprecated(since = "5.3")
	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, Class<T> requiredType) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result object.
	 * <p>The query is expected to be a single row/single column query; the returned
	 * result will be directly mapped to the corresponding object type.
	 * @param sql the SQL query to execute
	 * @param requiredType the type that the result object is expected to match
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result object of the required type, or {@code null} in case of SQL NULL
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
	 * if the query does not return a row containing a single column
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 * @see #queryForObject(String, Class)
	 */
	<T> @Nullable T queryForObject(String sql, Class<T> requiredType, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result map.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map (one entry for each column, using the column name as the key).
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return the result Map (one entry per column, with column name as key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see #queryForMap(String)
	 * @see ColumnMapRowMapper
	 * @see java.sql.Types
	 */
	Map<String, Object> queryForMap(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result map.
	 * <p>The {@code queryForMap} methods defined by this interface are appropriate
	 * when you don't have a domain model. Otherwise, consider using one of the
	 * {@code queryForObject} methods.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map (one entry for each column, using the column name as the key).
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the result Map (one entry for each column, using the
	 * column name as the key)
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
	 * if the query does not return exactly one row
	 * @throws DataAccessException if the query fails
	 * @see #queryForMap(String)
	 * @see ColumnMapRowMapper
	 */
	Map<String, Object> queryForMap(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result list.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * result objects, each of them matching the specified element type.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> elementType)
			throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result list.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * result objects, each of them matching the specified element type.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 * @deprecated as of 5.3, in favor of {@link #queryForList(String, Class, Object...)}
	 */
	@Deprecated(since = "5.3")
	<T> List<T> queryForList(String sql, @Nullable Object @Nullable [] args, Class<T> elementType) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result list.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * result objects, each of them matching the specified element type.
	 * @param sql the SQL query to execute
	 * @param elementType the required type of element in the result list
	 * (for example, {@code Integer.class})
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return a List of objects that match the specified element type
	 * @throws DataAccessException if the query fails
	 * @since 3.0.1
	 * @see #queryForList(String, Class)
	 * @see SingleColumnRowMapper
	 */
	<T> List<T> queryForList(String sql, Class<T> elementType, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result list.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * Maps (one entry for each column, using the column name as the key).
	 * Each element in the list will be of the form returned by this interface's
	 * {@code queryForMap} methods.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String)
	 * @see java.sql.Types
	 */
	List<Map<String, Object>> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting a result list.
	 * <p>The results will be mapped to a List (one entry for each row) of
	 * Maps (one entry for each column, using the column name as the key).
	 * Each element in the list will be of the form returned by this interface's
	 * {@code queryForMap} methods.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return a List that contains a Map per row
	 * @throws DataAccessException if the query fails
	 * @see #queryForList(String)
	 */
	List<Map<String, Object>> queryForList(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting an SqlRowSet.
	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
	 * is used.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 * @see java.sql.Types
	 */
	SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of
	 * arguments to bind to the query, expecting an SqlRowSet.
	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
	 * is used.
	 * @param sql the SQL query to execute
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return an SqlRowSet representation (possibly a wrapper around a
	 * {@code javax.sql.rowset.CachedRowSet})
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #queryForRowSet(String)
	 * @see SqlRowSetResultSetExtractor
	 * @see javax.sql.rowset.CachedRowSet
	 */
	SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Issue a single SQL update operation (such as an insert, update or delete
	 * statement) using a PreparedStatementCreator to provide SQL and any
	 * required parameters.
	 * <p>A PreparedStatementCreator can either be implemented directly or
	 * configured through a PreparedStatementCreatorFactory.
	 * @param psc a callback that provides SQL and any necessary parameters
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see PreparedStatementCreatorFactory
	 */
	int update(PreparedStatementCreator psc) throws DataAccessException;

	/**
	 * Issue an update statement using a PreparedStatementCreator to provide SQL and
	 * any required parameters. Generated keys will be put into the given KeyHolder.
	 * <p>Note that the given PreparedStatementCreator has to create a statement
	 * with activated extraction of generated keys (a JDBC 3.0 feature). This can
	 * either be done directly or through using a PreparedStatementCreatorFactory.
	 * <p>This method requires support for generated keys in the JDBC driver.
	 * @param psc a callback that provides SQL and any necessary parameters
	 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see PreparedStatementCreatorFactory
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
	 */
	int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException;

	/**
	 * Issue an update statement using a PreparedStatementSetter to set bind parameters,
	 * with given SQL. Simpler than using a PreparedStatementCreator as this method
	 * will create the PreparedStatement: The PreparedStatementSetter just needs to
	 * set parameters.
	 * @param sql the SQL containing bind parameters
	 * @param pss helper that sets bind parameters. If this is {@code null}
	 * we run an update with static SQL.
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException;

	/**
	 * Issue a single SQL update operation (such as an insert, update or delete statement)
	 * via a prepared statement, binding the given arguments.
	 * @param sql the SQL containing bind parameters
	 * @param args arguments to bind to the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 * @see java.sql.Types
	 */
	int update(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;

	/**
	 * Issue a single SQL update operation (such as an insert, update or delete statement)
	 * via a prepared statement, binding the given arguments.
	 * @param sql the SQL containing bind parameters
	 * @param args arguments to bind to the query
	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
	 * may also contain {@link SqlParameterValue} objects which indicate not
	 * only the argument value but also the SQL type and optionally the scale
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int update(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;

	/**
	 * Issue multiple update statements on a single PreparedStatement,
	 * using batch updates and a BatchPreparedStatementSetter to set values.
	 * <p>Will fall back to separate updates on a single PreparedStatement
	 * if the JDBC driver does not support batch updates.
	 * @param sql defining PreparedStatement that will be reused.
	 * All statements in the batch will use the same SQL.
	 * @param pss object to set parameters on the PreparedStatement
	 * created by this method
	 * @return an array of the number of rows affected by each statement
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException;

	/**
	 * Issue multiple update statements on a single PreparedStatement,
	 * using batch updates and a BatchPreparedStatementSetter to set values.
	 * Generated keys will be put into the given KeyHolder.
	 * <p>Note that the given PreparedStatementCreator has to create a statement
	 * with activated extraction of generated keys (a JDBC 3.0 feature). This can
	 * either be done directly or through using a PreparedStatementCreatorFactory.
	 * <p>This method requires support for generated keys in the JDBC driver.
	 * It will fall back to separate updates on a single PreparedStatement
	 * if the JDBC driver does not support batch updates.
	 * @param psc a callback that creates a PreparedStatement given a Connection
	 * @param pss object to set parameters on the PreparedStatement
	 * created by this method
	 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
	 * @return an array of the number of rows affected by each statement
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 * @since 6.1
	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
	 */
	int[] batchUpdate(PreparedStatementCreator psc, BatchPreparedStatementSetter pss,
			KeyHolder generatedKeyHolder) throws DataAccessException;

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 * @param sql the SQL statement to execute
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException;

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 * @param sql the SQL statement to execute.
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @param argTypes the SQL types of the arguments
	 * (constants from {@code java.sql.Types})
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException;

	/**
	 * Execute multiple batches using the supplied SQL statement with the collect of supplied
	 * arguments. The arguments' values will be set using the ParameterizedPreparedStatementSetter.
	 * Each batch should be of size indicated in 'batchSize'.
	 * @param sql the SQL statement to execute.
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @param batchSize batch size
	 * @param pss the ParameterizedPreparedStatementSetter to use
	 * @return an array containing for each batch another array containing the numbers of
	 * rows affected by each update in the batch
	 * (may also contain special JDBC-defined negative values for affected rows such as
	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
	 * @throws DataAccessException if there is any problem issuing the update
	 * @since 3.1
	 */
	<T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException;


	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC CallableStatement. This allows for implementing arbitrary
	 * data access operations on a single Statement, within Spring's managed JDBC
	 * environment: that is, participating in Spring-managed transactions and
	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param csc a callback that creates a CallableStatement given a Connection
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * Execute a JDBC data access operation, implemented as callback action
	 * working on a JDBC CallableStatement. This allows for implementing arbitrary
	 * data access operations on a single Statement, within Spring's managed JDBC
	 * environment: that is, participating in Spring-managed transactions and
	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a domain
	 * object or a collection of domain objects.
	 * @param callString the SQL call string to execute
	 * @param action a callback that specifies the action
	 * @return a result object returned by the action, or {@code null} if none
	 * @throws DataAccessException if there is any problem
	 */
	<T> @Nullable T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException;

	/**
	 * Execute an SQL call using a CallableStatementCreator to provide SQL and
	 * any required parameters.
	 * @param csc a callback that provides SQL and any necessary parameters
	 * @param declaredParameters list of declared SqlParameter objects
	 * @return a Map of extracted out parameters
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException;

}
