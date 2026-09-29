/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * A fluent {@code JdbcClient} with common JDBC query and update operations,
 * supporting JDBC-style positional as well as Spring-style named parameters
 * with a convenient unified facade for JDBC {@code PreparedStatement} execution.
 *
 * <p>An example for retrieving a query result as a {@code java.util.Optional}:
 * <pre class="code">
 * Optional&lt;Integer&gt; value = client.sql("SELECT AGE FROM CUSTOMER WHERE ID = :id")
 *     .param("id", 3)
 *     .query(Integer.class)
 *     .optional();
 * </pre>
 *
 * <p>Delegates to {@link org.springframework.jdbc.core.JdbcTemplate} and
 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
 * Batch updates are supported in a fluent fashion through {@link StatementSpec#batch()};
 * for other complex JDBC operations &mdash; for example, stored procedure calls &mdash;
 * you may use those lower-level template classes directly, or alternatively
 * {@link SimpleJdbcInsert} and {@link SimpleJdbcCall}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Jiri Krokviak
 * @author Yanming Zhou
 * @since 6.1
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see JdbcOperations
 * @see NamedParameterJdbcOperations
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
 */
public interface JdbcClient {

	/**
	 * The starting point for any JDBC operation: a custom SQL String.
	 * @param sql the SQL query or update statement as a String
	 * @return a chained statement specification
	 */
	StatementSpec sql(String sql);


	// Static factory methods

	/**
	 * Create a {@code JdbcClient} for the given {@link DataSource}.
	 * @param dataSource the DataSource to obtain connections from
	 */
	static JdbcClient create(DataSource dataSource) {
		return new DefaultJdbcClient(dataSource);
	}

	/**
	 * Create a {@code JdbcClient} for the given {@link JdbcOperations} delegate,
	 * typically an {@link org.springframework.jdbc.core.JdbcTemplate}.
	 * <p>Use this factory method to reuse existing {@code JdbcTemplate} configuration,
	 * including its {@code DataSource}.
	 * @param jdbcTemplate the delegate to perform operations on
	 */
	static JdbcClient create(JdbcOperations jdbcTemplate) {
		return new DefaultJdbcClient(jdbcTemplate);
	}

	/**
	 * Create a {@code JdbcClient} for the given {@link NamedParameterJdbcOperations} delegate,
	 * typically an {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
	 * <p>Use this factory method to reuse existing {@code NamedParameterJdbcTemplate}
	 * configuration, including its underlying {@code JdbcTemplate} and {@code DataSource}.
	 * @param jdbcTemplate the delegate to perform operations on
	 */
	static JdbcClient create(NamedParameterJdbcOperations jdbcTemplate) {
		return new DefaultJdbcClient(jdbcTemplate, null);
	}

	/**
	 * Create a {@code JdbcClient} for the given {@link NamedParameterJdbcOperations} delegate,
	 * typically an {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
	 * <p>Use this factory method to reuse existing {@code NamedParameterJdbcTemplate}
	 * configuration, including its underlying {@code JdbcTemplate} and {@code DataSource},
	 * along with a custom {@link ConversionService} for queries with mapped classes.
	 * @param jdbcTemplate the delegate to perform operations on
	 * @param conversionService a {@link ConversionService} for converting fetched JDBC values
	 * to mapped classes in {@link StatementSpec#query(Class)}
	 * @since 7.0
	 */
	static JdbcClient create(NamedParameterJdbcOperations jdbcTemplate, ConversionService conversionService) {
		return new DefaultJdbcClient(jdbcTemplate, conversionService);
	}


	/**
	 * A statement specification for parameter bindings and query/update execution.
	 */
	interface StatementSpec {

		/**
		 * Apply the given fetch size to any subsequent query statement.
		 * @param fetchSize the fetch size
		 * @since 7.0
		 * @see org.springframework.jdbc.core.JdbcTemplate#setFetchSize
		 */
		StatementSpec withFetchSize(int fetchSize);

		/**
		 * Apply the given maximum number of rows to any subsequent query statement.
		 * @param maxRows the maximum number of rows
		 * @since 7.0
		 * @see org.springframework.jdbc.core.JdbcTemplate#setMaxRows
		 */
		StatementSpec withMaxRows(int maxRows);

		/**
		 * Apply the given query timeout to any subsequent query statement.
		 * @param queryTimeout the query timeout in seconds
		 * @since 7.0
		 * @see org.springframework.jdbc.core.JdbcTemplate#setQueryTimeout
		 */
		StatementSpec withQueryTimeout(int queryTimeout);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by implicit order of parameter value registration.
		 * <p>This is primarily intended for statements with a single parameter
		 * or very few parameters, registering each parameter value in the order
		 * of the parameter's occurrence in the SQL statement.
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object)
		 */
		StatementSpec param(@Nullable Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object)
		 */
		StatementSpec param(int jdbcIndex, @Nullable Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object, int)
		 */
		StatementSpec param(int jdbcIndex, @Nullable Object value, int sqlType);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see org.springframework.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object)
		 */
		StatementSpec param(String name, @Nullable Object value);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this statement specification (for chaining)
		 * @see org.springframework.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object, int)
		 */
		StatementSpec param(String name, @Nullable Object value, int sqlType);

		/**
		 * Bind a var-args list of positional parameters for "?" placeholder resolution.
		 * <p>The given list will be added to existing positional parameters, if any.
		 * Each element from the complete list will be bound as a JDBC positional
		 * parameter with a corresponding JDBC index (i.e. list index + 1).
		 * @param values the parameter values to bind
		 * @return this statement specification (for chaining)
		 * @see #param(Object)
		 * @see #params(List)
		 */
		StatementSpec params(Object... values);

		/**
		 * Bind a list of positional parameters for "?" placeholder resolution.
		 * <p>The given list will be added to existing positional parameters, if any.
		 * Each element from the complete list will be bound as a JDBC positional
		 * parameter with a corresponding JDBC index (i.e. list index + 1).
		 * @param values the parameter values to bind
		 * @return this statement specification (for chaining)
		 * @see #param(Object)
		 */
		StatementSpec params(List<?> values);

		/**
		 * Bind named statement parameters for ":x" placeholder resolution.
		 * <p>The given map will be merged into existing named parameters, if any.
		 * @param paramMap a map of names and parameter values to bind
		 * @return this statement specification (for chaining)
		 * @see #param(String, Object)
		 */
		StatementSpec params(Map<String, ?> paramMap);

		/**
		 * Bind named statement parameters for ":x" placeholder resolution.
		 * <p>The given parameter object will define all named parameters
		 * based on its JavaBean properties, record components, or raw fields.
		 * A Map instance can be provided as a complete parameter source as well.
		 * @param namedParamObject a custom parameter object (for example, a JavaBean,
		 * record class, or field holder) with named properties serving as
		 * statement parameters
		 * @return this statement specification (for chaining)
		 * @see #paramSource(SqlParameterSource)
		 * @see org.springframework.jdbc.core.namedparam.MapSqlParameterSource
		 * @see org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource
		 */
		StatementSpec paramSource(Object namedParamObject);

		/**
		 * Bind named statement parameters for ":x" placeholder resolution.
		 * <p>The given parameter source will define all named parameters,
		 * possibly associating specific SQL types with each value.
		 * @param namedParamSource a custom {@link SqlParameterSource} instance
		 * @return this statement specification (for chaining)
		 * @see org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource#registerSqlType
		 */
		StatementSpec paramSource(SqlParameterSource namedParamSource);

		/**
		 * Proceed towards execution of a query, with several result options
		 * available in the returned query specification.
		 * @return the result query specification
		 * @see java.sql.PreparedStatement#executeQuery()
		 */
		ResultQuerySpec query();

		/**
		 * Proceed towards execution of a mapped query, with several options
		 * available in the returned query specification.
		 * @param mappedClass the target class to apply a RowMapper for
		 * (either a simple value type for a single column mapping or a
		 * JavaBean / record class / field holder for a multi-column mapping)
		 * @return the mapped query specification
		 * @see #query(RowMapper)
		 * @see org.springframework.jdbc.core.SingleColumnRowMapper
		 * @see org.springframework.jdbc.core.SimplePropertyRowMapper
		 */
		<T> MappedQuerySpec<@Nullable T> query(Class<T> mappedClass);

		/**
		 * Proceed towards execution of a mapped query, with several options
		 * available in the returned query specification.
		 * @param rowMapper the callback for mapping each row in the ResultSet
		 * @return the mapped query specification
		 * @see java.sql.PreparedStatement#executeQuery()
		 */
		<T extends @Nullable Object> MappedQuerySpec<T> query(RowMapper<T> rowMapper);

		/**
		 * Execute a query with the provided SQL statement,
		 * processing each row with the given callback.
		 * @param rch a callback for processing each row in the ResultSet
		 * @see java.sql.PreparedStatement#executeQuery()
		 */
		void query(RowCallbackHandler rch);

		/**
		 * Execute a query with the provided SQL statement,
		 * returning a result object for the entire ResultSet.
		 * @param rse a callback for processing the entire ResultSet
		 * @return the value returned by the ResultSetExtractor
		 * @see java.sql.PreparedStatement#executeQuery()
		 */
		<T extends @Nullable Object> T query(ResultSetExtractor<T> rse);

		/**
		 * Execute the provided SQL statement as an update.
		 * @return the number of rows affected
		 * @see java.sql.PreparedStatement#executeUpdate()
		 */
		int update();

		/**
		 * Execute the provided SQL statement as an update.
		 * <p>This method requires support for generated keys in the JDBC driver.
		 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
		 * (typically a {@link org.springframework.jdbc.support.GeneratedKeyHolder})
		 * @return the number of rows affected
		 * @see java.sql.PreparedStatement#executeUpdate()
		 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
		 */
		int update(KeyHolder generatedKeyHolder);

		/**
		 * Execute the provided SQL statement as an update.
		 * <p>This method requires support for generated keys in the JDBC driver.
		 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
		 * (typically a {@link org.springframework.jdbc.support.GeneratedKeyHolder})
		 * @param keyColumnNames names of the columns that will have keys generated for them
		 * @return the number of rows affected
		 * @see java.sql.PreparedStatement#executeUpdate()
		 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
		 */
		int update(KeyHolder generatedKeyHolder, String... keyColumnNames);

		/**
		 * Begin a batch update for the provided SQL statement, accumulating several
		 * batch entries with each entry representing the parameters for one
		 * statement execution within the batch.
		 * <p>Define each batch entry through the returned {@link BatchSpec} and
		 * finally trigger execution through {@link BatchSpec#update()}:
		 * <pre class="code">
		 * int[] rowsAffected = client.sql("INSERT INTO user (first_name, last_name) VALUES (:first, :last)")
		 *     .batch()
		 *         .entry(entry -&gt; entry.param("first", "Jane").param("last", "Smith"))
		 *         .entry(entry -&gt; entry.param("first", "John").param("last", "Doe"))
		 *     .update();
		 * </pre>
		 * <p>Alternatively, provide an entire list of parameter objects at once,
		 * with each object representing one batch entry:
		 * <pre class="code">
		 * List&lt;User&gt; users = ...;
		 * int[] rowsAffected = client.sql("INSERT INTO user (first_name, last_name) VALUES (:firstName, :lastName)")
		 *     .batch()
		 *         .entries(users)
		 *     .update();
		 * </pre>
		 * @return a batch specification for accumulating batch entries
		 * @since 7.1
		 * @see java.sql.PreparedStatement#executeBatch()
		 */
		BatchSpec batch();
	}


	/**
	 * A specification for accumulating several batch entries for a batch update,
	 * with each entry representing the parameters for one statement execution
	 * within the batch.
	 *
	 * <p>Each {@code entry(...)} or {@code entries(...)} call defines one or more
	 * complete batch entries; calls may be freely combined within the same batch.
	 * Parameters are bound in the same fashion as for a single {@link StatementSpec},
	 * either as JDBC-style positional parameters or as Spring-style named parameters
	 * (but not both within the same batch).
	 *
	 * @since 7.1
	 * @see StatementSpec#batch()
	 * @see BatchEntry
	 */
	interface BatchSpec {

		/**
		 * Define a batch entry by binding its parameters through the given callback.
		 * <p>The entry is complete once the callback returns.
		 * @param entryConsumer a callback for binding the parameters of the entry
		 * @return this batch specification (for chaining)
		 * @throws IllegalStateException if the entry does not declare any parameters,
		 * or if named and indexed parameters are mixed within the batch
		 */
		BatchSpec entry(Consumer<BatchEntry> entryConsumer);

		/**
		 * Define a batch entry with the given list of positional parameters for
		 * "?" placeholder resolution.
		 * <p>Each element of the list will be bound as a JDBC positional parameter
		 * with a corresponding JDBC index (i.e. list index + 1).
		 * <p>Note that the given list represents the parameters of a <em>single</em>
		 * entry; see {@link #entries(List)} for defining several entries at once.
		 * @param values the parameter values to bind
		 * @return this batch specification (for chaining)
		 * @throws IllegalStateException if the list is empty, or if named and
		 * indexed parameters are mixed within the batch
		 * @see StatementSpec#params(List)
		 */
		BatchSpec entry(List<?> values);

		/**
		 * Define a batch entry with the given named parameters for ":x"
		 * placeholder resolution.
		 * @param paramMap a map of names and parameter values to bind
		 * @return this batch specification (for chaining)
		 * @throws IllegalStateException if the map is empty, or if named and
		 * indexed parameters are mixed within the batch
		 * @see StatementSpec#params(Map)
		 */
		BatchSpec entry(Map<String, ?> paramMap);

		/**
		 * Define several batch entries at once, with each given parameter object
		 * defining the named parameters for one entry.
		 * <p>Each parameter object is resolved in the same fashion as for
		 * {@link BatchEntry#paramSource(Object)}: based on its JavaBean properties,
		 * record components, or raw fields. A {@link Map} or a
		 * {@link SqlParameterSource} instance is supported as well.
		 * @param namedParamObjects the parameter objects, one per batch entry
		 * @return this batch specification (for chaining)
		 * @throws IllegalStateException if named and indexed parameters are
		 * mixed within the batch
		 * @see #entries(List)
		 */
		BatchSpec entries(Object... namedParamObjects);

		/**
		 * Define several batch entries at once, with each element of the given
		 * list defining the named parameters for one entry.
		 * <p>Each parameter object is resolved in the same fashion as for
		 * {@link BatchEntry#paramSource(Object)}: based on its JavaBean properties,
		 * record components, or raw fields. A {@link Map} or a
		 * {@link SqlParameterSource} instance is supported as well.
		 * <p>Note that each element of the given list represents a <em>separate</em>
		 * entry; see {@link #entry(List)} for defining a single entry with
		 * positional parameters.
		 * @param namedParamObjects the parameter objects, one per batch entry
		 * @return this batch specification (for chaining)
		 * @throws IllegalStateException if named and indexed parameters are
		 * mixed within the batch
		 */
		BatchSpec entries(List<?> namedParamObjects);

		/**
		 * Execute the accumulated batch entries as a batch update.
		 * @return an array containing the numbers of rows affected by each execution in the batch
		 * (may also contain special JDBC-defined negative values for affected rows such as
		 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
		 * @throws DataAccessException if there is any problem issuing the update
		 * @see java.sql.PreparedStatement#executeBatch()
		 */
		int[] update();

		/**
		 * Execute the accumulated batch entries as multiple batch updates, each batch should
		 * be of size indicated in 'batchSize'.
		 * @param batchSize batch size
		 * @return an array containing for each batch another array containing the numbers of
		 * rows affected by each update in the batch
		 * (may also contain special JDBC-defined negative values for affected rows such as
		 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
		 * @throws DataAccessException if there is any problem issuing the update
		 * @see java.sql.PreparedStatement#executeBatch()
		 */
		int[][] update(int batchSize);

		/**
		 * Execute the accumulated batch entries as a batch update, returning
		 * generated keys.
		 * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
		 * @return an array containing the numbers of rows affected by each execution in the batch
		 * (may also contain special JDBC-defined negative values for affected rows such as
		 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
		 * @throws DataAccessException if there is any problem issuing the update
		 * @see #update()
		 * @see org.springframework.jdbc.support.GeneratedKeyHolder
		 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
		 */
		int[] update(KeyHolder generatedKeyHolder);

		/**
		 * Execute the accumulated batch entries as a batch update, returning
		 * generated keys.
		 * @param generatedKeyHolder a {@link KeyHolder} that will hold the generated keys
		 * @param keyColumnNames names of the columns that will have keys generated for them
		 * @return an array containing the numbers of rows affected by each execution in the batch
		 * (may also contain special JDBC-defined negative values for affected rows such as
		 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
		 * @throws DataAccessException if there is any problem issuing the update
		 * @see #update()
		 * @see org.springframework.jdbc.support.GeneratedKeyHolder
		 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
		 */
		int[] update(KeyHolder generatedKeyHolder, String... keyColumnNames);
	}


	/**
	 * A specification for binding the parameters of a single batch entry,
	 * as provided to {@link BatchSpec#entry(Consumer)}.
	 *
	 * <p>Parameters are bound in the same fashion as for a single {@link StatementSpec},
	 * either as JDBC-style positional parameters or as Spring-style named parameters
	 * (but not both).
	 *
	 * @since 7.1
	 * @see BatchSpec#entry(Consumer)
	 */
	interface BatchEntry {

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by implicit order of parameter value registration.
		 * <p>This is primarily intended for statements with a single parameter
		 * or very few parameters, registering each parameter value in the order
		 * of the parameter's occurrence in the SQL statement.
		 * @param value the parameter value to bind
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#param(Object)
		 */
		BatchEntry param(@Nullable Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#param(int, Object)
		 */
		BatchEntry param(int jdbcIndex, @Nullable Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#param(int, Object, int)
		 */
		BatchEntry param(int jdbcIndex, @Nullable Object value, int sqlType);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#param(String, Object)
		 */
		BatchEntry param(String name, @Nullable Object value);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#param(String, Object, int)
		 */
		BatchEntry param(String name, @Nullable Object value, int sqlType);

		/**
		 * Bind named statement parameters for ":x" placeholder resolution.
		 * <p>The given parameter object will define all named parameters for
		 * this entry, based on its JavaBean properties, record components, or
		 * raw fields. A Map instance can be provided as a complete parameter
		 * source as well.
		 * @param namedParamObject a custom parameter object
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#paramSource(Object)
		 */
		BatchEntry paramSource(Object namedParamObject);

		/**
		 * Bind named statement parameters for ":x" placeholder resolution.
		 * <p>The given parameter source will define all named parameters for
		 * this entry, possibly associating specific SQL types with each value.
		 * @param namedParamSource a custom {@link SqlParameterSource} instance
		 * @return this batch entry (for chaining)
		 * @see StatementSpec#paramSource(SqlParameterSource)
		 */
		BatchEntry paramSource(SqlParameterSource namedParamSource);
	}


	/**
	 * A specification for simple result queries.
	 */
	interface ResultQuerySpec {

		/**
		 * Retrieve the result as a row set.
		 * @return a detached row set representation
		 * of the original database result
		 */
		SqlRowSet rowSet();

		/**
		 * Retrieve the result as a list of rows,
		 * retaining the order from the original database result.
		 * @return a (potentially empty) list of rows,
		 * with each result row represented as a map of
		 * case-insensitive column names to column values
		 */
		List<Map<String, @Nullable Object>> listOfRows();

		/**
		 * Retrieve a single row result.
		 * @return the result row represented as a map of
		 * case-insensitive column names to column values
		 */
		Map<String, @Nullable Object> singleRow();

		/**
		 * Retrieve a single column result,
		 * retaining the order from the original database result.
		 * @return a (potentially empty) list of rows, with each
		 * row represented as its single column value
		 */
		List<@Nullable Object> singleColumn();

		/**
		 * Retrieve a single value result.
		 * <p>Note: As of 6.2, this will enforce non-null result values
		 * as originally designed (just accidentally not enforced before).
		 * (never {@code null})
		 * @see #optionalValue()
		 * @see DataAccessUtils#requiredSingleResult(Collection)
		 */
		default Object singleValue() {
			return DataAccessUtils.requiredSingleResult(singleColumn());
		}

		/**
		 * Retrieve a single value result, if available, as an {@link Optional} handle.
		 * @return an Optional handle with the single column value from the single row
		 * @since 6.2
		 * @see #singleValue()
		 * @see DataAccessUtils#optionalResult(Collection)
		 */
		default Optional<Object> optionalValue() {
			return DataAccessUtils.optionalResult(singleColumn());
		}
	}


	/**
	 * A specification for RowMapper-mapped queries.
	 *
	 * @param <T> the RowMapper-declared result type
	 */
	interface MappedQuerySpec<T extends @Nullable Object> {

		/**
		 * Retrieve the result as a lazily resolved stream of mapped objects,
		 * retaining the order from the original database result.
		 * @return the result Stream, containing mapped objects, needing to be
		 * closed once fully processed (for example, through a try-with-resources clause)
		 */
		Stream<T> stream();

		/**
		 * Retrieve the result as a pre-resolved list of mapped objects,
		 * retaining the order from the original database result.
		 * @return the result as a detached List, containing mapped objects
		 */
		List<T> list();

		/**
		 * Retrieve the result as an order-preserving set of mapped objects.
		 * @return the result as a detached Set, containing mapped objects
		 * @see #list()
		 * @see LinkedHashSet
		 */
		default Set<T> set() {
			return new LinkedHashSet<>(list());
		}

		/**
		 * Retrieve a single result as a required object instance.
		 * <p>Note: As of 6.2, this will enforce non-null result values
		 * as originally designed (just accidentally not enforced before).
		 * @return the single result object (never {@code null})
		 * @see #optional()
		 * @see DataAccessUtils#requiredSingleResult(Collection)
		 */
		default @NonNull T single() {
			return DataAccessUtils.requiredSingleResult(list());
		}

		/**
		 * Retrieve a single result, if available, as an {@link Optional} handle.
		 * @return an Optional handle with a single result object or none
		 * @see #single()
		 * @see DataAccessUtils#optionalResult(Collection)
		 */
		default Optional<@NonNull T> optional() {
			return DataAccessUtils.optionalResult(list());
		}
	}

}
