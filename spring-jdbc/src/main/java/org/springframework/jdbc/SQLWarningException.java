/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import java.sql.SQLWarning;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we're not ignoring {@link java.sql.SQLWarning SQLWarnings}.
 *
 * <p>If an SQLWarning is reported, the operation completed, so we will need
 * to explicitly roll it back if we're not happy when looking at the warning.
 * We might choose to ignore (and log) the warning, or to wrap and throw it
 * in the shape of this SQLWarningException instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.jdbc.core.JdbcTemplate#setIgnoreWarnings
 */
@SuppressWarnings("serial")
public class SQLWarningException extends UncategorizedDataAccessException {

	/**
	 * Constructor for SQLWarningException.
	 * @param msg the detail message
	 * @param ex the JDBC warning
	 */
	public SQLWarningException(String msg, SQLWarning ex) {
		super(msg, ex);
	}


	/**
	 * Return the underlying {@link SQLWarning}.
	 * @since 5.3.29
	 */
	public @Nullable SQLWarning getSQLWarning() {
		return (SQLWarning) getCause();
	}

	/**
	 * Return the underlying {@link SQLWarning}.
	 * @deprecated as of 5.3.29, in favor of {@link #getSQLWarning()}
	 */
	@Deprecated(since = "5.3.29")
	public @Nullable SQLWarning SQLWarning() {
		return getSQLWarning();
	}

}
