/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Exception thrown when SQL specified is invalid. Such exceptions always have
 * a {@code java.sql.SQLException} root cause.
 *
 * <p>It would be possible to have subclasses for no such table, no such column etc.
 * A custom SQLExceptionTranslator could create such more specific exceptions,
 * without affecting code using this class.
 *
 * @author Rod Johnson
 * @see InvalidResultSetAccessException
 */
@SuppressWarnings("serial")
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {

	private final String sql;


	/**
	 * Constructor for BadSqlGrammarException.
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public BadSqlGrammarException(String task, String sql, SQLException ex) {
		super(task + "; bad SQL grammar [" + sql + "]", ex);
		this.sql = sql;
	}


	/**
	 * Return the wrapped SQLException.
	 */
	public @Nullable SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * Return the SQL that caused the problem.
	 */
	public String getSql() {
		return this.sql;
	}

}
