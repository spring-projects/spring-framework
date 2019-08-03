/*
 * Copyright 2002-2018 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Generic callback interface for code that operates on a JDBC Connection.
 * Allows to execute any number of operations on a single Connection,
 * using any type and number of Statements.
 *
 * <p>This is particularly useful for delegating to existing data access code
 * that expects a Connection to work on and throws SQLException. For newly
 * written code, it is strongly recommended to use JdbcTemplate's more specific
 * operations, for example a {@code query} or {@code update} variant.
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @param <T> the result type
 * @see JdbcTemplate#execute(ConnectionCallback)
 * @see JdbcTemplate#query
 * @see JdbcTemplate#update
 */
@FunctionalInterface
public interface ConnectionCallback<T> {

	/**
	 * Gets called by {@code JdbcTemplate.execute} with an active JDBC
	 * Connection. Does not need to care about activating or closing the
	 * Connection, or handling transactions.
	 * <p>If called without a thread-bound JDBC transaction (initiated by
	 * DataSourceTransactionManager), the code will simply get executed on the
	 * JDBC connection with its transactional semantics. If JdbcTemplate is
	 * configured to use a JTA-aware DataSource, the JDBC Connection and thus
	 * the callback code will be transactional if a JTA transaction is active.
	 * <p>Allows for returning a result object created within the callback, i.e.
	 * a domain object or a collection of domain objects. Note that there's special
	 * support for single step actions: see {@code JdbcTemplate.queryForObject}
	 * etc. A thrown RuntimeException is treated as application exception:
	 * it gets propagated to the caller of the template.
	 * @param con active JDBC Connection
	 * @return a result object, or {@code null} if none
	 * @throws SQLException if thrown by a JDBC method, to be auto-converted
	 * to a DataAccessException by a SQLExceptionTranslator
	 * @throws DataAccessException in case of custom exceptions
	 * @see JdbcTemplate#queryForObject(String, Class)
	 * @see JdbcTemplate#queryForRowSet(String)
	 */
	@Nullable
	T doInConnection(Connection con) throws SQLException, DataAccessException;

}
