/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Fatal exception thrown when we can't connect to an RDBMS using JDBC.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class CannotGetJdbcConnectionException extends DataAccessResourceFailureException {

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @since 5.0
	 */
	public CannotGetJdbcConnectionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @param ex the root cause SQLException
	 */
	public CannotGetJdbcConnectionException(String msg, @Nullable SQLException ex) {
		super(msg, ex);
	}

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @param ex the root cause IllegalStateException
	 * @since 5.3.22
	 */
	public CannotGetJdbcConnectionException(String msg, IllegalStateException ex) {
		super(msg, ex);
	}

}
