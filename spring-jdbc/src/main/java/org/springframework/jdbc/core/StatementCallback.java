/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.SQLException;
import java.sql.Statement;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;

/**
 * Generic callback interface for code that operates on a JDBC Statement.
 * Allows to execute any number of operations on a single Statement,
 * for example a single {@code executeUpdate} call or repeated
 * {@code executeUpdate} calls with varying SQL.
 *
 * <p>Used internally by JdbcTemplate, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @since 16.03.2004
 * @param <T> the result type
 * @see JdbcTemplate#execute(StatementCallback)
 */
@FunctionalInterface
public interface StatementCallback<T extends @Nullable Object> {

	/**
	 * Gets called by {@code JdbcTemplate.execute} with an active JDBC
	 * Statement. Does not need to care about closing the Statement or the
	 * Connection, or about handling transactions: this will all be handled
	 * by Spring's JdbcTemplate.
	 * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
	 * within the callback implementation. Spring will close the Statement
	 * object after the callback returned, but this does not necessarily imply
	 * that the ResultSet resources will be closed: the Statement objects might
	 * get pooled by the connection pool, with {@code close} calls only
	 * returning the object to the pool but not physically closing the resources.
	 * <p>If called without a thread-bound JDBC transaction (initiated by
	 * DataSourceTransactionManager), the code will simply get executed on the
	 * JDBC connection with its transactional semantics. If JdbcTemplate is
	 * configured to use a JTA-aware DataSource, the JDBC connection and thus
	 * the callback code will be transactional if a JTA transaction is active.
	 * <p>Allows for returning a result object created within the callback, i.e.
	 * a domain object or a collection of domain objects. Note that there's
	 * special support for single step actions: see JdbcTemplate.queryForObject etc.
	 * A thrown RuntimeException is treated as application exception, it gets
	 * propagated to the caller of the template.
	 * @param stmt active JDBC Statement
	 * @return a result object, or {@code null} if none
	 * @throws SQLException if thrown by a JDBC method, to be auto-converted
	 * to a DataAccessException by an SQLExceptionTranslator
	 * @throws DataAccessException in case of custom exceptions
	 * @see JdbcTemplate#queryForObject(String, Class)
	 * @see JdbcTemplate#queryForRowSet(String)
	 */
	T doInStatement(Statement stmt) throws SQLException, DataAccessException;

}
