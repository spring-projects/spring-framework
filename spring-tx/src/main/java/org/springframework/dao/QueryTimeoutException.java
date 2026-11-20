/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception to be thrown on a query timeout. This could have different causes depending on
 * the database API in use but most likely thrown after the database interrupts or stops
 * the processing of a query before it has completed.
 *
 * <p>This exception can be thrown by user code trapping the native database exception or
 * by exception translation.
 *
 * @author Thomas Risberg
 * @since 3.1
 */
@SuppressWarnings("serial")
public class QueryTimeoutException extends TransientDataAccessException {

	/**
	 * Constructor for QueryTimeoutException.
	 * @param msg the detail message
	 */
	public QueryTimeoutException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for QueryTimeoutException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public QueryTimeoutException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
