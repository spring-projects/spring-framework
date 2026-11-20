/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Exception thrown when we couldn't clean up after a data access operation,
 * but the actual operation went OK.
 *
 * <p>For example, this exception or a subclass might be thrown if a JDBC
 * Connection couldn't be closed after it had been used successfully.
 *
 * <p>Note that data access code might perform resources cleanup in a
 * {@code finally} block and therefore log cleanup failure rather than rethrow it,
 * to keep the original data access exception, if any.
 *
 * @author Rod Johnson
 * @deprecated as of 6.0.3 since it is not in use within core JDBC/ORM support
 */
@Deprecated(since = "6.0.3")
@SuppressWarnings("serial")
public class CleanupFailureDataAccessException extends NonTransientDataAccessException {

	/**
	 * Constructor for CleanupFailureDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the underlying data access API,
	 * such as JDBC
	 */
	public CleanupFailureDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
