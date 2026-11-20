/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we can't classify an SQLException into
 * one of our generic data access exceptions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class UncategorizedSQLException extends UncategorizedDataAccessException {

	/** SQL that led to the problem. */
	private final @Nullable String sql;


	/**
	 * Constructor for UncategorizedSQLException.
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public UncategorizedSQLException(String task, @Nullable String sql, SQLException ex) {
		super(task + "; uncategorized SQLException" + (sql != null ? " for SQL [" + sql + "]" : "") +
				"; SQL state [" + ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " +
				ex.getMessage(), ex);
		this.sql = sql;
	}


	/**
	 * Return the underlying SQLException.
	 */
	public @Nullable SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * Return the SQL that led to the problem (if known).
	 */
	public @Nullable String getSql() {
		return this.sql;
	}

}
