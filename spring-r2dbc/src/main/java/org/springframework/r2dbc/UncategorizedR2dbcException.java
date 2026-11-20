/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc;

import io.r2dbc.spi.R2dbcException;
import org.jspecify.annotations.Nullable;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we can't classify a {@link R2dbcException} into
 * one of our generic data access exceptions.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class UncategorizedR2dbcException extends UncategorizedDataAccessException {

	/** SQL that led to the problem. */
	private final @Nullable String sql;


	/**
	 * Constructor for {@code UncategorizedSQLException}.
	 * @param msg the detail message
	 * @param sql the offending SQL statement
	 * @param ex the exception thrown by underlying data access API
	 */
	public UncategorizedR2dbcException(String msg, @Nullable String sql, R2dbcException ex) {
		super(msg, ex);
		this.sql = sql;
	}


	/**
	 * Return the wrapped {@link R2dbcException}.
	 */
	public @Nullable R2dbcException getR2dbcException() {
		return (R2dbcException) getCause();
	}

	/**
	 * Return the SQL that led to the problem (if known).
	 */
	public @Nullable String getSql() {
		return this.sql;
	}

}
