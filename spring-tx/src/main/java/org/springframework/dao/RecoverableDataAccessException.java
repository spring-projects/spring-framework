/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Data access exception thrown when a previously failed operation might be able
 * to succeed if the application performs some recovery steps and retries the entire
 * transaction or in the case of a distributed transaction, the transaction branch.
 * At a minimum, the recovery operation must include closing the current connection
 * and getting a new connection.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLRecoverableException
 */
@SuppressWarnings("serial")
public class RecoverableDataAccessException extends DataAccessException {

	/**
	 * Constructor for RecoverableDataAccessException.
	 * @param msg the detail message
	 */
	public RecoverableDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for RecoverableDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * data access API such as JDBC)
	 */
	public RecoverableDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
