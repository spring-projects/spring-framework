/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Root of the hierarchy of data access exceptions that are considered non-transient -
 * where a retry of the same operation would fail unless the cause of the Exception
 * is corrected.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLNonTransientException
 */
@SuppressWarnings("serial")
public abstract class NonTransientDataAccessException extends DataAccessException {

	/**
	 * Constructor for NonTransientDataAccessException.
	 * @param msg the detail message
	 */
	public NonTransientDataAccessException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for NonTransientDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * data access API such as JDBC)
	 */
	public NonTransientDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
