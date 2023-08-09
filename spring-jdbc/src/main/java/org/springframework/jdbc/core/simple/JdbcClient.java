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

package org.springframework.jdbc.core.simple;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

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
 * with a convenient unified facade for JDBC PreparedStatement execution.
 *
 * <p>An example for retrieving a query result as a {@code java.util.Optional}:
 * <pre class="code">
 * Optional&lt;Integer&gt; value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
 *     .param("id", 3)
 *     .query((rs, rowNum) -> rs.getInt(1))
 *     .optional();
 * </pre>
 *
 * <p>Delegates to {@link org.springframework.jdbc.core.JdbcTemplate} and
 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
 * For complex JDBC operations, e.g. batch inserts and stored procedure calls,
 * you may use those lower-level template classes directly - or alternatively,
 * {@link SimpleJdbcInsert} and {@link SimpleJdbcCall}.
 *
 * @author Juergen Hoeller
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
	 * typically a {@link org.springframework.jdbc.core.JdbcTemplate}.
	 * <p>Use this factory method for reusing existing {@code JdbcTemplate} configuration,
	 * including its {@code DataSource}.
	 * @param jdbcTemplate the delegate to perform operations on
	 */
	static JdbcClient create(JdbcOperations jdbcTemplate) {
		return new DefaultJdbcClient(jdbcTemplate);
	}

	/**
	 * Create a {@code JdbcClient} for the given {@link NamedParameterJdbcOperations} delegate,
	 * typically a {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}.
	 * <p>Use this factory method for reusing existing {@code NamedParameterJdbcTemplate}
	 * configuration, including its underlying {@code JdbcTemplate} and the {@code DataSource}.
	 * @param jdbcTemplate the delegate to perform operations on
	 */
	static JdbcClient create(NamedParameterJdbcOperations jdbcTemplate) {
		return new DefaultJdbcClient(jdbcTemplate);
	}


	/**
	 * A statement specification for parameter bindings and query/update execution.
	 */
	interface StatementSpec {

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by implicit order of parameter value registration.
		 * <p>This is primarily intended for statements with a single or very few
		 * parameters, registering each parameter value in the order of the
		 * parameter's occurrence in the SQL statement.
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object)
		 */
		StatementSpec param(Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object)
		 */
		StatementSpec param(int jdbcIndex, Object value);

		/**
		 * Bind a positional JDBC statement parameter for "?" placeholder resolution
		 * by explicit JDBC statement parameter index.
		 * @param jdbcIndex the JDBC-style index (starting with 1)
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this statement specification (for chaining)
		 * @see java.sql.PreparedStatement#setObject(int, Object, int)
		 */
		StatementSpec param(int jdbcIndex, Object value, int sqlType);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @return this statement specification (for chaining)
		 * @see org.springframework.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object)
		 */
		StatementSpec param(String name, Object value);

		/**
		 * Bind a named statement parameter for ":x" placeholder resolution,
		 * with each "x" name matching a ":x" placeholder in the SQL statement.
		 * @param name the parameter name
		 * @param value the parameter value to bind
		 * @param sqlType the associated SQL type (see {@link java.sql.Types})
		 * @return this statement specification (for chaining)
		 * @see org.springframework.jdbc.core.namedparam.MapSqlParameterSource#addValue(String, Object, int)
		 */
		StatementSpec param(String name, Object value, int sqlType);

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
		 * based on its JavaBean properties, record components or raw fields.
		 * A Map instance can be provided as a complete parameter source as well.
		 * @param namedParamObject a custom parameter object (e.g. a JavaBean or
		 * record class) with named properties serving as statement parameters
		 * @return this statement specification (for chaining)
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
		 * @param rowMapper the callback for mapping each row in the ResultSet
		 * @return the mapped query specification
		 * @see java.sql.PreparedStatement#executeQuery()
		 */
		<T> MappedQuerySpec<T> query(RowMapper<T> rowMapper);

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
		<T> T query(ResultSetExtractor<T> rse);

		/**
		 * Execute the provided SQL statement as an update.
		 * @return the number of rows affected
		 * @see java.sql.PreparedStatement#executeUpdate()
		 */
		int update();

		/**
		 * Execute the provided SQL statement as an update.
		 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
		 * (typically a {@link org.springframework.jdbc.support.GeneratedKeyHolder})
		 * @return the number of rows affected
		 * @see java.sql.PreparedStatement#executeUpdate()
		 */
		int update(KeyHolder generatedKeyHolder);
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
		List<Map<String, Object>> listOfRows();

		/**
		 * Retrieve a single row result.
		 * @return the result row represented as a map of
		 * case-insensitive column names to column values
		 */
		Map<String, Object> singleRow();

		/**
		 * Retrieve a single column result,
		 * retaining the order from the original database result.
		 * @return a (potentially empty) list of rows, with each
		 * row represented as a column value of the given type
		 */
		<T> List<T> singleColumn(Class<T> requiredType);

		/**
		 * Retrieve a single value result.
		 * @return the single row represented as its single
		 * column value of the given type
		 * @see DataAccessUtils#requiredSingleResult(Collection)
		 */
		default <T> T singleValue(Class<T> requiredType) {
			return DataAccessUtils.requiredSingleResult(singleColumn(requiredType));
		}
	}


	/**
	 * A specification for RowMapper-mapped queries.
	 *
	 * @param <T> the RowMapper-declared result type
	 */
	interface MappedQuerySpec<T> {

		/**
		 * Retrieve the result as a lazily resolved stream of mapped objects,
		 * retaining the order from the original database result.
		 * @return the result Stream, containing mapped objects, needing to be
		 * closed once fully processed (e.g. through a try-with-resources clause)
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
		 * Retrieve a single result, if available, as an {@link Optional} handle.
		 * @return an Optional handle with a single result object or none
		 * @see #list()
		 * @see DataAccessUtils#optionalResult(Collection)
		 */
		default Optional<T> optional() {
			return DataAccessUtils.optionalResult(list());
		}

		/**
		 * Retrieve a single result as a required object instance.
		 * @return the single result object (never {@code null})
		 * @see #list()
		 * @see DataAccessUtils#requiredSingleResult(Collection)
		 */
		default T single() {
			return DataAccessUtils.requiredSingleResult(list());
		}
	}

}
