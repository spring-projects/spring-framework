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

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * JDBC operations interface usable on Java 5 and above, exposing a
 * set of common JDBC operations, whose interface is simplified
 * through the use of varargs and autoboxing.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Thomas Risberg
 * @since 2.0
 * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
 * @see SimpleJdbcTemplate
 * @see org.springframework.jdbc.core.JdbcOperations
 * @deprecated since Spring 3.1 in favor of {@link org.springframework.jdbc.core.JdbcOperations} and
 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations}. The JdbcTemplate and 
 * NamedParameterJdbcTemplate now provide all the functionality of the SimpleJdbcTemplate.
 */
@Deprecated
public interface SimpleJdbcOperations {

	/**
	 * Expose the classic Spring JdbcTemplate to allow invocation of less
	 * commonly used methods.
	 */
	JdbcOperations getJdbcOperations();

	/**
	 * Expose the Spring NamedParameterJdbcTemplate to allow invocation of less
	 * commonly used methods.
	 */
	NamedParameterJdbcOperations getNamedParameterJdbcOperations();


	/**
	 * Query for an <code>int</code> passing in a SQL query
	 * using the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * and a map containing the arguments.
	 * @param sql the SQL query to run.
	 * @param args the map containing the arguments for the query
	 */
	int queryForInt(String sql, Map<String, ?> args) throws DataAccessException;

	/**
	 * Query for an <code>int</code> passing in a SQL query
	 * using the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * and a <code>SqlParameterSource</code> containing the arguments.
	 * @param sql the SQL query to run.
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query.
	 */
	int queryForInt(String sql, SqlParameterSource args) throws DataAccessException;

	/**
	 * Query for an <code>int</code> passing in a SQL query
	 * using the standard '?' placeholders for parameters
	 * and a variable number of arguments.
	 * @param sql the SQL query to run.
	 * @param args the variable number of arguments for the query
	 */
	int queryForInt(String sql, Object... args) throws DataAccessException;

	/**
	 * Query for an <code>long</code> passing in a SQL query
	 * using the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * and a map containing the arguments.
	 * @param sql the SQL query to run.
	 * @param args the map containing the arguments for the query
	 */
	long queryForLong(String sql, Map<String, ?> args) throws DataAccessException;

	/**
	 * Query for an <code>long</code> passing in a SQL query
	 * using the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * and a <code>SqlParameterSource</code> containing the arguments.
	 * @param sql the SQL query to run.
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 */
	long queryForLong(String sql, SqlParameterSource args) throws DataAccessException;

	/**
	 * Query for an <code>long</code> passing in a SQL query
	 * using the standard '?' placeholders for parameters
	 * and a variable number of arguments.
	 * @param sql the SQL query to run.
	 * @param args the variable number of arguments for the query
	 */
	long queryForLong(String sql, Object... args) throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> identified by the supplied @{@link Class}.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param requiredType the required type of the return value
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, Class)
	 * @see JdbcOperations#queryForObject(String, Object[], Class)
	 */
	<T> T queryForObject(String sql, Class<T> requiredType, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> identified by the supplied @{@link Class}.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param requiredType the required type of the return value
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, Class)
	 * @see JdbcOperations#queryForObject(String, Object[], Class)
	 */
	<T> T queryForObject(String sql, Class<T> requiredType, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> identified by the supplied @{@link Class}.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param requiredType the required type of the return value
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForObject(String, Class)
	 * @see JdbcOperations#queryForObject(String, Object[], Class)
	 */
	<T> T queryForObject(String sql, Class<T> requiredType, Object... args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link RowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> T queryForObject(String sql, RowMapper<T> rm, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link RowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> T queryForObject(String sql, RowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link RowMapper} to the query results to the object.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> T queryForObject(String sql, RowMapper<T> rm, Object... args)
			throws DataAccessException;

	/**
	 * Query for an object of type <code>T</code> using the supplied
	 * {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> T queryForObject(String sql, ParameterizedRowMapper<T> rm, Object... args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link RowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> List<T> query(String sql, RowMapper<T> rm, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> List<T> query(String sql, ParameterizedRowMapper<T> rm, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link RowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> List<T> query(String sql, RowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> List<T> query(String sql, ParameterizedRowMapper<T> rm, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link RowMapper} to the query results to the object.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param rm the @{@link RowMapper} to use for result mapping
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 */
	<T> List<T> query(String sql, RowMapper<T> rm, Object... args)
			throws DataAccessException;

	/**
	 * Query for a {@link List} of <code>Objects</code> of type <code>T</code> using
	 * the supplied {@link ParameterizedRowMapper} to the query results to the object.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param rm the @{@link ParameterizedRowMapper} to use for result mapping
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForObject(String, org.springframework.jdbc.core.RowMapper)
	 * @see JdbcOperations#queryForObject(String, Object[], org.springframework.jdbc.core.RowMapper)
	 * @deprecated as of Spring 3.0: Use the method using the newly genericized RowMapper interface
	 * instead since the RowMapper and ParameterizedRowMapper interfaces are equivalent now.
	 */
	@Deprecated
	<T> List<T> query(String sql, ParameterizedRowMapper<T> rm, Object... args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the supplied arguments.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map<String, Object> (one entry for each column, using the column name as the key).
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForMap(String)
	 * @see JdbcOperations#queryForMap(String, Object[])
	 */
	Map<String, Object> queryForMap(String sql, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the supplied arguments.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map<String, Object> (one entry for each column, using the column name as the key).
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForMap(String)
	 * @see JdbcOperations#queryForMap(String, Object[])
	 */
	Map<String, Object> queryForMap(String sql, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the (optional) supplied arguments.
	 * <p>The query is expected to be a single row query; the result row will be
	 * mapped to a Map<String, Object> (one entry for each column, using the column name as the key).
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForMap(String)
	 * @see JdbcOperations#queryForMap(String, Object[])
	 */
	Map<String, Object> queryForMap(String sql, Object... args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the supplied arguments.
	 * <p>Each element in the returned {@link List} is constructed as a {@link Map}
	 * as described in {@link #queryForMap}
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param args the map containing the arguments for the query
	 * @see JdbcOperations#queryForList(String)
	 * @see JdbcOperations#queryForList(String, Object[])
	 */
	List<Map<String, Object>> queryForList(String sql, Map<String, ?> args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the supplied arguments.
	 * <p>Each element in the returned {@link List} is constructed as a {@link Map}
	 * as described in {@link #queryForMap}
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL query to run
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the query
	 * @see JdbcOperations#queryForList(String)
	 * @see JdbcOperations#queryForList(String, Object[])
	 */
	List<Map<String, Object>> queryForList(String sql, SqlParameterSource args)
			throws DataAccessException;

	/**
	 * Execute the supplied query with the (optional) supplied arguments.
	 * <p>Each element in the returned {@link List} is constructed as a {@link Map}
	 * as described in {@link #queryForMap}
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL query to run
	 * @param args the variable number of arguments for the query
	 * @see JdbcOperations#queryForList(String)
	 * @see JdbcOperations#queryForList(String, Object[])
	 */
	List<Map<String, Object>> queryForList(String sql, Object... args)
			throws DataAccessException;

	/**
	 * Execute the supplied SQL statement with (optional) supplied arguments.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL statement to execute
	 * @param args the map containing the arguments for the query
	 * @return the numbers of rows affected by the update
	 * @see NamedParameterJdbcOperations#update(String, Map)
	 */
	int update(String sql, Map<String, ?> args) throws DataAccessException;

	/**
	 * Execute the supplied SQL statement with supplied arguments.
	 * Uses sql with the named parameter support provided by the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
	 * @param sql the SQL statement to execute
	 * @param args the <code>SqlParameterSource</code> containing the arguments for the statement
	 * @return the numbers of rows affected by the update
	 * @see NamedParameterJdbcOperations#update(String, SqlParameterSource)
	 */
	int update(String sql, SqlParameterSource args) throws DataAccessException;

	/**
	 * Execute the supplied SQL statement with supplied arguments.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL statement to execute
	 * @param args the variable number of arguments for the query
	 * @return the numbers of rows affected by the update
	 * @see JdbcOperations#update(String)
	 * @see JdbcOperations#update(String, Object[])
	 */
	int update(String sql, Object... args) throws DataAccessException;

	/**
	 * Executes a batch using the supplied SQL statement with the batch of supplied arguments.
	 * Uses sql with the named parameter support.
	 * @param sql the SQL statement to execute
	 * @param batchValues the array of Maps containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 */
	public int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 * Uses sql with the named parameter support.
	 * @param sql the SQL statement to execute
	 * @param batchArgs the array of {@link SqlParameterSource} containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 */
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL statement to execute
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 */
	public int[] batchUpdate(String sql, List<Object[]> batchArgs);

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 * Uses sql with the standard '?' placeholders for parameters
	 * @param sql the SQL statement to execute.
	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
	 * @param argTypes SQL types of the arguments
	 * (constants from <code>java.sql.Types</code>)
	 * @return an array containing the numbers of rows affected by each update in the batch
	 */
	public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes);

}
